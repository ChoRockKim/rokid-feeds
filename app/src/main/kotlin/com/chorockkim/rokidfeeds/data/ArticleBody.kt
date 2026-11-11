package com.chorockkim.rokidfeeds.data

import com.chorockkim.rokidfeeds.util.htmlToText

/**
 * 기사 원문 페이지에서 본문만 뽑는다. RSS 요약은 언론사가 83~500자로 잘라 주기 때문이다.
 *
 * 언론사마다 HTML 구조가 달라 소스별 규칙을 둔다(연합뉴스·동아일보·경향신문, 2026-11-04 기준 구조).
 * 구조가 바뀌어 못 뽑으면 null을 돌려주고, 호출한 쪽은 RSS 요약으로 폴백한다.
 * 정규식으로 필요한 부분만 잘라 읽는다(RssParser와 같은 이유로 XML/HTML 파서를 쓰지 않는다).
 */
object ArticleBody {
    /** 글래스에서 읽을 분량 상한. 넘으면 문장 끝에서 자르고 `…`을 붙인다(링크 쪽이 뒤따른다). */
    const val MAX_CHARS = 2_400

    /** 이보다 짧게 뽑히면 규칙이 어긋난 것으로 보고 버린다. */
    private const val MIN_CHARS = 40

    private val DOT_ALL = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    private val PLAIN_P = Regex("<p>(.*?)</p>", DOT_ALL)
    private val KHAN_P = Regex("<p class=\"content_text[^\"]*\">(.*?)</p>", DOT_ALL)
    private val SCRIPT = Regex("<(script|style|figure)\\b.*?</\\1>", DOT_ALL)
    private val COMMENT = Regex("<!--.*?-->", DOT_ALL)
    private val EMAIL_ONLY = Regex("^[\\w.+-]+@[\\w.-]+$")

    /** [link]의 호스트에 맞는 규칙으로 [html]에서 본문을 뽑는다. 지원하지 않는 사이트이거나 못 뽑으면 null. */
    fun extract(link: String, html: String): String? {
        val host = hostOf(link) ?: return null
        val paragraphs = when {
            host.endsWith("yna.co.kr") -> yna(html)
            host.endsWith("donga.com") -> donga(html)
            host.endsWith("khan.co.kr") -> khan(html)
            else -> return null
        } ?: return null
        val text = paragraphs.joinToString("\n\n")
        return if (text.length < MIN_CHARS) null else cap(text)
    }

    /** 연합뉴스: `story-news` 영역의 속성 없는 `<p>`. 저작권 문구(`txt-copyright`)에서 끝난다. */
    private fun yna(html: String): List<String>? {
        val start = html.indexOf("<div class=\"story-news article\">").takeIf { it >= 0 } ?: return null
        val end = html.indexOf("txt-copyright", start).takeIf { it >= 0 } ?: html.length
        return PLAIN_P.findAll(html.substring(start, end))
            .map { htmlToText(it.groupValues[1]) }
            .filter { it.isNotBlank() && !EMAIL_ONLY.matches(it) }
            .toList()
    }

    /** 경향신문: `articleBody` 아래의 `content_text` 문단. */
    private fun khan(html: String): List<String>? {
        val start = html.indexOf("id=\"articleBody\"").takeIf { it >= 0 } ?: return null
        return KHAN_P.findAll(html.substring(start))
            .map { htmlToText(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()
    }

    /** 동아일보: `news_view` 영역에서 스크립트·광고·사진을 걷어 내고 줄 단위로 읽는다(문단이 `<br><br>`로 나뉜다). */
    private fun donga(html: String): List<String>? {
        val start = html.indexOf("<section class=\"news_view\"").takeIf { it >= 0 } ?: return null
        val end = html.indexOf("</section>", start).takeIf { it >= 0 } ?: html.length
        val region = COMMENT.replace(SCRIPT.replace(html.substring(start, end), ""), "")
        return htmlToText(region).lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("ⓒ") && !it.contains("저작권자") }
    }

    private fun cap(text: String): String {
        if (text.length <= MAX_CHARS) return text
        val window = text.substring(0, MAX_CHARS)
        val cut = window.indexOfLast { it == '.' || it == '다' || it == '\n' || it == '?' || it == '!' }
        val head = if (cut >= MAX_CHARS / 2) window.substring(0, cut + 1) else window
        return head.trimEnd() + "…"
    }

    private fun hostOf(link: String): String? =
        Regex("^https?://([^/?#:]+)", RegexOption.IGNORE_CASE).find(link)?.groupValues?.get(1)?.lowercase()
}

/** 기사 링크를 받아 본문을 뽑는다. 네트워크 오류나 구조 불일치는 null(호출한 쪽이 RSS 요약으로 폴백). */
class ArticleBodies(private val http: BoundedHttp = BoundedHttp()) {
    suspend fun load(link: String): String? =
        runCatching { ArticleBody.extract(link, http.fetch(link)) }.getOrNull()
}
