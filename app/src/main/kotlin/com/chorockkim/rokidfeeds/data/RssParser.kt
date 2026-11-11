package com.chorockkim.rokidfeeds.data

import com.chorockkim.rokidfeeds.util.htmlToText
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * RSS 2.0 `<item>`과 Atom `<entry>`를 [Article]로 바꾼다(GeekNews가 Atom).
 *
 * XML 파서 대신 정규식으로 `<item>` 단위만 잘라 읽는다. 외부 피드를 받는 앱이라 DTD/엔티티 해석(XXE)을 아예 하지 않고,
 * JVM 단위 테스트에서도 같은 코드가 돈다. 대신 중첩된 같은 이름의 태그나 네임스페이스 별칭은 다루지 않는다.
 */
object RssParser {
    private val ITEM = Regex("<(item|entry)[\\s>].*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val ATOM_LINK = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val HREF = Regex("href\\s*=\\s*['\"]([^'\"]*)['\"]")
    private val CDATA = Regex("<!\\[CDATA\\[(.*?)]]>", RegexOption.DOT_MATCHES_ALL)

    fun parse(xml: String, limit: Int = Int.MAX_VALUE): List<Article> =
        ITEM.findAll(xml).mapNotNull { parseItem(it.value) }.take(limit).toList()

    private fun parseItem(item: String): Article? {
        val title = htmlToText(tag(item, "title") ?: return null)
        val link = htmlToText(tag(item, "link") ?: atomLink(item) ?: return null)
        if (title.isBlank() || link.isBlank()) return null
        return Article(
            title = title,
            link = link,
            summary = htmlToText((tag(item, "description") ?: tag(item, "content") ?: tag(item, "summary")).orEmpty()),
            author = (tag(item, "dc:creator") ?: tag(item, "name") ?: tag(item, "author"))?.let(::htmlToText)?.ifBlank { null },
            publishedAt = parseDate(tag(item, "pubDate") ?: tag(item, "dc:date") ?: tag(item, "published") ?: tag(item, "updated")),
        )
    }

    /** Atom의 `<link href='...'/>` 중 `rel`이 없거나 `alternate`인 것의 주소. */
    private fun atomLink(item: String): String? =
        ATOM_LINK.findAll(item).map { it.value }
            .filter { !it.contains("rel=", ignoreCase = true) || it.contains(Regex("rel\\s*=\\s*['\"]alternate['\"]")) }
            .firstNotNullOfOrNull { HREF.find(it)?.groupValues?.get(1) }

    /** `<name>...</name>`의 안쪽을 돌려준다. CDATA 표식은 벗기고 안쪽 HTML은 그대로 둔다. */
    private fun tag(item: String, name: String): String? {
        val match = Regex("<${Regex.escape(name)}(?:\\s[^>]*)?>(.*?)</${Regex.escape(name)}>", RegexOption.DOT_MATCHES_ALL)
            .find(item) ?: return null
        val inner = match.groupValues[1]
        return CDATA.replace(inner) { it.groupValues[1] }
    }

    private fun parseDate(raw: String?): Long {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return 0L
        return runCatching { ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() }
            .recoverCatching { OffsetDateTime.parse(text).toEpochSecond() }
            .getOrDefault(0L)
    }
}
