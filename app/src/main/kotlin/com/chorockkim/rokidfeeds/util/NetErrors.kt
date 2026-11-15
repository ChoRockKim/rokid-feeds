package com.chorockkim.rokidfeeds.util

/**
 * 우리 서버(server-v2) 요청이 실패한 이유를 띠 위에 짧게(예외 원문은 영어·호스트 이름이라 길다).
 * 서버는 Railway에서 쓰지 않으면 잠들어서 첫 요청이 몇 초 늦다. 잠든 것과 꺼진 것을 헷갈리지 않게, 부르는 쪽은
 * 늦으면 "깨우는 중"을 먼저 보여 주고 몇 번 다시 한 뒤에만 실패를 띄운다.
 */
object NetErrors {
    /** 이만큼 넘게 걸리면 "서버 깨우는 중"으로 보여 준다. */
    const val WAKE_HINT_MS = 2_000L

    fun reason(e: Throwable?): String = when (e) {
        is java.net.UnknownHostException -> "서버 주소를 못 찾음(인터넷 확인)"
        is java.net.SocketTimeoutException -> "서버 응답 없음(꺼졌을 수 있음)"
        is java.net.ConnectException -> "서버에 연결 못 함"
        is javax.net.ssl.SSLException -> "보안 연결 실패"
        null -> "알 수 없음"
        else -> httpCode(e.message)?.let { if (it >= 500) "서버 오류 HTTP $it(깨는 중이거나 꺼짐)" else "HTTP $it" }
            ?: e.javaClass.simpleName
    }

    private fun httpCode(message: String?): Int? =
        message?.let { Regex("""HTTP (\d{3})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
}
