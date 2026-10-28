package com.justice.crossshare

import com.justice.crossshare.model.SharedFileItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FileUtilsTest {

    @Test
    fun testSharedFileItemFormattedSize() {
        val item1 = SharedFileItem(name = "test.txt", size = 0L, mimeType = "text/plain")
        assertEquals("0 B", item1.formattedSize)

        val item2 = SharedFileItem(name = "image.png", size = 1500L, mimeType = "image/png")
        assertEquals("1.5 KB", item2.formattedSize)

        val item3 = SharedFileItem(name = "video.mp4", size = 10485760L, mimeType = "video/mp4")
        assertEquals("10.0 MB", item3.formattedSize)
    }
}
