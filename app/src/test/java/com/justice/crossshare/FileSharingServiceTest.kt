package com.justice.crossshare

import com.justice.crossshare.service.FileSharingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FileSharingServiceTest {

    @Test
    fun testFileSharingServiceConstants() {
        assertEquals("file_sharing_channel", FileSharingService.CHANNEL_ID)
        assertEquals("CrossShare Server", FileSharingService.CHANNEL_NAME)
        assertEquals(1001, FileSharingService.NOTIFICATION_ID)
        assertEquals("com.justice.crossshare.action.START_SERVER", FileSharingService.ACTION_START_SERVER)
        assertEquals("com.justice.crossshare.action.STOP_SERVER", FileSharingService.ACTION_STOP_SERVER)
        assertEquals("com.justice.crossshare.extra.PORT", FileSharingService.EXTRA_PORT)
    }

    @Test
    fun testFileSharingServiceInitialState() {
        assertFalse(FileSharingService.isServerRunning.value)
        assertNull(FileSharingService.serverUrl.value)
        assertEquals(8080, FileSharingService.serverPort.value)
        assertNull(FileSharingService.qrCodeBitmap.value)
        assertNull(FileSharingService.qrCodeImageBitmap.value)
    }
}
