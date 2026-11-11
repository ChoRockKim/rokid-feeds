package com.chorockkim.rokidfeeds

import com.sun.net.httpserver.HttpServer
import com.chorockkim.rokidfeeds.data.BoundedHttp
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BoundedHttpTest {
    private lateinit var server: HttpServer
    private val base get() = "http://127.0.0.1:${server.address.port}"

    private fun reply(path: String, status: Int, body: String = "", headers: Map<String, String> = emptyMap()) {
        server.createContext(path) { ex ->
            headers.forEach { (k, v) -> ex.responseHeaders.add(k, v) }
            val bytes = body.toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) ex.responseBody.use { it.write(bytes) } else ex.close()
        }
    }

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        reply("/ok", 200, "안녕 feed")
        reply("/go-ok", 302, headers = mapOf("Location" to "/ok"))
        reply("/no-location", 302)
        reply("/loop", 302, headers = mapOf("Location" to "/loop"))
        reply("/big", 200, "x".repeat(10_000))
        reply("/ftp", 302, headers = mapOf("Location" to "ftp://example.com/feed"))
        reply("/missing", 404, "nope")
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    private fun fetch(path: String, http: BoundedHttp = BoundedHttp()) = runBlocking { http.fetch("$base$path") }

    @Test
    fun `정상 응답은 UTF-8로 읽는다`() = assertEquals("안녕 feed", fetch("/ok"))

    @Test
    fun `상대 Location을 따라간다`() = assertEquals("안녕 feed", fetch("/go-ok"))

    @Test
    fun `리다이렉트가 한도를 넘으면 실패한다`() {
        val error = assertThrows(IllegalStateException::class.java) { fetch("/loop", BoundedHttp(maxRedirects = 4)) }
        assertTrue(error.message.orEmpty().contains("너무 많습니다"))
    }

    @Test
    fun `한도 안의 리다이렉트는 따라간다`() {
        // /go-ok는 1번 리다이렉트한다. 한도 1이면 통과, 0이면 실패
        assertEquals("안녕 feed", fetch("/go-ok", BoundedHttp(maxRedirects = 1)))
        assertThrows(IllegalStateException::class.java) { fetch("/go-ok", BoundedHttp(maxRedirects = 0)) }
    }

    @Test
    fun `Location 없는 리다이렉트는 실패한다`() {
        val error = assertThrows(IllegalStateException::class.java) { fetch("/no-location") }
        assertTrue(error.message.orEmpty().contains("Location"))
    }

    @Test
    fun `허용하지 않는 스킴으로의 리다이렉트는 거부한다`() {
        val error = assertThrows(IllegalStateException::class.java) { fetch("/ftp") }
        assertTrue(error.message.orEmpty().contains("스킴"))
    }

    @Test
    fun `본문이 한도를 넘으면 실패하고 한도 이하면 통과한다`() {
        val error = assertThrows(IllegalStateException::class.java) { fetch("/big", BoundedHttp(maxBodyBytes = 5_000)) }
        assertTrue(error.message.orEmpty().contains("너무 큽니다"))
        assertEquals(10_000, fetch("/big", BoundedHttp(maxBodyBytes = 10_000)).length)
    }

    @Test
    fun `200이 아니면 상태 코드와 함께 실패한다`() {
        val error = assertThrows(IllegalStateException::class.java) { fetch("/missing") }
        assertTrue(error.message.orEmpty().contains("HTTP 404"))
    }

    // ---- nextUrl (순수) ----

    @Test
    fun `nextUrl은 상대 경로를 현재 URL 기준으로 해석한다`() {
        assertEquals("https://a.com/x/b.xml", BoundedHttp.nextUrl("https://a.com/x/a.xml", "b.xml"))
        assertEquals("https://a.com/b.xml", BoundedHttp.nextUrl("https://a.com/x/a.xml", "/b.xml"))
        assertEquals("https://b.com/c", BoundedHttp.nextUrl("https://a.com/x", "https://b.com/c"))
    }

    @Test
    fun `nextUrl은 http에서 https 업그레이드를 허용한다`() {
        assertEquals("https://a.com/feed", BoundedHttp.nextUrl("http://a.com/feed", "https://a.com/feed"))
    }

    @Test
    fun `nextUrl은 https에서 http 다운그레이드를 거부한다`() {
        assertThrows(IllegalStateException::class.java) { BoundedHttp.nextUrl("https://a.com/feed", "http://a.com/feed") }
    }

    @Test
    fun `nextUrl은 ftp·file·javascript 스킴을 거부한다`() {
        for (location in listOf("ftp://a.com/x", "file:///etc/passwd", "javascript:alert(1)")) {
            assertThrows(location, IllegalStateException::class.java) { BoundedHttp.nextUrl("https://a.com/feed", location) }
        }
    }
}
