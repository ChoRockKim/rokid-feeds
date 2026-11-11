package com.chorockkim.rokidfeeds.util

private val TAG = Regex("<[^>]*>")
private val PARAGRAPH = Regex("<p>|<br\\s*/?>", RegexOption.IGNORE_CASE)
private val LIST_ITEM_START = Regex("<li(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE)
private val LIST_ITEM_END = Regex("</li>", RegexOption.IGNORE_CASE)
private val NUMERIC_ENTITY = Regex("&#(x[0-9a-fA-F]+|[0-9]+);")

/** 목록 항목 앞 표시. 이모지·특수 기호 대신 단색 화면에서도 같은 모양인 가운뎃점. */
const val LIST_MARK = "· "

/**
 * RSS 제목·요약의 HTML 조각을 평문으로 바꾼다. `<p>`와 `<br>`은 줄바꿈, 목록 항목은 [LIST_MARK]로 시작하는 한 줄,
 * 나머지 태그는 제거, 엔티티는 복원.
 */
fun htmlToText(html: String): String {
    val withBreaks = LIST_ITEM_END.replace(LIST_ITEM_START.replace(PARAGRAPH.replace(html, "\n\n"), LIST_MARK), "\n")
    val noTags = TAG.replace(withBreaks, "")
    val decoded = NUMERIC_ENTITY.replace(noTags) { match ->
        val raw = match.groupValues[1]
        val code = if (raw.startsWith("x")) raw.drop(1).toIntOrNull(16) else raw.toIntOrNull()
        if (code == null) match.value else String(Character.toChars(code))
    }
    return decoded
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&") // 다른 엔티티를 먼저 풀고 마지막에 처리한다
        .trim()
}

private val TRAILING_ELLIPSIS = Regex("(?:\\s*(?:…|\\.\\.\\.))+$")

/** 피드가 잘라 보낸 요약 끝에 붙이는 표시. 원문 링크는 상세 마지막 쪽에 함께 보인다. */
const val CONTINUES_MARK = "… 원문에서 계속"

/**
 * 피드가 요약을 길이로 잘라 보낼 때(끝이 `…`/`...`) 단어 중간에서 끊긴 마지막 조각을 버린다.
 * GeekNews는 약 500자에서 목록 항목 중간을 자르고 `…`를 붙인다("현재 Appl…"). 여러 줄이면 잘린 마지막 줄을,
 * 한 줄이면 잘린 마지막 단어를 버리고 [CONTINUES_MARK]를 붙인다. 끝이 `…`가 아니면 그대로 둔다.
 */
fun dropTruncatedTail(text: String): String {
    val trimmed = text.trimEnd()
    val ellipsis = TRAILING_ELLIPSIS.find(trimmed) ?: return text
    val body = trimmed.substring(0, ellipsis.range.first).trimEnd()
    val lastBreak = body.lastIndexOf('\n')
    val kept = when {
        lastBreak > 0 -> body.substring(0, lastBreak).trimEnd()
        body.contains(' ') -> body.substringBeforeLast(' ').trimEnd()
        else -> body
    }
    if (kept.isEmpty()) return text
    return if (lastBreak > 0) "$kept\n$CONTINUES_MARK" else "$kept $CONTINUES_MARK"
}
