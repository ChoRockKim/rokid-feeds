package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 값은 손으로 만든 가짜(실제 세션 값을 넣지 않는다). */
class CookieSnapshotTest {
    @Test
    fun `쿠키 머리글을 이름·값으로, 같은 이름은 앞의 것`() {
        assertEquals(listOf("a" to "1", "b" to "x=y"), CookieSnapshot.parse("a=1; b=x=y; a=2; =bad; noval"))
        assertEquals(emptyList<Pair<String, String>>(), CookieSnapshot.parse(null))
    }

    @Test
    fun `되살릴 때는 사이트 전체 도메인, 세션만 HttpOnly`() {
        val lines = CookieSnapshot.restoreLines("sessionid=S; csrftoken=C", ".threads.com")
        assertEquals("sessionid=S; Domain=.threads.com; Path=/; Max-Age=31536000; Secure; HttpOnly", lines[0])
        assertEquals("csrftoken=C; Domain=.threads.com; Path=/; Max-Age=31536000; Secure", lines[1])
    }

    @Test
    fun `저장본을 JSON으로 오가고 세션 쿠키로 로그인 여부를 본다`() {
        val saved = CookieSnapshot.encode(mapOf("https://www.threads.com/" to "ds_user_id=1; csrftoken=C", "https://www.instagram.com/" to null))
        val back = CookieSnapshot.decode(saved)
        assertEquals(mapOf("https://www.threads.com/" to "ds_user_id=1; csrftoken=C"), back)
        assertTrue(CookieSnapshot.loggedIn(back))
        assertFalse(CookieSnapshot.loggedIn(CookieSnapshot.decode(CookieSnapshot.encode(mapOf("https://www.threads.com/" to "csrftoken=C")))))
        assertEquals(emptyMap<String, String>(), CookieSnapshot.decode("not json"))
    }
}
