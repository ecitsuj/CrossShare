package com.justice.crossshare.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/**
 * Utility class to generate QR code bitmaps for sharing server URLs visually.
 */
object QrCodeGenerator {

    /**
     * Generates an ARGB_8888 Bitmap QR code for a given text URL.
     */
    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        if (text.isBlank()) return null
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a Compose-friendly ImageBitmap QR code for a given text URL.
     */
    fun generateQrCodeImageBitmap(text: String, size: Int = 512): ImageBitmap? {
        return generateQrCode(text, size)?.asImageBitmap()
    }
}
