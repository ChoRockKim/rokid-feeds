package com.chorockkim.rokidfeeds.threads

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 폰 링크 로그인 중계(rokid-conversate 서버 `server/conversate/relay.py`) 클라이언트.
 * 안경이 세션을 만들어 6자리 코드를 보여 주면, 폰에서 `<base>/login`을 열어 코드·아이디·비밀번호(필요하면 인증 코드)를 보낸다.
 * 안경은 [poll]로 받아 간다(서버는 넘겨준 뒤 바로 지운다). 받은 값은 WebView 폼에 넣을 때만 쓰고 보관하지 않는다.
 */
class LoginRelay(private val base: String) {
    data class Session(val code: String, val secret: String)

    sealed interface Message {
        data class Credentials(val username: String, val password: String) : Message
        data class Code(val text: String) : Message
    }

    /** 폰에서 열 주소(스킴 없이 보여 준다: 안경 화면에서 읽고 폰에 치기 쉽게). */
    val phoneUrl: String get() = base.removePrefix("https://").removePrefix("http://") + "/login"

    suspend fun newSession(): Session = withContext(Dispatchers.IO) {
        val o = JSONObject(request("POST", "/relay/new", null, null))
        Session(o.getString("code"), o.getString("secret"))
    }

    suspend fun poll(session: Session): List<Message> = withContext(Dispatchers.IO) {
        parseMessages(request("GET", "/relay/poll", session.secret, null))
    }

    /** 폰 페이지에 보여 줄 상태: waiting, logging_in, need_code, done, error. */
    suspend fun status(session: Session, state: String, message: String = "") = withContext(Dispatchers.IO) {
        request("POST", "/relay/status", session.secret, JSONObject().put("state", state).put("message", message).toString())
        Unit
    }

    private fun request(method: String, path: String, secret: String?, body: String?): String {
        val c = URL(base + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 10_000
            // 서버가 잠들어 있으면(Railway App Sleeping) 첫 요청이 깨우느라 늦다.
            c.readTimeout = 30_000
            secret?.let { c.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            if (code == 401) throw ExpiredException()
            check(code in 200..299) { "로그인 중계 서버 오류 (HTTP $code)" }
            return c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            c.disconnect()
        }
    }

    /** 세션이 만료됨(10분). 새로 만들어야 한다. */
    class ExpiredException : IllegalStateException("로그인 코드가 만료됐습니다")

    companion object {
        fun parseMessages(json: String): List<Message> {
            val list = JSONObject(json).optJSONArray("messages") ?: return emptyList()
            return (0 until list.length()).mapNotNull { i ->
                val m = list.optJSONObject(i) ?: return@mapNotNull null
                when (m.optString("kind")) {
                    "credentials" -> Message.Credentials(m.optString("username"), m.optString("password"))
                        .takeIf { it.username.isNotEmpty() && it.password.isNotEmpty() }
                    "code" -> m.optString("text").takeIf { it.isNotEmpty() }?.let { Message.Code(it) }
                    else -> null
                }
            }
        }
    }
}
