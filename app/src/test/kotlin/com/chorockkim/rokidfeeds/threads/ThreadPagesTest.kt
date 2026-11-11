package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPagesTest {
    private fun p(code: String, text: String, images: List<String> = emptyList()) =
        ThreadPost(code, code, "a", text, 0, images = images, thumbs = images.map { "t-$it" })

    @Test
    fun `짧은 글은 한 쪽에 본문과 사진 줄과 아이콘 줄`() {
        val pages = buildPostPages(ThreadItem(p("M", "짧은 글", listOf("u1", "u2"))), null)
        assertEquals(1, pages.size)
        assertEquals(listOf("u1", "u2"), pages[0].photos)
        assertEquals(listOf("t-u1", "t-u2"), pages[0].thumbs)
        assertTrue(pages[0].header && pages[0].actions)
    }

    @Test
    fun `긴 글도 사진 줄은 첫 쪽에만, 사진만 있는 쪽은 없고 모든 쪽에서 탭하면 그 글의 사진`() {
        val long = List(40) { "문장 ${it}입니다." }.joinToString(" ")
        val pages = buildPostPages(ThreadItem(p("M", long, listOf("u"))), null)
        assertTrue(pages.size >= 2)
        assertEquals(listOf("t-u"), pages.first().thumbs)
        assertTrue(pages.drop(1).all { it.thumbs.isEmpty() })
        assertTrue(pages.none { it.body.isBlank() })
        assertTrue(pages.all { it.photos == listOf("u") })
        assertTrue(pages.first().body.length <= WITH_PHOTOS_CHARS)
        assertTrue(pages.first().header && pages.drop(1).none { it.header })
        assertTrue(pages.last().actions && pages.dropLast(1).none { it.actions })
    }

    @Test
    fun `이어 쓴 글은 본문 다음에, 이모지는 빼고, 상세가 오면 상세의 본문을 쓴다`() {
        val head = p("M", "목록의 본문 😀")
        val pages = buildPostPages(ThreadItem(head, listOf(p("S", "이어서"))), null)
        assertEquals(listOf("목록의 본문", "이어서"), pages.map { it.body })
        assertTrue(pages.all { it.header })
        val detail = ThreadDetail(p("M", "상세 본문"), listOf(p("S", "이어서"), p("S2", "또 이어서")), emptyList())
        assertEquals(listOf("상세 본문", "이어서", "또 이어서"), buildPostPages(ThreadItem(head), detail).map { it.body })
    }

    @Test
    fun `글 없이 사진만, 아무것도 없으면 안내`() {
        assertEquals(listOf("u"), buildPostPages(ThreadItem(p("M", "", listOf("u"))), null).single().photos)
        assertEquals("(내용 없음)", buildPostPages(ThreadItem(p("M", "")), null).single().body)
    }

    @Test
    fun `이어진 글 쪽은 몇 번째인지 알고, 다른 작성자 글은 빼낸다`() {
        val main = p("M", "본문")
        val other = ThreadPost("X", "X", "someone.else", "다른 사람 글", 0)
        val pages = buildPostPages(ThreadItem(main, listOf(p("S", "이어서"), other)), null)
        assertEquals(listOf("본문", "이어서"), pages.map { it.body })
        assertEquals(listOf(0 to 2, 1 to 2), pages.map { it.part to it.parts })
    }
}
