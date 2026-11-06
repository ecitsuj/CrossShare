package com.justice.crossshare

import com.justice.crossshare.network.NetworkUtils
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun testGetLocalIpAddress() {
        val ip = NetworkUtils.getLocalIpAddress()
        if (ip != null) {
            assertTrue(ip.isNotEmpty())
            assertTrue(!ip.startsWith("127."))
        }
    }

    @Test
    fun testGetFormattedServerUrl() {
        val ip = NetworkUtils.getLocalIpAddress()
        val url = NetworkUtils.getFormattedServerUrl(8080)
        if (ip != null) {
            assertNotNull(url)
            assertTrue(url!!.startsWith("http://"))
            assertTrue(url.endsWith(":8080"))
        } else {
            assertNull(url)
        }
    }
}

