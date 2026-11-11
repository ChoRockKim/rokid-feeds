package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginFieldTest {
    @Test
    fun `evaluateJavascript 결과를 읽는다`() {
        // WebView는 JS 문자열 결과를 한 번 더 JSON 문자열로 감싸 준다.
        val raw = "\"{\\\"type\\\":\\\"password\\\",\\\"label\\\":\\\"비밀번호\\\"}\""
        assertEquals(LoginField(password = true, label = "비밀번호"), LoginField.parse(raw))
        assertEquals(false, LoginField.parse("\"{\\\"type\\\":\\\"text\\\",\\\"label\\\":\\\"사용자 이름\\\"}\"")!!.password)
    }

    @Test
    fun `입력칸이 아니면 null`() {
        assertNull(LoginField.parse("\"\""))
        assertNull(LoginField.parse("null"))
        assertNull(LoginField.parse(null))
        assertNull(LoginField.parse("garbage"))
    }

    @Test
    fun `데스크톱 UA로 바꾼다`() {
        val mobile = "Mozilla/5.0 (Linux; Android 12; RG-glasses Build/SKQ1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36"
        assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Safari/537.36",
            ThreadsWeb.desktopUserAgent(mobile),
        )
    }
}
