package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginRelayTest {
    @Test
    fun `서버가 넘긴 메시지를 읽고 빈 값·모르는 종류는 버린다`() {
        val json = """{"state":"logging_in","messages":[
            {"kind":"credentials","username":"me","password":"pw"},
            {"kind":"code","text":"123456"},
            {"kind":"credentials","username":"","password":"pw"},
            {"kind":"other"}]}"""
        assertEquals(
            listOf(LoginRelay.Message.Credentials("me", "pw"), LoginRelay.Message.Code("123456")),
            LoginRelay.parseMessages(json),
        )
        assertEquals(emptyList<LoginRelay.Message>(), LoginRelay.parseMessages("""{"messages":[]}"""))
    }

    @Test
    fun `폰에서 열 주소는 스킴 없이`() {
        assertEquals("server-v2-production-8f97.up.railway.app/login", LoginRelay("https://server-v2-production-8f97.up.railway.app").phoneUrl)
    }

    @Test
    fun `evaluateJavascript 결과 벗기기와 데스크톱 UA`() {
        assertEquals("{\"ok\":true}", ThreadsWeb.unquote("\"{\\\"ok\\\":true}\""))
        assertEquals("true", ThreadsWeb.unquote("true"))
        val mobile = "Mozilla/5.0 (Linux; Android 12; RG-glasses Build/SKQ1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36"
        assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Safari/537.36",
            ThreadsWeb.desktopUserAgent(mobile),
        )
    }
}
