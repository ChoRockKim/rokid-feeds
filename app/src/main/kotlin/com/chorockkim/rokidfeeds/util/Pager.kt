package com.chorockkim.rokidfeeds.util

/**
 * 상세 첫 쪽(제목 최대 3줄과 메타가 함께 보임)과 나머지 쪽에 들어가는 본문 글자 수 상한.
 * 480×282 프레임(원래 352에서 20% 줄임)에서 한글 본문은 한 줄에 약 32자이고, 제목 2줄인 첫 쪽에 본문 약 9줄, 헤더 없는 쪽에 약 11줄이 들어간다(글자 16/14sp 기준 한 줄 약 34자). 200/290자는 그 한계에서 줄바꿈 손실을 감안해 잡았다.
 * 문단 사이 빈 줄과 단어 단위 줄바꿈의 손실을 감안해 약 3할 여유를 두고 에뮬레이터에서 넘침을 확인해 정한 값이다.
 */
const val FIRST_PAGE_CHARS = 200
const val PAGE_CHARS = 290

/** 링크(캡션 글자, 최대 3줄)가 차지하는 자리를 본문 글자 수로 환산한 값. */
const val LINK_COST_CHARS = 70

const val NO_SUMMARY = "요약이 제공되지 않는 기사입니다"

/** 상세 한 쪽. [showHeader]면 제목·언론사·시간을 위에 함께 그린다. */
data class DetailPage(val body: String, val showHeader: Boolean, val link: String?)

private val SENTENCE_END = charArrayOf('.', '?', '!', '…', '。')

/**
 * [text]를 쪽으로 나눈다. 모든 쪽의 길이는 첫 쪽 [firstBudget], 나머지 [restBudget] 이하다.
 * 자를 곳은 문단(`\n\n`) → 문장 끝 → 공백 순으로 찾는다. 문단·문장 끝은 예산의 절반 이후에서만 쓴다(너무 짧은 쪽을 피함).
 * 공백이 전혀 없으면 예산 위치에서 강제로 자른다. 단어 중간은 공백이 있는 한 자르지 않는다.
 */
fun paginate(text: String, firstBudget: Int, restBudget: Int): List<String> {
    require(firstBudget > 0 && restBudget > 0) { "예산은 양수여야 한다" }
    val pages = mutableListOf<String>()
    var remaining = text.trim()
    var budget = firstBudget
    while (remaining.isNotEmpty()) {
        if (remaining.length <= budget) {
            pages += remaining
            break
        }
        val cut = findCut(remaining, budget)
        val chunk = remaining.substring(0, cut).trimEnd()
        pages += chunk
        remaining = remaining.substring(cut).trimStart()
        budget = restBudget
    }
    return pages
}

/** 길이가 [budget] 이하가 되는 자를 위치(1..budget)를 고른다. */
private fun findCut(text: String, budget: Int): Int {
    val window = text.substring(0, budget + 1) // 다음 글자가 공백인지 보려고 한 글자 더 본다
    val minimum = budget / 2

    window.lastIndexOf("\n\n").let { if (it in minimum..budget) return it }
    for (i in budget - 1 downTo minimum) {
        if (window[i] in SENTENCE_END && (i + 1 >= window.length || window[i + 1].isWhitespace())) return i + 1
    }
    for (i in budget downTo 1) {
        if (window[i].isWhitespace()) return i
    }
    return budget
}

/**
 * 기사 한 건의 상세 쪽 목록을 만든다. 요약이 비어 있으면 [NO_SUMMARY] 한 쪽.
 * 링크는 마지막 쪽에 자리가 있으면 같이, 없으면 링크만 있는 쪽을 덧붙인다.
 */
fun buildDetailPages(summary: String, link: String): List<DetailPage> {
    val bodies = paginate(summary.ifBlank { NO_SUMMARY }, FIRST_PAGE_CHARS, PAGE_CHARS)
    val pages = bodies.mapIndexed { index, body -> DetailPage(body, showHeader = index == 0, link = null) }.toMutableList()
    if (link.isBlank()) return pages
    val last = pages.last()
    val budget = if (pages.size == 1) FIRST_PAGE_CHARS else PAGE_CHARS
    if (last.body.length + LINK_COST_CHARS <= budget) {
        pages[pages.lastIndex] = last.copy(link = link)
    } else {
        pages += DetailPage(body = "", showHeader = false, link = link)
    }
    return pages
}
