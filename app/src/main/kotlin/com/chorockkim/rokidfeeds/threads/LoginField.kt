package com.chorockkim.rokidfeeds.threads

import org.json.JSONObject

/** 로그인 화면에서 고른 웹 입력칸: 비밀번호인지와 이름표("사용자 이름, 전화번호 또는 이메일 주소"). */
data class LoginField(val password: Boolean, val label: String) {
    companion object {
        /** `evaluateJavascript` 결과(JSON 문자열을 다시 따옴표로 감싼 것) → [LoginField]. 입력칸이 아니면 null. */
        fun parse(raw: String?): LoginField? = runCatching {
            val text = raw?.takeIf { it.length > 2 && it != "null" } ?: return null
            val inner = if (text.startsWith("\"")) org.json.JSONTokener(text).nextValue() as String else text
            if (inner.isBlank()) return null
            val o = JSONObject(inner)
            LoginField(password = o.optString("type") == "password", label = o.optString("label"))
        }.getOrNull()
    }
}
