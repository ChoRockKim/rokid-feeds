package com.chorockkim.rokidfeeds.util

/**
 * 이모지를 뺀다. 이모지는 컬러 글꼴로 그려져 녹색 단색 화면에 맞지 않는다(다른 Rokid 앱과 같은 규칙).
 * 그림 문자(U+1F000 이상, U+2600~27BF), 국기 문자, 변형 선택자(FE0F), 결합 문자(200D), 피부색(1F3FB~1F3FF)을 지우고
 * 그 때문에 생긴 겹친 공백을 하나로 줄인다. 한글·한자·문장부호는 그대로 둔다.
 */
fun stripEmoji(text: String): String {
    val out = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        if (!isEmoji(cp)) out.appendCodePoint(cp)
        i += Character.charCount(cp)
    }
    return out.toString()
        .replace(Regex("[ \\t]{2,}"), " ")
        .lines().joinToString("\n") { it.trimEnd() }
        .trim()
}

private fun isEmoji(cp: Int): Boolean =
    cp >= 0x1F000 && cp <= 0x1FAFF ||
        cp in 0x2600..0x27BF ||
        cp in 0x2B00..0x2BFF ||
        cp == 0xFE0F || cp == 0xFE0E || cp == 0x200D || cp == 0x20E3 ||
        cp in 0xE0020..0xE007F
