package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-11 threads.com(로그인 안 함)에서 본 JSON 모양을 줄여 손으로 쓴 예시. 실제 글 내용은 넣지 않는다. */
class ThreadsParserTest {
    private fun post(pk: String, code: String, user: String, text: String, extra: String = "", info: String = "") =
        """{"pk":"$pk","code":"$code","taken_at":1800000000,"user":{"pk":"u$user","username":"$user"},
           "caption":{"text":"$text"},"like_count":7,"text_post_app_info":{"direct_reply_count":2$info}$extra}"""

    private val image = ""","image_versions2":{"candidates":[
        {"url":"https://cdn.example/big.webp","width":1440},{"url":"https://cdn.example/mid.webp","width":640},{"url":"https://cdn.example/small.webp","width":320}]}"""

    @Test
    fun `목록 묶음은 첫 글과 이어 쓴 글로 읽고 사진은 480 이상 중 가장 작은 것`() {
        val json = """{"data":{"mediaData":{"edges":[
            {"node":{"thread_items":[{"post":${post("1", "A1", "alice", "첫 글", image)}},{"post":${post("2", "A2", "alice", "이어서")}}]}},
            {"node":{"thread_items":[{"post":${post("3", "B1", "bob", "둘째 묶음")}}]}}]}}}"""
        val items = ThreadsParser.items(json)
        assertEquals(listOf("A1", "B1"), items.map { it.head.code })
        assertEquals(listOf("A2"), items[0].continuation.map { it.code })
        assertEquals(listOf("https://cdn.example/mid.webp"), items[0].head.images)
        assertEquals("alice", items[0].head.author)
        assertEquals(2, items[0].head.replyCount)
        assertEquals("https://www.threads.com/@alice/post/A1", items[0].head.url)
    }

    @Test
    fun `여러 장 사진과 동영상`() {
        val carousel = ""","carousel_media":[{"image_versions2":{"candidates":[{"url":"https://cdn.example/1.webp","width":720}]}},
            {"image_versions2":{"candidates":[{"url":"https://cdn.example/2.webp","width":720}]}}],"video_versions":[{"url":"x"}]"""
        val p = ThreadsParser.items("""{"thread_items":[{"post":${post("1", "A", "a", "t", carousel)}}]}""").single().head
        assertEquals(listOf("https://cdn.example/1.webp", "https://cdn.example/2.webp"), p.images)
        assertTrue(p.hasVideo)
    }

    @Test
    fun `글 페이지는 본문·이어 쓴 글·댓글로 나누고 관련 글은 뺀다`() {
        val page = """{"data":{"media":${post("1", "MAIN", "alice", "본문", info = """,
            "self_thread":{"posts":{"edges":[{"node":${post("2", "S1", "alice", "이어서")}}]}},
            "direct_replies":{"edges":[{"node":{"posts":{"edges":[{"node":${post("3", "R1", "bob", "댓글 하나")}}]}}},
                                      {"node":{"posts":{"edges":[{"node":${post("4", "R2", "carol", "댓글 둘")}}]}}}]}""")},
            "relatedPosts":{"threads":[{"thread_items":[{"post":${post("9", "X", "zed", "관련 글")}}]}]}}}"""
        val d = ThreadsParser.merge(null, ThreadsParser.detailPart(page, "MAIN"))!!
        assertEquals("본문", d.main.text)
        assertEquals(listOf("S1"), d.continuation.map { it.code })
        assertEquals(listOf("R1", "R2"), d.replies.map { it.code })
    }

    @Test
    fun `댓글을 더 받는 응답에는 본문이 없어도 이어 붙인다`() {
        val first = ThreadsParser.merge(null, ThreadsParser.detailPart("""{"media":${post("1", "MAIN", "alice", "본문")}}""", "MAIN"))
        val more = """{"data":{"media":{"text_post_app_info":{"direct_replies":{"edges":[{"node":{"posts":{"edges":[{"node":${post("5", "R3", "dan", "늦은 댓글")}}]}}}]}}}}}"""
        val merged = ThreadsParser.merge(first, ThreadsParser.detailPart(more, "MAIN"))!!
        assertEquals(listOf("R3"), merged.replies.map { it.code })
        assertEquals("MAIN", merged.main.code)
    }

    @Test
    fun `본문을 못 찾으면 목록에서 받은 글로 대신하고 그것도 없으면 null`() {
        val part = ThreadsParser.detailPart("""{"x":1}""", "MAIN")
        assertNull(ThreadsParser.merge(null, part))
        val fallback = ThreadPost("1", "MAIN", "alice", "목록의 글", 0)
        assertEquals(fallback, ThreadsParser.merge(null, part, fallbackMain = fallback)!!.main)
    }

    @Test
    fun `줄마다 따로 온 JSON과 for 접두어, 깨진 입력`() {
        val lines = "for (;;);" + """{"thread_items":[{"post":${post("1", "A", "a", "t").replace("\n", " ")}}]}""" + "\n" + """{"extensions":{}}"""
        assertEquals(1, ThreadsParser.items(lines).size)
        assertEquals(0, ThreadsParser.items("<html>not json</html>").size)
    }

    @Test
    fun `묶음에 섞인 다른 사람 글은 이어진 글로 넣지 않는다`() {
        val json = """{"thread_items":[{"post":${post("1", "A", "alice", "원글")}},{"post":${post("2", "B", "bob", "다른 사람 답글")}},{"post":${post("3", "C", "alice", "이어서")}}]}"""
        val item = ThreadsParser.items(json).single()
        assertEquals("A", item.head.code)
        assertEquals(listOf("C"), item.continuation.map { it.code })
    }
}
