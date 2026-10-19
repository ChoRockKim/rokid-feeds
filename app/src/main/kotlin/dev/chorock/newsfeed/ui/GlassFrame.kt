package dev.chorock.newsfeed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 480×352 고정 캔버스를 검정 배경 가운데에 둔다. 화면이 더 작으면(예: 폰 에뮬레이터) 비율을 유지한 채 줄여서 맞춘다.
 */
@Composable
fun GlassFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Glass.Background),
        contentAlignment = Alignment.Center,
    ) {
        val scale = minOf(maxWidth / Glass.WIDTH_DP.dp, maxHeight / Glass.HEIGHT_DP.dp).coerceAtMost(1.5f)
        Box(
            modifier = Modifier
                .requiredSize(Glass.WIDTH_DP.dp, Glass.HEIGHT_DP.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Glass.Primary40, RoundedCornerShape(12.dp))
                .background(Glass.Background)
                .padding(12.dp),
        ) {
            content()
        }
    }
}
