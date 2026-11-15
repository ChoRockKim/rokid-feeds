package com.chorockkim.rokidfeeds.util

import com.chorockkim.rokidfeeds.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Test

class NetErrorsTest {
    @Test
    fun `실패 이유를 짧은 한국어로`() {
        assertEquals("서버 응답 없음(꺼졌을 수 있음)", NetErrors.reason(java.net.SocketTimeoutException("timeout")))
        assertEquals("서버 주소를 못 찾음(인터넷 확인)", NetErrors.reason(java.net.UnknownHostException("x")))
        assertEquals("서버 오류 HTTP 502(깨는 중이거나 꺼짐)", NetErrors.reason(Updater.UpdateException("업데이트 확인 실패 (HTTP 502)")))
        assertEquals("HTTP 404", NetErrors.reason(java.io.IOException("relay HTTP 404")))
    }
}
