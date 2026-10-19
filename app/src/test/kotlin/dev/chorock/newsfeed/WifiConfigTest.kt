package dev.chorock.newsfeed

import dev.chorock.newsfeed.net.WifiConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiConfigTest {
    @Test fun `SSID가 비어 있으면 설정되지 않은 것`() {
        assertFalse(WifiConfig("", "pw").isConfigured)
        assertFalse(WifiConfig("   ", "").isConfigured)
        assertTrue(WifiConfig("home", "pw").isConfigured)
    }

    @Test fun `비밀번호가 비어 있으면 개방형 네트워크`() {
        assertTrue(WifiConfig("cafe", "").isOpen)
        assertTrue(WifiConfig("cafe", "  ").isOpen)
        assertFalse(WifiConfig("home", "secret").isOpen)
    }
}
