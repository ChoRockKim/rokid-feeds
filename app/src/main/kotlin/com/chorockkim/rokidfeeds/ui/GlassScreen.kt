package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** 띠 위 상태 줄에 띄울 것. 비어 있으면 그 칸은 그리지 않는다. */
data class StatusBar(
    /** 왼쪽: "새 버전 v0.2.0", 업데이트 진행. */
    val update: String? = null,
    /** 가운데: Wi-Fi·인터넷 상태, 지난번 튕긴 이유. */
    val notice: String? = null,
    /** 오른쪽: 시각 "14:32". */
    val clock: String = "",
    /** 오른쪽: 배터리 %. 모르면 null. */
    val battery: Int? = null,
    val charging: Boolean = false,
)

/**
 * 전체 화면은 검정(안경에서 투명)이고, 앱은 아래 가운데의 480×282 띠 안에만 그린다. 그 바로 위 빈 줄에 작은 상태 알약
 * (업데이트 · 네트워크 · 시각/배터리)을 둬서 앱 내용을 가리지 않는다(rokid-youtube·rokid-lens와 같은 배치).
 *
 * 실제 화면 크기에 맞추는 일은 [com.chorockkim.rokidfeeds.MainActivity.attachBaseContext]의 밀도 조정이 한다:
 * 화면 가로가 480dp가 되도록 맞추므로 여기서는 dp 그대로 그린다.
 */
@Composable
fun GlassScreen(status: StatusBar, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Glass.Background)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(Glass.WIDTH_DP.dp, Glass.HEIGHT_DP.dp)
                .border(1.dp, Glass.Primary40, RoundedCornerShape(12.dp))
                .padding(Glass.Margin),
        ) { content() }
        StatusRow(
            status,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = Glass.HEIGHT_DP.dp + 6.dp, start = 12.dp, end = 12.dp),
        )
    }
}

@Composable
private fun StatusRow(status: StatusBar, modifier: Modifier) {
    Box(modifier) {
        status.update?.let { Pill(it, Modifier.align(Alignment.CenterStart)) }
        status.notice?.let { Pill(it, Modifier.align(Alignment.Center)) }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status.clock.isNotEmpty()) BasicText(status.clock, style = Glass.Label)
            status.battery?.let { level ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (status.charging) ChargingBolt()
                    BatteryIcon(level, status.charging)
                    BasicText("$level%", style = if (status.charging || level > LOW_BATTERY) Glass.BodySmall else Glass.Label)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, modifier: Modifier) {
    BasicText(
        text,
        style = Glass.Label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .border(1.dp, Glass.Primary, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** 이 아래면 배터리 숫자를 굵게(경고). 색은 녹색 하나뿐이라 굵기로만 구분한다. */
const val LOW_BATTERY = 15

/**
 * 배터리 아이콘: 둥근 테두리 + 꼭지 + 잔량만큼 채운 막대. 충전 중이면 채움이 강조색, 아니면 낮을 때 테두리만 남는다.
 * 이모지 대신 선으로 그린다(단색 화면).
 */
@Composable
fun BatteryIcon(level: Int, charging: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 22.dp, height = 11.dp)) {
        val stroke = 1.2.dp.toPx()
        val tip = 2.dp.toPx()
        val bodyW = size.width - tip - stroke
        drawRoundRect(
            color = Glass.Dim,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyW, size.height - stroke),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            color = Glass.Dim,
            topLeft = Offset(bodyW + stroke / 2, size.height * 0.3f),
            size = Size(tip, size.height * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
        val inset = stroke + 1.dp.toPx()
        val fullW = bodyW - 2 * inset + stroke
        val fill = fullW * level.coerceIn(0, 100) / 100f
        if (fill > 0f) drawRoundRect(
            color = if (charging || level > LOW_BATTERY) Glass.Primary else Glass.Dim,
            topLeft = Offset(inset, inset),
            size = Size(fill.coerceAtLeast(1.dp.toPx()), size.height - 2 * inset),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

/** 충전 중 번개(채운 단색 도형). */
@Composable
fun ChargingBolt(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 7.dp, height = 11.dp)) {
        val w = size.width
        val h = size.height
        drawPath(Path().apply {
            moveTo(w * 0.62f, 0f)
            lineTo(0f, h * 0.58f)
            lineTo(w * 0.45f, h * 0.58f)
            lineTo(w * 0.32f, h)
            lineTo(w, h * 0.40f)
            lineTo(w * 0.55f, h * 0.40f)
            close()
        }, Glass.Primary)
    }
}
