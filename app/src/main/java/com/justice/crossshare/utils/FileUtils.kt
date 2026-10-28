package com.justice.crossshare.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.justice.crossshare.model.SharedFileItem

object FileUtils {

    fun Uri.toSharedFileItem(context: Context): SharedFileItem {
        var name = "unknown_file"
        var size = 0L
        val mimeType = context.contentResolver.getType(this)

        try {
            context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { name = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (name == "unknown_file") {
            name = this.lastPathSegment ?: "file_${System.currentTimeMillis()}"
        }

        if (size == 0L) {
            try {
                context.contentResolver.openFileDescriptor(this, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return SharedFileItem(
            name = name,
            size = size,
            mimeType = mimeType,
            uri = this,
            isReceived = false,
        )
    }

    fun openFileItem(context: Context, item: SharedFileItem) {
        try {
            val uri: Uri = when {
                (item.file != null && item.file.exists()) -> {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        item.file
                    )
                }
                item.uri != null -> item.uri
                else -> {
                    Toast.makeText(context, "File is not accessible", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open File"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFileItem(context: Context, item: SharedFileItem) {
        try {
            val uri: Uri = when {
                (item.file != null && item.file.exists()) -> {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        item.file
                    )
                }
                item.uri != null -> item.uri
                else -> {
                    Toast.makeText(context, "File is not accessible", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "CrossShare") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to copy URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share CrossShare URL") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
