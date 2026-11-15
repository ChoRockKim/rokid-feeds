package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.StoryNav
import com.chorockkim.rokidfeeds.nav.StoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryNavTest {
    // 세 사람: 2장, 아직 안 받음, 1장
    private val counts = listOf(2, null, 1)
    private fun r(s: StoryState, k: NavKey) = StoryNav.reduce(s, k, counts.size) { counts.getOrNull(it) }

    @Test
    fun `목록은 양끝 순환, 탭 = 보기, 뒤로 = 종료`() {
        assertEquals(2, r(StoryState(), NavKey.Previous).state.reel)
        assertEquals(0, r(StoryState(reel = 2), NavKey.Next).state.reel)
        assertEquals(StoryState(viewing = true, reel = 1), r(StoryState(reel = 1), NavKey.Select).state)
        assertTrue(r(StoryState(), NavKey.Back).exit)
        assertEquals(StoryState(), StoryNav.reduce(StoryState(), NavKey.Select, 0) { null }.state) // 빈 목록
    }

    @Test
    fun `다음 장 → 다음 사람, 안 받은 사람에서는 기다리고, 마지막 끝은 목록`() {
        val v = StoryState(viewing = true)
        assertEquals(1, r(v, NavKey.Next).state.item)
        assertEquals(StoryState(viewing = true, reel = 1), r(v.copy(item = 1), NavKey.Next).state)
        assertEquals(StoryState(viewing = true, reel = 1), r(StoryState(viewing = true, reel = 1), NavKey.Next).state)
        assertEquals(StoryState(viewing = false, reel = 2), r(StoryState(viewing = true, reel = 2), NavKey.Next).state)
    }

    @Test
    fun `이전은 이전 장·이전 사람, 탭은 멈춤, 뒤로는 목록(고른 사람 그대로)`() {
        assertEquals(0, r(StoryState(viewing = true, item = 1), NavKey.Previous).state.item)
        assertEquals(StoryState(viewing = true, reel = 0), r(StoryState(viewing = true, reel = 1), NavKey.Previous).state)
        assertTrue(r(StoryState(viewing = true), NavKey.Select).state.paused)
        assertEquals(StoryState(viewing = false, reel = 2), r(StoryState(viewing = true, reel = 2, item = 0, paused = true), NavKey.Back).state)
        // 넘어가면 멈춤이 풀린다
        assertEquals(false, r(StoryState(viewing = true, paused = true), NavKey.Next).state.paused)
    }
}
