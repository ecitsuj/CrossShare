package com.justice.crossshare.network

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utility to discover active local IP address on Wi-Fi, Hotspot, USB Tethering, Ethernet, or P2P interfaces.
 */
object NetworkUtils {

    /**
     * Inspects NetworkInterfaces to find active, non-loopback IPv4 address across
     * wlan, ap, rndis, p2p, or eth interfaces.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            val candidates = mutableListOf<Pair<String, String>>() // interfaceName to IP

            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val interfaceName = networkInterface.name.lowercase()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val ip = address.hostAddress ?: continue
                        if (ip.isNotEmpty() && !ip.startsWith("127.")) {
                            candidates.add(Pair(interfaceName, ip))
                        }
                    }
                }
            }

            // Interface priority: wlan > ap / softap > rndis > eth > p2p
            val preferredInterfaces = listOf("wlan", "ap", "softap", "rndis", "eth", "p2p")
            for (pref in preferredInterfaces) {
                val match = candidates.firstOrNull { (name, _) -> name.contains(pref) }
                if (match != null) return match.second
            }

            // Fallback to site-local addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
            val siteLocalMatch = candidates.firstOrNull { (_, ip) ->
                ip.startsWith("192.168.") || ip.startsWith("10.") || isClassBPrivate(ip)
            }
            if (siteLocalMatch != null) return siteLocalMatch.second

            return candidates.firstOrNull()?.second
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun isClassBPrivate(ip: String): Boolean {
        if (!ip.startsWith("172.")) return false
        val parts = ip.split(".")
        if (parts.size >= 2) {
            val second = parts[1].toIntOrNull() ?: return false
            return second in 16..31
        }
        return false
    }

    fun getFormattedServerUrl(port: Int = 8080): String? {
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$port"
    }
}
