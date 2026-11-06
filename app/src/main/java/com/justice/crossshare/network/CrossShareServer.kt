package com.justice.crossshare.network

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.justice.crossshare.manager.SharedFileManager
import com.justice.crossshare.model.SharedFileItem
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
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
                        allowHeadersPrefixed("")
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader(HttpHeaders.Authorization)
                        allowHeader(HttpHeaders.Accept)
                        allowHeader(HttpHeaders.Origin)
                        allowHeader(HttpHeaders.UserAgent)
                        allowHeader(HttpHeaders.AccessControlAllowOrigin)
                        allowHeader(HttpHeaders.AccessControlAllowHeaders)
                        allowMethod(HttpMethod.Options)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Post)
                        allowMethod(HttpMethod.Put)
                        allowMethod(HttpMethod.Delete)
                    }

                    routing {
                        // Favicon route to prevent browser 400/404 log warnings
                        get("/favicon.ico") {
                            call.respond(HttpStatusCode.NoContent)
                        }

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
                                val multipart = call.receiveMultipart(formFieldLimit = 10L * 1024 * 1024 * 1024)
                                val savedFiles = mutableListOf<SharedFileItem>()
                                val errorMessages = mutableListOf<String>()

                                multipart.forEachPart { part ->
                                    try {
                                        if (part is PartData.FileItem) {
                                            val rawName = part.originalFileName
                                            val sanitizedName = if (rawName.isNullOrBlank()) {
                                                "upload_${System.currentTimeMillis()}"
                                            } else {
                                                rawName.substringAfterLast("/").substringAfterLast("\\")
                                            }
                                            val mimeType = part.contentType?.toString()?.takeIf { it.isNotBlank() } ?: "application/octet-stream"

                                            try {
                                                val savedItem = saveUploadedFileToDownloads(part, sanitizedName, mimeType)
                                                if (savedItem != null) {
                                                    SharedFileManager.addReceivedFile(savedItem)
                                                    savedFiles.add(savedItem)
                                                } else {
                                                    errorMessages.add("Failed to process uploaded file '$sanitizedName'")
                                                }
                                            } catch (fileErr: Exception) {
                                                Log.e("CrossShareServer", "Error saving file '$sanitizedName'", fileErr)
                                                errorMessages.add("Error saving '$sanitizedName': ${fileErr.message ?: fileErr.javaClass.simpleName}")
                                            }
                                        }
                                    } catch (partErr: Exception) {
                                        Log.e("CrossShareServer", "Part parsing error", partErr)
                                        errorMessages.add("Part error: ${partErr.message ?: partErr.javaClass.simpleName}")
                                    } finally {
                                        part.dispose()
                                    }
                                }

                                if (savedFiles.isNotEmpty()) {
                                    val responseJson = JSONObject().apply {
                                        put("status", "success")
                                        put("uploadedCount", savedFiles.size)
                                        if (errorMessages.isNotEmpty()) {
                                            put("warnings", JSONArray(errorMessages))
                                        }
                                    }
                                    call.respondText(responseJson.toString(), ContentType.Application.Json)
                                } else {
                                    val detailMsg = if (errorMessages.isNotEmpty()) {
                                        errorMessages.joinToString("; ")
                                    } else {
                                        "No valid files found in upload payload"
                                    }
                                    Log.e("CrossShareServer", "Upload rejected: $detailMsg")
                                    call.respondText(
                                        JSONObject().apply {
                                            put("status", "error")
                                            put("message", detailMsg)
                                        }.toString(),
                                        ContentType.Application.Json,
                                        HttpStatusCode.BadRequest
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("CrossShareServer", "Upload route failed", e)
                                call.respondText(
                                    JSONObject().apply {
                                        put("status", "error")
                                        put("message", e.message ?: "Upload route failed")
                                    }.toString(),
                                    ContentType.Application.Json,
                                    HttpStatusCode.InternalServerError
                                )
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

        val safeMimeType = if (mimeType.isBlank() || mimeType == "null") "application/octet-stream" else mimeType

        // 1. Primary Save Method: MediaStore.Downloads (Android 10+ / API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, safeMimeType)
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
                    } catch (writeErr: Exception) {
                        Log.e("CrossShareServer", "Failed writing to MediaStore Uri $savedUri", writeErr)
                        try {
                            resolver.delete(savedUri, null, null)
                        } catch (ignored: Exception) {}
                        savedUri = null
                    }
                }
            } catch (insertErr: Exception) {
                Log.e("CrossShareServer", "MediaStore insert failed, falling back to File I/O", insertErr)
                savedUri = null
            }
        }

        // 2. Fallback Save Method: Direct java.io.File with ENOTDIR & Scoped Storage Collisions Protection
        if (savedUri == null) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val subDir = File(downloadsDir, "CrossShare")
                if (subDir.exists() && !subDir.isDirectory) {
                    // Collision protection: subDir exists as a file, attempt deletion
                    try {
                        subDir.delete()
                    } catch (e: Exception) {
                        Log.e("CrossShareServer", "Could not delete file named CrossShare in Downloads", e)
                    }
                }

                val targetDir = if (subDir.isDirectory || (!subDir.exists() && subDir.mkdirs())) {
                    subDir
                } else {
                    // ENOTDIR protection: fall back directly to downloadsDir if subDir is not a directory
                    downloadsDir
                }

                val nameWithoutExt = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
                val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""

                var targetFile = File(targetDir, fileName)
                var counter = 0
                while (targetFile.exists()) {
                    counter++
                    targetFile = File(targetDir, "${nameWithoutExt}_${System.currentTimeMillis()}_$counter$ext")
                }

                try {
                    targetFile.outputStream().use { outputStream ->
                        totalBytesCopied = streamPartData(filePart, outputStream)
                    }
                } catch (streamErr: Exception) {
                    Log.e("CrossShareServer", "FileOutputStream failed on $targetFile, trying unique fallback name", streamErr)
                    val fallbackFile = File(targetDir, "${nameWithoutExt}_${System.currentTimeMillis()}_fallback$ext")
                    fallbackFile.outputStream().use { outputStream ->
                        totalBytesCopied = streamPartData(filePart, outputStream)
                    }
                    targetFile = fallbackFile
                }

                savedFile = targetFile
                savedUri = Uri.fromFile(targetFile)
            } catch (fileErr: Exception) {
                Log.e("CrossShareServer", "Direct File save failed completely", fileErr)
                throw fileErr
            }
        }

        val finalUri = savedUri ?: return null

        return SharedFileItem(
            name = savedFile?.name ?: fileName,
            size = totalBytesCopied,
            mimeType = safeMimeType,
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
