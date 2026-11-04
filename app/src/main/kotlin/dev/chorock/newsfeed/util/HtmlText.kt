package dev.chorock.newsfeed.util

private val TAG = Regex("<[^>]*>")
private val PARAGRAPH = Regex("<p>|<br\\s*/?>", RegexOption.IGNORE_CASE)
private val LIST_ITEM_END = Regex("</li>", RegexOption.IGNORE_CASE)
private val NUMERIC_ENTITY = Regex("&#(x[0-9a-fA-F]+|[0-9]+);")

/** RSS 제목·요약의 HTML 조각을 평문으로 바꾼다. `<p>`와 `<br>`은 줄바꿈, 나머지 태그는 제거, 엔티티는 복원. */
fun htmlToText(html: String): String {
    val withBreaks = LIST_ITEM_END.replace(PARAGRAPH.replace(html, "\n\n"), "\n")
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
