package dev.chorock.newsfeed

import com.sun.net.httpserver.HttpServer
import dev.chorock.newsfeed.data.FeedSource
import dev.chorock.newsfeed.data.NewsApi
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 로컬 HTTP 서버로 통합·부분 실패·전체 실패 경로를 확인한다. */
class NewsApiTest {
    private lateinit var server: HttpServer
    private val base get() = "http://127.0.0.1:${server.address.port}"

    private fun rss(vararg items: Pair<String, String>) =
        """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel>${
            items.joinToString("") { (title, date) ->
                "<item><title>$title</title><link>https://x.com/$title</link><pubDate>$date</pubDate></item>"
            }
        }</channel></rss>"""

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/a") { ex ->
            val body = rss("a-new" to "Mon, 19 Oct 2026 16:00:00 +0900", "a-old" to "Mon, 19 Oct 2026 10:00:00 +0900")
                .toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
        }
        server.createContext("/b") { ex ->
            val body = rss("b-mid" to "Mon, 19 Oct 2026 13:00:00 +0900").toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
        }
        server.createContext("/bad") { ex -> ex.sendResponseHeaders(500, -1); ex.close() }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    @Test
    fun `세 언론사를 합쳐 최신순으로 정렬하고 언론사 이름을 붙인다`() = runBlocking {
        val feed = NewsApi(listOf(FeedSource("A사", "$base/a"), FeedSource("B사", "$base/b"))).feed()
        assertEquals(listOf("a-new", "b-mid", "a-old"), feed.articles.map { it.title })
        assertEquals(listOf("A사", "B사", "A사"), feed.articles.map { it.source })
        assertTrue(feed.failedSources.isEmpty())
    }

    @Test
    fun `일부 언론사가 실패해도 나머지 기사와 실패한 이름을 돌려준다`() = runBlocking {
        val feed = NewsApi(listOf(FeedSource("A사", "$base/a"), FeedSource("고장사", "$base/bad"))).feed()
        assertEquals(listOf("a-new", "a-old"), feed.articles.map { it.title })
        assertEquals(listOf("고장사"), feed.failedSources)
    }

    @Test
    fun `전부 실패하면 예외를 던진다`() {
        val api = NewsApi(listOf(FeedSource("고장1", "$base/bad"), FeedSource("고장2", "$base/bad")))
        val error = assertThrows(IllegalStateException::class.java) { runBlocking { api.feed() } }
        assertTrue(error.message.orEmpty().contains("HTTP 500"))
    }

    @Test
    fun `언론사별 가져오는 개수를 제한한다`() = runBlocking {
        val feed = NewsApi(listOf(FeedSource("A사", "$base/a"))).feed(perSource = 1)
        assertEquals(listOf("a-new"), feed.articles.map { it.title })
    }
}
