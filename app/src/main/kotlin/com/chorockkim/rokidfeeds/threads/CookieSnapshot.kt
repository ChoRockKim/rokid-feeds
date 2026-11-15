package com.chorockkim.rokidfeeds.threads

import org.json.JSONObject

/**
 * 계정 바꿔 끼우기(방법 B)용 쿠키 저장본. 안경 WebView가 프로필을 지원하지 않아(사용자 확인 2026-11-15) 계정마다
 * threads.com·instagram.com 쿠키를 앱 저장소에 보관했다가, 바꿀 때 WebView 쿠키를 지우고 그 계정 것을 다시 넣는다.
 *
 * CookieManager.getCookie는 `이름=값; 이름=값`만 준다(도메인·만료·HttpOnly는 없다). 그래서 되살릴 때는 사이트 전체
 * 도메인(`.threads.com`)·경로 `/`·Secure·1년으로 넣는다. 세션 쿠키(sessionid)만 HttpOnly로 둔다(페이지 JS가 csrftoken을 읽는다).
 * Android 없는 순수 코드.
 */
object CookieSnapshot {
    /** 보관할 사이트: 받을 주소 → 되살릴 도메인. */
    val SITES = linkedMapOf(
        "https://www.threads.com/" to ".threads.com",
        "https://www.instagram.com/" to ".instagram.com",
    )

    private val HTTP_ONLY = setOf("sessionid")
    private const val MAX_AGE_S = 365L * 24 * 60 * 60

    /** "a=1; b=2" → [(a,1),(b,2)]. 같은 이름이 둘이면(도메인이 달라) 앞의 것. */
    fun parse(header: String?): List<Pair<String, String>> =
        header.orEmpty().split(';').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i).trim() to part.substring(i + 1).trim()
        }.filter { it.first.isNotEmpty() }.distinctBy { it.first }

    /** 되살릴 Set-Cookie 줄들. */
    fun restoreLines(header: String?, domain: String): List<String> = parse(header).map { (name, value) ->
        "$name=$value; Domain=$domain; Path=/; Max-Age=$MAX_AGE_S; Secure" + if (name in HTTP_ONLY) "; HttpOnly" else ""
    }

    /** 사이트 주소 → 쿠키 머리글을 JSON으로(앱 내부 저장소에만 둔다). */
    fun encode(bySite: Map<String, String?>): String =
        JSONObject().apply { bySite.forEach { (k, v) -> if (!v.isNullOrBlank()) put(k, v) } }.toString()

    fun decode(json: String?): Map<String, String> = runCatching {
        val o = JSONObject(json ?: return emptyMap())
        o.keys().asSequence().associateWith { o.optString(it) }
    }.getOrDefault(emptyMap())

    /** 저장본으로 로그인돼 있는지(threads 세션 쿠키). */
    fun loggedIn(bySite: Map<String, String>): Boolean =
        parse(bySite[SITES.keys.first()]).any { it.first == "sessionid" || it.first == "ds_user_id" }
}
