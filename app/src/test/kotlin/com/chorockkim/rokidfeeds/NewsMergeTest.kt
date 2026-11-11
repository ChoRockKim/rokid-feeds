package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.data.Article
import com.chorockkim.rokidfeeds.data.mergeArticles
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsMergeTest {
    private fun article(title: String, time: Long, source: String = "") =
        Article(title = title, link = "https://x.com/$title", summary = "", author = null, publishedAt = time, source = source)

    @Test
    fun `소스를 번갈아 한 개씩 끼워 넣는다`() {
        val merged = mergeArticles(
            listOf(
                listOf(article("a1", 100), article("a2", 50)),
                listOf(article("b1", 90), article("b2", 10)),
                listOf(article("c1", 120)),
            ),
        )
        assertEquals(listOf("a1", "b1", "c1", "a2", "b2"), merged.map { it.title })
    }

    @Test
    fun `시각이 늦은 소스도 앞쪽에 나온다`() {
        val news = (1..10).map { article("n$it", 1000L - it) }      // 몇 분 전 기사 10개
        val community = (1..3).map { article("g$it", 100L - it) }   // 몇 시간 전 글
        val merged = mergeArticles(listOf(news, community)).map { it.title }
        assertEquals(listOf("n1", "g1", "n2", "g2", "n3", "g3", "n4"), merged.take(7))
        assertEquals(13, merged.size)
    }

    @Test
    fun `소스 안에서는 최신순으로 정렬한다`() {
        val merged = mergeArticles(listOf(listOf(article("old", 1), article("new", 9))))
        assertEquals(listOf("new", "old"), merged.map { it.title })
    }

    @Test
    fun `시각을 모르는 기사는 그 소스의 맨 뒤로 보낸다`() {
        val merged = mergeArticles(listOf(listOf(article("unknown", 0), article("known", 5))))
        assertEquals(listOf("known", "unknown"), merged.map { it.title })
    }

    @Test
    fun `같은 차례에서는 입력 순서를 유지한다`() {
        val merged = mergeArticles(
            listOf(listOf(article("a", 7)), listOf(article("b", 7)), listOf(article("c", 7))),
        )
        assertEquals(listOf("a", "b", "c"), merged.map { it.title })
    }

    @Test
    fun `빈 입력이면 빈 목록`() {
        assertEquals(emptyList<Article>(), mergeArticles(emptyList()))
        assertEquals(emptyList<Article>(), mergeArticles(listOf(emptyList(), emptyList())))
    }
}
