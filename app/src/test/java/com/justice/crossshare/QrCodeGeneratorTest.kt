package com.justice.crossshare

import com.justice.crossshare.utils.QrCodeGenerator
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun testGenerateQrCodeEmptyTextReturnsNull() {
        val bitmap = QrCodeGenerator.generateQrCode("")
        assertNull(bitmap)

        val blankBitmap = QrCodeGenerator.generateQrCode("   ")
        assertNull(blankBitmap)
    }

    @Test
    fun testGenerateQrCodeImageBitmapEmptyTextReturnsNull() {
        val imageBitmap = QrCodeGenerator.generateQrCodeImageBitmap("")
        assertNull(imageBitmap)

        val blankImageBitmap = QrCodeGenerator.generateQrCodeImageBitmap("   ")
        assertNull(blankImageBitmap)
    }
}
