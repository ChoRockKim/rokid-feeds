package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.threads.TextSplitter

/**
 * 글 상세 본문을 **실제 줄 수로** 쪽에 나눈다. 글자 수로 나누면 기기 글꼴·줄바꿈에 따라 줄이 늘어 마지막 줄이 띠 아래로
 * 넘쳤다(사용자 확인). 그리는 것과 같은 글꼴·폭으로 줄바꿈을 재고, 한 쪽에 들어가는 줄 수만큼 줄 끝에서 끊는다.
 *
 * 띠 안쪽 높이 246dp(282 - 위아래 여백 18×2)에서 빼는 것: 아래 안내 줄 20, 작성자 줄 36, 아이콘 줄 22, 사진 줄 62.
 * 줄 높이는 본문 스타일의 lineHeight(20sp, 글꼴 배율은 1로 고정). 한 줄 여유를 둔다.
 */
@Composable
fun rememberPostSplitter(): TextSplitter {
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val density = LocalDensity.current
    return remember(measurer, density) { { text, withPhotos -> splitByLines(measurer, density, text, withPhotos) } }
}

private val BodyStyle get() = ThreadsText.Body.copy(fontSize = POST_BODY_SP, lineHeight = POST_LINE_SP)

fun splitByLines(measurer: TextMeasurer, density: Density, text: String, withPhotos: Boolean): List<String> {
    val width = with(density) { POST_TEXT_WIDTH.roundToPx() }
    val layout = measurer.measure(text, BodyStyle, constraints = Constraints(maxWidth = width), density = density)
    val lineHeight = with(density) { POST_LINE_SP.toDp() }.value
    fun linesFor(availableDp: Float) = ((availableDp / lineHeight).toInt() - 1).coerceAtLeast(2)
    val first = linesFor(INNER_HEIGHT - FOOTER - HEADER - ACTIONS - if (withPhotos) PHOTOS else 0f)
    val rest = linesFor(INNER_HEIGHT - FOOTER - ACTIONS)
    val pages = mutableListOf<String>()
    var line = 0
    var budget = first
    while (line < layout.lineCount) {
        val endLine = (line + budget).coerceAtMost(layout.lineCount) - 1
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(endLine)
        pages += text.substring(start, end).trim()
        line = endLine + 1
        budget = rest
    }
    return pages.filter { it.isNotEmpty() }
}

/** 글 상세 본문 글꼴(ThreadPostScreen과 같아야 한다). */
val POST_BODY_SP = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)
val POST_LINE_SP = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)

/** 띠 폭 480 - 좌우 여백 18×2. */
private val POST_TEXT_WIDTH = (Glass.WIDTH_DP - 36).dp
private const val INNER_HEIGHT = 246f
private const val FOOTER = 20f
private const val HEADER = 36f
private const val ACTIONS = 22f
private const val PHOTOS = 62f
