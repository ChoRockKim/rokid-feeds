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
    fun `긴 글은 여러 쪽이고 사진은 마지막 쪽이 짧으면 거기, 길면 다음 쪽에 따로`() {
        val long = List(40) { "문장 ${it}입니다." }.joinToString(" ")
        val pages = buildPostPages(ThreadItem(p("M", long, listOf("u"))), null)
        assertTrue(pages.size >= 2)
        assertTrue(pages.first().header && !pages[1].header)
        assertEquals(1, pages.count { it.photos.isNotEmpty() })
        assertTrue(pages.last().actions)
        assertTrue(pages.dropLast(1).none { it.actions })
        val withPhotos = pages.single { it.photos.isNotEmpty() }
        assertTrue(withPhotos.body.length <= WITH_PHOTOS_CHARS)
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
}
