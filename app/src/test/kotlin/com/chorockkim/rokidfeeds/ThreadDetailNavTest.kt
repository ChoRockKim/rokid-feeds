package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.DetailState
import com.chorockkim.rokidfeeds.nav.Mode
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.ThreadDetailNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadDetailNavTest {
    // 글 2쪽(둘째 쪽에 사진 3장), 댓글 4개
    private val photos = listOf(0, 3)
    private val comments = 4
    private fun go(s: DetailState, vararg keys: NavKey) = keys.fold(s) { st, k -> ThreadDetailNav.reduce(st, k, photos, comments).state }

    @Test
    fun `글을 아래로 넘기면 다음 쪽, 마지막 쪽 다음은 댓글 목록`() {
        assertEquals(DetailState(page = 1), go(DetailState(), NavKey.Next))
        assertEquals(DetailState(Mode.Comments, page = 1, comment = 0), go(DetailState(), NavKey.Next, NavKey.Next))
        assertEquals(DetailState(Mode.Comments, page = 1, comment = 3), go(DetailState(page = 1), NavKey.Next, NavKey.Next, NavKey.Next, NavKey.Next, NavKey.Next, NavKey.Next))
    }

    @Test
    fun `사진이 있는 쪽에서 탭하면 사진 크게, 위아래로 다른 사진, 뒤로는 글`() {
        val viewer = go(DetailState(page = 1), NavKey.Select)
        assertEquals(DetailState(Mode.Photos, page = 1, photo = 0), viewer)
        assertEquals(2, go(viewer, NavKey.Next, NavKey.Next, NavKey.Next).photo) // 끝에서 멈춤
        assertEquals(0, go(viewer, NavKey.Previous).photo)
        assertEquals(DetailState(Mode.Post, page = 1, photo = 1), go(viewer, NavKey.Next, NavKey.Back))
    }

    @Test
    fun `사진 없는 쪽의 탭은 다음 쪽`() {
        assertEquals(DetailState(page = 1), go(DetailState(page = 0), NavKey.Select))
    }

    @Test
    fun `댓글에서 탭은 그 댓글 열기, 뒤로는 글 첫 쪽, 맨 위에서 위로는 글 마지막 쪽`() {
        val at2 = DetailState(Mode.Comments, page = 1, comment = 2)
        assertEquals(2, ThreadDetailNav.reduce(at2, NavKey.Select, photos, comments).openComment)
        assertEquals(DetailState(Mode.Post, page = 0, comment = 2), go(at2, NavKey.Back))
        assertEquals(DetailState(Mode.Post, page = 1, comment = 0), go(DetailState(Mode.Comments, page = 1, comment = 0), NavKey.Previous))
    }

    @Test
    fun `글에서 뒤로는 밖으로, 댓글이 없으면 글 끝에서 멈춘다`() {
        assertTrue(ThreadDetailNav.reduce(DetailState(), NavKey.Back, photos, comments).back)
        val noComments = ThreadDetailNav.reduce(DetailState(page = 1), NavKey.Next, photos, 0)
        assertEquals(DetailState(page = 1), noComments.state)
        assertNull(ThreadDetailNav.reduce(DetailState(Mode.Comments), NavKey.Select, photos, 0).openComment)
    }

    @Test
    fun `상세가 늦게 와 쪽·댓글이 바뀌어도 범위 안으로`() {
        assertEquals(DetailState(Mode.Post, page = 0), ThreadDetailNav.normalize(DetailState(Mode.Comments, page = 3, comment = 7), listOf(0), 0))
        assertEquals(DetailState(Mode.Comments, page = 1, comment = 3), ThreadDetailNav.normalize(DetailState(Mode.Comments, page = 1, comment = 9), photos, 4))
        assertEquals(DetailState(Mode.Post, page = 0), ThreadDetailNav.normalize(DetailState(Mode.Photos, page = 0, photo = 2), photos, 4))
    }

    @Test
    fun `길쭉한 사진은 탭하면 위 조각, 쓸기로 가운데·아래(끝에서 멈춤), 뒤로 사진, 뒤로 글`() {
        val tall: (Int, Int) -> Boolean = { page, photo -> page == 1 && photo == 1 }
        fun step(s: DetailState, k: NavKey) = ThreadDetailNav.reduce(s, k, photos, comments, tall).state
        val zoom = step(DetailState(Mode.Photos, page = 1, photo = 1), NavKey.Select)
        assertEquals(DetailState(Mode.Zoom, page = 1, photo = 1, part = 0), zoom)
        assertEquals(listOf(1, 2, 2), generateSequence(zoom) { step(it, NavKey.Next) }.drop(1).take(3).map { it.part }.toList())
        assertEquals(1, step(step(step(zoom, NavKey.Next), NavKey.Next), NavKey.Previous).part)
        val back = step(step(zoom, NavKey.Next), NavKey.Back)
        assertEquals(DetailState(Mode.Photos, page = 1, photo = 1, part = 0), back)
        assertEquals(Mode.Post, step(back, NavKey.Back).mode)
        assertEquals(DetailState(Mode.Photos, page = 1, photo = 1), step(DetailState(Mode.Photos, page = 1, photo = 0), NavKey.Select))
    }

    @Test
    fun `세 조각은 사진 전체를 덮고 서로 살짝 겹친다`() {
        val h = 1000
        val w = (0 until 3).map { com.chorockkim.rokidfeeds.ui.zoomWindow(h, it, 3) }
        assertEquals(0, w[0].first)
        assertEquals(h, w[2].first + w[2].second)
        assertTrue(w[0].first + w[0].second > w[1].first && w[1].first + w[1].second > w[2].first)
    }
}
