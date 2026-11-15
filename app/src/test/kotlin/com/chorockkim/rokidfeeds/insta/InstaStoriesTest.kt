package com.chorockkim.rokidfeeds.insta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 공개 자료로 짐작한 모양을 줄여 손으로 쓴 예시(실제 내용·주소 아님). */
class InstaStoriesTest {
    private fun img(name: String) = """"image_versions2":{"candidates":[{"url":"https://cdn.example/$name.jpg","width":720,"height":1280}]}"""

    @Test
    fun `목록은 사람·시각·봤는지만, 장은 비어 있다`() {
        val json = """{"data":{"xdt_api__v1__feed__reels_tray":{"tray":[
            {"id":"1","latest_reel_media":1800000300,"seen":0,"user":{"pk":"1","username":"alice"}},
            {"id":"2","latest_reel_media":1800000100,"seen":1800000100,"user":{"pk":"2","username":"bob"}}]}}}"""
        val r = InstaStories.reels(json)
        assertEquals(listOf("alice", "bob"), r.map { it.username })
        assertFalse(r[0].seen)
        assertTrue(r[1].seen)
        assertTrue(r.all { it.items.isEmpty() })
        assertEquals("https://www.instagram.com/stories/alice/", r[0].url)
    }

    @Test
    fun `스토리 페이지의 reels_media에서 사진·동영상 장을 읽는다`() {
        val json = """{"data":{"xdt_api__v1__feed__reels_media":{"reels_media":[
            {"id":"1","user":{"pk":"1","username":"alice"},"items":[
                {"pk":"11","taken_at":1800000000,"media_type":1,${img("p")}},
                {"pk":"12","taken_at":1800000200,"media_type":2,${img("v")},"video_versions":[{"type":101,"url":"https://cdn.example/v.mp4"}]},
                {"pk":"13","media_type":1}]},
            {"id":"2","user":{"pk":"2","username":"bob"},"items":[{"pk":"21","taken_at":1800000100,${img("b")}}]}]}}}"""
        val r = InstaStories.reels(json)
        assertEquals(listOf("alice", "bob"), r.map { it.username })
        assertEquals(listOf("11", "12"), r[0].items.map { it.pk })
        assertNull(r[0].items[0].video)
        assertEquals("https://cdn.example/v.mp4", r[0].items[1].video)
        assertEquals("https://cdn.example/p.jpg", r[0].items[0].image)
        assertEquals(1800000200, r[0].latest) // latest_reel_media가 없으면 마지막 장 시각
    }

    @Test
    fun `게시물은 스토리로 보지 않고, 합칠 때는 장이 많은 쪽`() {
        val post = """{"pk":"9","code":"C","taken_at":1,"user":{"username":"x"},"items":[]}"""
        assertTrue(InstaStories.reels(post).isEmpty())
        val a = StoryReel("1", "alice", 5, items = listOf(StoryItem("1", 1, "i")))
        val b = StoryReel("1", "alice", 6)
        assertEquals(1, InstaStories.merge(a, b).items.size)
        assertEquals(6, InstaStories.merge(a, b).latest)
    }
}
