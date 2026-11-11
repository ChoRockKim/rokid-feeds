package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPagesTest {
    private fun p(code: String, author: String, text: String, images: List<String> = emptyList(), replies: Int = 0) =
        ThreadPost(code, code, author, text, 0, replyCount = replies, images = images)

    @Test
    fun `본문 → 사진 → 이어 쓴 글 → 댓글 순서이고 각 글 첫 쪽에 작성자가 붙는다`() {
        val main = p("M", "alice", "본문", images = listOf("u1", "u2"), replies = 1)
        val pages = buildThreadPages(ThreadItem(main), ThreadDetail(main, listOf(p("S", "alice", "이어서")), listOf(p("R", "bob", "댓글"))), loading = false)
        assertEquals(5, pages.size)
        assertEquals(main, (pages[0] as ThreadPage.Text).post)
        assertEquals(listOf("u1", "u2"), pages.subList(1, 3).map { (it as ThreadPage.Image).url })
        assertEquals("이어서", (pages[3] as ThreadPage.Text).body)
        val replies = pages[4] as ThreadPage.Replies
        assertEquals(listOf("bob"), replies.replies.map { it.author })
        assertEquals(0, replies.first)
        assertEquals(1, replies.total)
    }

    @Test
    fun `상세를 받는 동안은 안내 쪽, 못 받았으면 댓글 없음 안내`() {
        val main = p("M", "a", "본문", replies = 3)
        assertEquals("댓글 불러오는 중…", (buildThreadPages(ThreadItem(main), null, loading = true).last() as ThreadPage.Text).body)
        val failed = buildThreadPages(ThreadItem(main), ThreadDetail(main, emptyList(), emptyList()), loading = false)
        assertTrue((failed.last() as ThreadPage.Text).body.startsWith("댓글을 불러오지 못했습니다"))
    }

    @Test
    fun `긴 글은 여러 쪽, 이모지는 빠지고 사진만 있는 글은 사진 쪽만`() {
        val long = List(40) { "문장 ${it}입니다." }.joinToString(" ") + " 😀"
        val pages = buildThreadPages(ThreadItem(p("M", "a", long)), null, loading = false)
        assertTrue(pages.size > 1)
        assertTrue(pages.all { it is ThreadPage.Text && it.body.length <= THREAD_PAGE_CHARS && "\uD83D" !in it.body })
        val photoOnly = buildThreadPages(ThreadItem(p("M", "a", "", images = listOf("u"))), null, loading = false)
        assertEquals(listOf("u"), photoOnly.map { (it as ThreadPage.Image).url })
    }

    @Test
    fun `사진은 글마다 최대 개수까지만`() {
        val many = p("M", "a", "t", images = List(10) { "u$it" })
        assertEquals(MAX_IMAGES_PER_POST, buildThreadPages(ThreadItem(many), null, false).count { it is ThreadPage.Image })
    }

    @Test
    fun `짧은 댓글은 한 쪽에 최대 3개, 긴 댓글과 사진 댓글은 혼자`() {
        val main = p("M", "a", "본문")
        val replies = List(4) { p("R$it", "u$it", "짧은 댓글") } + p("L", "long", "긴 댓글 ".repeat(60)) + p("P", "pic", "사진", images = listOf("u"))
        val pages = buildThreadPages(ThreadItem(main), ThreadDetail(main, emptyList(), replies), loading = false).drop(1)
        assertEquals(listOf(3, 1), pages.filterIsInstance<ThreadPage.Replies>().map { it.replies.size })
        assertEquals(listOf(0, 3), pages.filterIsInstance<ThreadPage.Replies>().map { it.first })
        assertTrue(pages.filterIsInstance<ThreadPage.Text>().any { it.label == "댓글 5/6" })
        assertTrue(pages.filterIsInstance<ThreadPage.Image>().any { it.post.code == "P" })
    }
}
