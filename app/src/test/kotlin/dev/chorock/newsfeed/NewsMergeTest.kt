package dev.chorock.newsfeed

import dev.chorock.newsfeed.data.Article
import dev.chorock.newsfeed.data.mergeArticles
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsMergeTest {
    private fun article(title: String, time: Long, source: String = "") =
        Article(title = title, link = "https://x.com/$title", summary = "", author = null, publishedAt = time, source = source)

    @Test
    fun `언론사를 섞어 최신순으로 정렬한다`() {
        val merged = mergeArticles(
            listOf(
                listOf(article("a1", 100), article("a2", 50)),
                listOf(article("b1", 90), article("b2", 10)),
                listOf(article("c1", 120)),
            ),
        )
        assertEquals(listOf("c1", "a1", "b1", "a2", "b2"), merged.map { it.title })
    }

    @Test
    fun `시각을 모르는 기사는 맨 뒤로 보낸다`() {
        val merged = mergeArticles(listOf(listOf(article("unknown", 0), article("known", 5))))
        assertEquals(listOf("known", "unknown"), merged.map { it.title })
    }

    @Test
    fun `시각이 같으면 입력 순서를 유지한다`() {
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
