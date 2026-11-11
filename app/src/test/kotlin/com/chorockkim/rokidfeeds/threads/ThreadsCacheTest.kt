package com.chorockkim.rokidfeeds.threads

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadsCacheTest {
    private var now = 1_000_000L
    private val dir = Files.createTempDirectory("threads-cache").toFile()
    private val cache = ThreadsCache(dir) { now }

    private val post = ThreadPost("1", "A1", "alice", "본문", 1800000000, likeCount = 3, replyCount = 2,
        images = listOf("https://x/1"), thumbs = listOf("https://x/t1"), hasVideo = true, avatar = null)
    private val item = ThreadItem(post, listOf(post.copy(pk = "2", code = "A2", text = "이어서")))

    @Test
    fun `목록과 글 상세를 그대로 되살린다`() {
        cache.saveFeed(listOf(item))
        assertEquals(listOf(item), cache.loadFeed()!!.value)
        val detail = ThreadDetail(post, item.continuation, listOf(post.copy(pk = "9", code = "R1", author = "bob", text = "댓글")))
        cache.saveDetail("A1", detail)
        assertEquals(detail, cache.loadDetail("A1")!!.value)
        assertEquals(now, cache.loadDetail("A1")!!.savedAt)
    }

    @Test
    fun `하루가 지나면 없는 것으로, 없는 글은 null`() {
        cache.saveFeed(listOf(item))
        cache.saveDetail("A1", ThreadDetail(post, emptyList(), emptyList()))
        now += ThreadsCache.MAX_AGE_MS + 1
        assertNull(cache.loadFeed())
        assertNull(cache.loadDetail("A1"))
        assertNull(cache.loadDetail("nope"))
    }

    @Test
    fun `글 코드에 이상한 글자가 있어도 저장소 밖으로 나가지 않는다`() {
        cache.saveDetail("../../evil", ThreadDetail(post, emptyList(), emptyList()))
        assertEquals(setOf("post-evil.json"), dir.list()!!.toSet())
    }
}
