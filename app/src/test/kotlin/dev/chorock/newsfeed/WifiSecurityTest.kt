package dev.chorock.newsfeed

import dev.chorock.newsfeed.net.WifiSecurity
import dev.chorock.newsfeed.net.securityOf
import dev.chorock.newsfeed.net.validatePassphrase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiSecurityTest {

    @Test
    fun `capabilities 문자열에서 보안 종류를 정한다`() {
        val cases = mapOf(
            "[ESS]" to WifiSecurity.Open,
            "" to WifiSecurity.Open,
            "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]" to WifiSecurity.Wpa2,
            "[RSN-PSK-CCMP][ESS]" to WifiSecurity.Wpa2,
            "[RSN-SAE-CCMP][MFPR][MFPC][ESS]" to WifiSecurity.Wpa3,
            "[RSN-PSK+SAE-CCMP][MFPC][ESS]" to WifiSecurity.Wpa2, // WPA2/WPA3 혼합은 WPA2로
            "[RSN-OWE-CCMP][ESS]" to WifiSecurity.Owe,
            "[OWE_TRANSITION][ESS]" to WifiSecurity.Open,
            "[WPA2-EAP-CCMP][RSN-EAP-CCMP][ESS]" to WifiSecurity.Unsupported,
            "[RSN-EAP_SUITE_B_192-GCMP-256][ESS]" to WifiSecurity.Unsupported,
            "[WEP][ESS]" to WifiSecurity.Unsupported,
            "[WPA-TKIP][ESS]" to WifiSecurity.Unsupported,
        )
        cases.forEach { (caps, expected) -> assertEquals(caps, expected, securityOf(caps)) }
    }

    @Test
    fun `소문자 표기도 같게 본다`() = assertEquals(WifiSecurity.Wpa2, securityOf("[wpa2-psk-ccmp][ess]"))

    @Test
    fun `비밀번호가 필요한 보안과 지원 여부`() {
        assertTrue(WifiSecurity.Wpa2.needsPassword)
        assertTrue(WifiSecurity.Wpa3.needsPassword)
        assertFalse(WifiSecurity.Open.needsPassword)
        assertFalse(WifiSecurity.Owe.needsPassword)
        assertFalse(WifiSecurity.Unsupported.isSupported)
        assertTrue(WifiSecurity.Owe.isSupported)
    }

    @Test
    fun `WPA2 비밀번호는 8에서 63자 또는 64자리 16진수만 통과한다`() {
        assertNull(validatePassphrase(WifiSecurity.Wpa2, "12345678"))
        assertNull(validatePassphrase(WifiSecurity.Wpa2, "a".repeat(63)))
        assertNull(validatePassphrase(WifiSecurity.Wpa2, "0123456789abcdefABCDEF".padEnd(64, '0')))
        assertNotNull(validatePassphrase(WifiSecurity.Wpa2, ""))
        assertNotNull(validatePassphrase(WifiSecurity.Wpa2, "1234567"))
        assertNull(validatePassphrase(WifiSecurity.Wpa2, "a".repeat(64))) // 64자 16진수는 PSK로 유효하다(a는 16진수 문자)
        assertNotNull(validatePassphrase(WifiSecurity.Wpa2, "g".repeat(64))) // 64자인데 16진수가 아님
        assertNotNull(validatePassphrase(WifiSecurity.Wpa2, "a".repeat(65)))
    }

    @Test
    fun `WPA3 비밀번호는 1에서 128자를 통과한다`() {
        assertNull(validatePassphrase(WifiSecurity.Wpa3, "x"))
        assertNull(validatePassphrase(WifiSecurity.Wpa3, "x".repeat(128)))
        assertNotNull(validatePassphrase(WifiSecurity.Wpa3, ""))
        assertNotNull(validatePassphrase(WifiSecurity.Wpa3, "x".repeat(129)))
    }

    @Test
    fun `개방형과 OWE는 비밀번호 검사가 없고 미지원은 항상 막는다`() {
        assertNull(validatePassphrase(WifiSecurity.Open, ""))
        assertNull(validatePassphrase(WifiSecurity.Owe, ""))
        assertNotNull(validatePassphrase(WifiSecurity.Unsupported, "whatever"))
    }
}
