package com.justice.crossshare

import com.justice.crossshare.manager.SharedFileManager
import com.justice.crossshare.model.SharedFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SharedFileManagerTest {

    @Before
    fun setUp() {
        SharedFileManager.clearAll()
    }

    @Test
    fun testAddAndGetSharedFile() {
        val item = SharedFileItem(
            id = "test-id-1",
            name = "document.pdf",
            size = 1048576L,
            mimeType = "application/pdf",
            uri = null
        )

        SharedFileManager.addSharedFile(item)

        val retrieved = SharedFileManager.getSharedFile("test-id-1")
        assertNotNull(retrieved)
        assertEquals("document.pdf", retrieved?.name)
        assertEquals("1.0 MB", retrieved?.formattedSize)

        val allFiles = SharedFileManager.getAllDownloadableFiles()
        assertEquals(1, allFiles.size)
        assertEquals("test-id-1", allFiles[0].id)
    }

    @Test
    fun testAddAndGetReceivedFile() {
        val item = SharedFileItem(
            id = "recv-id-1",
            name = "image.png",
            size = 2048L,
            mimeType = "image/png",
            uri = null,
            isReceived = true
        )

        SharedFileManager.addReceivedFile(item)

        val receivedList = SharedFileManager.receivedFiles.value
        assertEquals(1, receivedList.size)
        assertEquals("image.png", receivedList[0].name)
    }

    @Test
    fun testRemoveSharedFile() {
        val item = SharedFileItem(
            id = "remove-id",
            name = "archive.zip",
            size = 500L,
            mimeType = "application/zip",
            uri = null
        )

        SharedFileManager.addSharedFile(item)
        assertNotNull(SharedFileManager.getSharedFile("remove-id"))

        SharedFileManager.removeSharedFile("remove-id")
        assertNull(SharedFileManager.getSharedFile("remove-id"))
        assertEquals(0, SharedFileManager.sharedFiles.value.size)
    }
}
