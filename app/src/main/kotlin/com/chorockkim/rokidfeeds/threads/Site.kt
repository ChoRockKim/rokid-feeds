package com.chorockkim.rokidfeeds.threads

import android.net.Uri

/**
 * 숨은 WebView([ThreadsWeb])가 여는 사이트. 둘 다 메타 웹이라 데이터 모양(글·사진·동영상 필드)과 로그인 폼이 비슷해
 * WebView·로그인(폰 링크)·쿠키 바꿔 끼우기를 같이 쓴다.
 */
enum class Site(val home: String, val login: String, private val hosts: List<String>) {
    Threads("https://www.threads.com/", "https://www.threads.com/login", listOf("threads.com", "threads.net")),
    /** 인스타는 스토리만(사용자 요청 2026-11-15). 로그인은 instagram.com 로그인 폼. */
    Instagram("https://www.instagram.com/", "https://www.instagram.com/accounts/login/", listOf("instagram.com"));

    /** 이 사이트 주소인지(그 주소에서 온 JSON만 받는다). */
    fun owns(uri: Uri): Boolean = uri.scheme == "https" && uri.host.let { h -> h != null && hosts.any { h == it || h.endsWith(".$it") } }
}
