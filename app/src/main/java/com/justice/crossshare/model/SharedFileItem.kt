package com.justice.crossshare.model

import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Represents a file item shared either from the device to browsers or received from browsers.
 */
data class SharedFileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val mimeType: String?,
    val uri: Uri? = null,
    val file: File? = null,
    val isReceived: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val clampedIndex = digitGroups.coerceIn(0, units.size - 1)
            return String.format(
                "%.1f %s",
                size / Math.pow(1024.0, clampedIndex.toDouble()),
                units[clampedIndex]
            )
        }
}
