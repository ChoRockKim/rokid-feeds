package com.chorockkim.rokidfeeds.threads

import org.json.JSONObject

/**
 * 로그인 버튼을 누른 뒤 메타 서버가 돌려준 결과 요약(`caa_login_web`, 2026-11-15 instagram.com Chromium에서 모양 확인).
 * 화면에는 실패면 늘 "로그인 정보가 잘못"이라고만 나와서(실제로는 캡차·본인 확인일 수 있다) 진짜 이유를 보여 주려고 받는다.
 * 브리지(`threads_bridge.js`)가 비밀번호·토큰 없이 아래 필드만 넘긴다. 순수 코드.
 */
data class LoginResult(
    val authenticated: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val captcha: Boolean = false,
    val twoFactor: Boolean = false,
    /** 서버가 보내려는 다음 페이지 경로(본인 확인 등). */
    val redirect: String? = null,
    val profileSelector: Boolean = false,
) {
    /** 실패 이유(한국어). 성공이거나 인증 코드 단계(앱이 따로 처리)면 null. */
    fun reason(): String? = when {
        authenticated || twoFactor -> null
        captcha || errorCode in CAPTCHA_CODES -> "인스타가 로봇 확인(캡차)을 요구해 로그인을 막았습니다 · 정보는 맞을 수 있습니다"
        redirect != null && ("challenge" in redirect || "checkpoint" in redirect || "auth_platform" in redirect) ->
            "인스타가 본인 확인을 요구합니다 · 인스타 앱·메일에서 '본인 맞음'을 누른 뒤 다시 ($redirect)"
        profileSelector -> "인스타가 계정 고르기 화면을 띄웠습니다 · 다시 보내 주세요"
        errorCode != null || errorMessage != null ->
            "인스타 응답: " + listOfNotNull(errorCode, errorMessage?.take(60)).joinToString(" · ")
        redirect != null -> "인스타가 다른 페이지로 보냈습니다 ($redirect)"
        else -> "인스타가 로그인을 거절했습니다(이유 없음) · 아이디·비밀번호를 확인해 주세요"
    }

    companion object {
        /** 캡차를 통과해야 하는 거절(1348009, 공개 프로젝트 mautrix/meta가 이렇게 다룸, 공식 문서 없음). 사용자 안경에서 받음(실기 2026-11-15). */
        val CAPTCHA_CODES = setOf("1348009")

        fun parse(json: String?): LoginResult? = runCatching {
            val o = JSONObject(json ?: return null)
            fun str(k: String) = o.optString(k).takeIf { it.isNotEmpty() && it != "null" }
            LoginResult(
                authenticated = o.optBoolean("auth"),
                errorCode = str("code"),
                errorMessage = str("msg"),
                captcha = o.optBoolean("captcha"),
                twoFactor = o.optBoolean("twofa"),
                redirect = str("redirect"),
                profileSelector = o.optBoolean("selector"),
            )
        }.getOrNull()
    }
}
