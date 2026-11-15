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
    fun `폰이 안경 화면에서 누른 자리·끌기·글자를 읽고 범위 밖은 버린다`() {
        val json = """{"messages":[
            {"kind":"tap","x":0.25,"y":1},{"kind":"tap","x":2,"y":0},{"kind":"tap","x":"a","y":0},
            {"kind":"scroll","dy":-3},{"kind":"type","text":"ab"},{"kind":"type","text":""}]}"""
        assertEquals(
            listOf(LoginRelay.Message.Tap(0.25f, 1f), LoginRelay.Message.Scroll(-1f), LoginRelay.Message.Type("ab")),
            LoginRelay.parseMessages(json),
        )
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
