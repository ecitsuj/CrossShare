package com.justice.crossshare.network

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utility to discover active local IP address on Wi-Fi, Hotspot, USB Tethering, Ethernet, or P2P interfaces.
 */
object NetworkUtils {

    private val EXCLUDED_KEYWORDS = listOf("p2p", "tun", "tap", "ppp", "dummy", "rmnet")

    /**
     * Inspects NetworkInterfaces to find active, non-loopback IPv4 address across
     * hotspot, wlan, usb tethering, or ethernet interfaces.
     *
     * Filters out virtual, VPN, and peer-to-peer background interfaces (such as
     * Samsung Quick Share and Wi-Fi Direct) and prioritizes Wi-Fi Sharing / Hotspot interfaces.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            val candidateInterfaces = mutableListOf<NetworkInterface>()

            while (interfaces.hasMoreElements()) {
                val netInterface = interfaces.nextElement()
                try {
                    if (!netInterface.isUp || netInterface.isLoopback) continue
                } catch (e: Exception) {
                    continue
                }

                val lowerName = netInterface.name.lowercase()
                if (EXCLUDED_KEYWORDS.any { lowerName.contains(it) }) {
                    continue
                }

                candidateInterfaces.add(netInterface)
            }

            candidateInterfaces.sortBy { getInterfacePriority(it.name) }

            for (netInterface in candidateInterfaces) {
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        val host = address.hostAddress
                        if (!host.isNullOrEmpty() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getInterfacePriority(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.startsWith("swlan") || lower.startsWith("ap") -> 0
            lower == "wlan0" || lower.startsWith("wlan") -> 1
            lower.startsWith("rndis") || lower.startsWith("ncm") -> 2
            lower.startsWith("eth") || lower.startsWith("en") -> 3
            else -> 4
        }
    }

    fun getFormattedServerUrl(port: Int = 8080): String? {
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$port"
    }
}

