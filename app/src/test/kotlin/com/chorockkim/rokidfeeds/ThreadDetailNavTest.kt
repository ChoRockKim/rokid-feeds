package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.DetailPos
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.ThreadDetailNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadDetailNavTest {
    // 쪽: 본문, 사진, 댓글 3개, 댓글 2개
    private val sizes = listOf(0, 0, 3, 2)
    private fun go(pos: DetailPos, vararg keys: NavKey) = keys.fold(pos) { p, k -> ThreadDetailNav.reduce(p, k, sizes).pos }

    @Test
    fun `다음은 댓글을 하나씩 고르고 쪽 끝에서 다음 쪽, 맨 끝에서 멈춘다`() {
        assertEquals(DetailPos(2, 0), go(DetailPos(), NavKey.Next, NavKey.Next))
        assertEquals(DetailPos(2, 2), go(DetailPos(2, 0), NavKey.Next, NavKey.Next))
        assertEquals(DetailPos(3, 0), go(DetailPos(2, 2), NavKey.Next))
        assertEquals(DetailPos(3, 1), go(DetailPos(3, 0), NavKey.Next, NavKey.Next, NavKey.Next))
    }

    @Test
    fun `이전은 앞 쪽의 마지막 댓글로 들어가고 맨 앞에서 멈춘다`() {
        assertEquals(DetailPos(2, 2), go(DetailPos(3, 0), NavKey.Previous))
        assertEquals(DetailPos(1, 0), go(DetailPos(2, 0), NavKey.Previous))
        assertEquals(DetailPos(0, 0), go(DetailPos(1, 0), NavKey.Previous, NavKey.Previous))
    }

    @Test
    fun `댓글 쪽에서 선택하면 그 댓글을 열고, 다른 쪽에서는 다음 쪽`() {
        assertEquals(1, ThreadDetailNav.reduce(DetailPos(2, 1), NavKey.Select, sizes).openReply)
        val onText = ThreadDetailNav.reduce(DetailPos(0, 0), NavKey.Select, sizes)
        assertNull(onText.openReply)
        assertEquals(DetailPos(1, 0), onText.pos)
    }

    @Test
    fun `뒤로는 back, 댓글이 늦게 와 쪽이 줄어도 범위 안으로`() {
        assertTrue(ThreadDetailNav.reduce(DetailPos(2, 1), NavKey.Back, sizes).back)
        assertEquals(DetailPos(1, 0), ThreadDetailNav.normalize(DetailPos(5, 4), listOf(0, 0)))
        assertEquals(DetailPos(2, 2), ThreadDetailNav.normalize(DetailPos(2, 9), sizes))
    }
}
