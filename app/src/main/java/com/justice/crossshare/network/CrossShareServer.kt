package com.justice.crossshare.network

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.justice.crossshare.manager.SharedFileManager
import com.justice.crossshare.model.SharedFileItem
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.BindException
import java.net.URLEncoder

/**
 * Core HTTP Server engine using Ktor CIO.
 * Serves embedded web page, handles streaming downloads, and processes memory-safe multipart uploads.
 */
class CrossShareServer(private val context: Context) {

    private var serverEngine: EmbeddedServer<*, *>? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    @Synchronized
    fun start(initialPort: Int = 8080) {
        if (_isServerRunning.value) return

        val maxAttempts = 10
        var boundPort = initialPort
        var startedEngine: EmbeddedServer<*, *>? = null
        var lastException: Exception? = null

        for (attempt in 0 until maxAttempts) {
            val currentPort = initialPort + attempt
            try {
                println("CrossShareServer: Attempting to start server on port $currentPort")
                val engine = embeddedServer(CIO, port = currentPort, host = "0.0.0.0", watchPaths = emptyList()) {
                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader(HttpHeaders.Authorization)
                        allowMethod(HttpMethod.Options)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Post)
                    }

                    routing {
                        // Embedded Web Interface
                        get("/") {
                            call.respondText(WebPageContent.getHtml(), ContentType.Text.Html)
                        }

                        // API Status
                        get("/api/status") {
                            val statusJson = JSONObject().apply {
                                put("status", "running")
                                put("ip", NetworkUtils.getLocalIpAddress() ?: "unknown")
                                put("port", currentPort)
                            }
                            call.respondText(statusJson.toString(), ContentType.Application.Json)
                        }

                        // API File List available for download from device
                        get("/api/files") {
                            val files = SharedFileManager.getAllDownloadableFiles()
                            val jsonArray = JSONArray()
                            files.forEach { fileItem ->
                                val obj = JSONObject().apply {
                                    put("id", fileItem.id)
                                    put("name", fileItem.name)
                                    put("size", fileItem.size)
                                    put("formattedSize", fileItem.formattedSize)
                                    put("mimeType", fileItem.mimeType ?: "application/octet-stream")
                                    put("isReceived", fileItem.isReceived)
                                }
                                jsonArray.put(obj)
                            }
                            call.respondText(jsonArray.toString(), ContentType.Application.Json)
                        }

                        // File Download / Streaming route
                        get("/download/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrEmpty()) {
                                call.respond(HttpStatusCode.BadRequest, "Missing file ID")
                                return@get
                            }

                            val fileItem = SharedFileManager.getSharedFile(id)
                            if (fileItem == null) {
                                call.respond(HttpStatusCode.NotFound, "File not found")
                                return@get
                            }

                            val inputStream = try {
                                fileItem.uri?.let { context.contentResolver.openInputStream(it) }
                                    ?: fileItem.file?.inputStream()
                            } catch (e: Exception) {
                                fileItem.file?.inputStream()
                            }

                            if (inputStream == null) {
                                call.respond(HttpStatusCode.NotFound, "Unable to read file source")
                                return@get
                            }

                            val mimeType = fileItem.mimeType ?: "application/octet-stream"
                            val encodedFileName = URLEncoder.encode(fileItem.name, "UTF-8").replace("+", "%20")

                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                "attachment; filename=\"${fileItem.name}\"; filename*=UTF-8''$encodedFileName"
                            )

                            call.respondOutputStream(
                                contentType = ContentType.parse(mimeType),
                                status = HttpStatusCode.OK,
                                contentLength = if (fileItem.size > 0) fileItem.size else null
                            ) {
                                inputStream.use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        write(buffer, 0, bytesRead)
                                    }
                                    flush()
                                }
                            }
                        }

                        // Memory-Safe File Upload Route
                        post("/upload") {
                            try {
                                val multipart = call.receiveMultipart()
                                val savedFiles = mutableListOf<SharedFileItem>()

                                while (true) {
                                    val part = multipart.readPart() ?: break
                                    when (part) {
                                        is PartData.FileItem -> {
                                            val originalName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"
                                            val sanitizedName = originalName.substringAfterLast("/").substringAfterLast("\\")
                                            val mimeType = part.contentType?.toString() ?: "application/octet-stream"

                                            val savedItem = saveUploadedFileToDownloads(part, sanitizedName, mimeType)
                                            if (savedItem != null) {
                                                SharedFileManager.addReceivedFile(savedItem)
                                                savedFiles.add(savedItem)
                                            }
                                            part.dispose()
                                        }
                                        else -> part.dispose()
                                    }
                                }

                                if (savedFiles.isNotEmpty()) {
                                    val responseJson = JSONObject().apply {
                                        put("status", "success")
                                        put("uploadedCount", savedFiles.size)
                                    }
                                    call.respondText(responseJson.toString(), ContentType.Application.Json)
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, """{"status":"error","message":"No files uploaded"}""")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                call.respond(HttpStatusCode.InternalServerError, """{"status":"error","message":"${e.message}"}""")
                            }
                        }
                    }
                }

                engine.start(wait = false)
                startedEngine = engine
                boundPort = currentPort
                println("CrossShareServer: Successfully started server on port $boundPort")
                break
            } catch (e: Exception) {
                println("CrossShareServer: Failed to start on port $currentPort: ${e.message}")
                lastException = e
                try {
                    startedEngine?.stop(100, 200)
                } catch (ignored: Exception) {}
                startedEngine = null

                val msg = e.message?.lowercase() ?: ""
                val isBindError = e is BindException || e is IOException || msg.contains("bind") || msg.contains("address already in use") || msg.contains("address in use")
                if (!isBindError) {
                    throw e
                }
            }
        }

        if (startedEngine == null) {
            _isServerRunning.value = false
            throw lastException ?: IOException("Failed to start server on any port starting from $initialPort")
        }

        serverEngine = startedEngine
        _serverPort.value = boundPort
        _isServerRunning.value = true
    }

    @Synchronized
    fun stop() {
        if (!_isServerRunning.value) return
        try {
            serverEngine?.stop(1000, 2000)
            println("CrossShareServer: Server stopped successfully")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            serverEngine = null
            _isServerRunning.value = false
        }
    }

    private fun saveUploadedFileToDownloads(
        filePart: PartData.FileItem,
        fileName: String,
        mimeType: String
    ): SharedFileItem? {
        val resolver = context.contentResolver
        var savedUri: Uri? = null
        var savedFile: File? = null
        var totalBytesCopied: Long = 0

        // Android 10+ (API 29+) MediaStore Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CrossShare")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            savedUri = resolver.insert(collection, contentValues)

            if (savedUri != null) {
                try {
                    resolver.openOutputStream(savedUri)?.use { outputStream ->
                        totalBytesCopied = streamPartData(filePart, outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(savedUri, contentValues, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(savedUri, null, null)
                    savedUri = null
                }
            }
        }

        // Fallback for API < 29 or if MediaStore insert was null
        if (savedUri == null) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val crossShareDir = File(downloadsDir, "CrossShare")
                if (!crossShareDir.exists()) {
                    crossShareDir.mkdirs()
                }
                var targetFile = File(crossShareDir, fileName)
                if (targetFile.exists()) {
                    val nameWithoutExt = fileName.substringBeforeLast(".")
                    val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
                    targetFile = File(crossShareDir, "${nameWithoutExt}_${System.currentTimeMillis()}$ext")
                }
                targetFile.outputStream().use { outputStream ->
                    totalBytesCopied = streamPartData(filePart, outputStream)
                }
                savedFile = targetFile
                savedUri = Uri.fromFile(targetFile)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        val finalUri = savedUri ?: return null

        return SharedFileItem(
            name = fileName,
            size = totalBytesCopied,
            mimeType = mimeType,
            uri = finalUri,
            file = savedFile,
            isReceived = true
        )
    }

    private fun streamPartData(filePart: PartData.FileItem, outputStream: OutputStream): Long {
        var bytesCopied: Long = 0
        val inputStream = filePart.provider().toInputStream()
        inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesCopied += bytesRead
            }
            outputStream.flush()
        }
        return bytesCopied
    }
}
