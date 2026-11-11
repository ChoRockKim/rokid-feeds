package com.chorockkim.rokidfeeds.data

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 원격 서버가 정하는 모든 축을 제한한 GET: 타임아웃, 리다이렉트 횟수, 본문 크기.
 * (Rokid Nexus의 news-nexus `NewsHttpClient`와 같은 원칙. gzip은 직접 다루지 않는다 — Android `HttpURLConnection`이
 * `Accept-Encoding`을 직접 지정하지 않으면 알아서 풀어 준다.)
 */
class BoundedHttp(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 8_000,
    private val maxBodyBytes: Int = 4 * 1024 * 1024,
    private val maxRedirects: Int = 4,
    private val userAgent: String = "news-feed/0.1 (Android)",
) {
    /** 본문을 UTF-8 문자열로 돌려준다. 실패는 예외로 올린다. 리다이렉트는 최대 [maxRedirects]번만 따라간다. */
    // 확인한 세 피드는 모두 UTF-8이다.
    suspend fun fetch(url: String): String = fetchBytes(url).toString(Charsets.UTF_8)

    /** 본문을 바이트로 돌려준다(Threads 사진). 상한은 [fetch]와 같다. */
    suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        var current = url
        for (hop in 0..maxRedirects) {
            val connection = open(current)
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: error("Location 없는 리다이렉트: HTTP $status")
                    current = nextUrl(current, location)
                    continue
                }
                check(status == 200) { "HTTP $status: $current" }
                return@withContext connection.inputStream.use { readBounded(it) }
            } finally {
                connection.disconnect()
            }
        }
        error("리다이렉트가 너무 많습니다 (최대 ${maxRedirects}회)")
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false // 횟수와 스킴을 직접 통제한다
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("User-Agent", userAgent)
        return connection
    }

    private fun readBounded(stream: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            check(out.size() + read <= maxBodyBytes) { "응답이 너무 큽니다 (최대 ${maxBodyBytes / 1024}KiB)" }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    companion object {
        /**
         * 리다이렉트 대상 URL을 정한다. 상대 `Location`은 [current] 기준으로 해석한다.
         * http/https 외 스킴과 https→http 다운그레이드는 거부한다.
         */
        fun nextUrl(current: String, location: String): String {
            val base = URI(current)
            val target = base.resolve(location.trim())
            val scheme = target.scheme?.lowercase()
            check(scheme == "http" || scheme == "https") { "허용하지 않는 리다이렉트 스킴: $scheme" }
            check(!(base.scheme.equals("https", ignoreCase = true) && scheme == "http")) {
                "https에서 http로의 리다이렉트는 거부합니다"
            }
            return target.toString()
        }
    }
}
