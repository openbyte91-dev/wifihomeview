package com.openbyt91dev.wifihomeview.data.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class NetworkUtilsTest {

    @Test
    fun `getSubnetIps returns correct range for 24 network`() {
        val mockIp = InetAddress.getByName("192.168.1.5")
        val prefix: Short = 24

        val ips = NetworkUtils.getSubnetIps(mockIp, prefix)
        
        // 192.168.1.1 to 192.168.1.254 = 254 hosts
        assertEquals(254, ips.size)
        assertEquals("192.168.1.1", ips.first())
        assertEquals("192.168.1.254", ips.last())
    }

    @Test
    fun `getSubnetIps handles weird 30 small subnet`() {
        // e.g. 192.168.1.4/30 -> Network: .4, Broadcast: .7 -> Usable: .5, .6
        val mockIp = InetAddress.getByName("192.168.1.5")
        val prefix: Short = 30
        
        val ips = NetworkUtils.getSubnetIps(mockIp, prefix)
        
        assertEquals(2, ips.size)
        assertEquals("192.168.1.5", ips.first()) // Host itself
        assertEquals("192.168.1.6", ips.last())
    }
}