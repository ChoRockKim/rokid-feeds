package com.chorockkim.rokidfeeds.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rokid 안경 화면 규격. rokid-glass-ios-navigation(`glasses-android`)·rokid-conversate와 같은 값이다:
 * 480×282 논리 단위의 띠를 화면 아래 가운데에, 여백 18, 강조 `#00ff66` / 보조 `#4da86c`.
 * 검은 픽셀은 빛이 나지 않으므로(투명) 검정 바탕에 얇은 녹색 윤곽선만 쓰고 밝은 면은 만들지 않는다.
 */
object Glass {
    val Primary = Color(0xFF00FF66)
    /** 보조 글자(메타·안내). */
    val Dim = Color(0xFF4DA86C)
    val Primary40 = Primary.copy(alpha = 0.4f)
    val Primary08 = Primary.copy(alpha = 0.08f)
    val Background = Color.Black

    const val WIDTH_DP = 480
    const val HEIGHT_DP = 282
    val Margin = 18.dp

    val Heading = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
    val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = Primary)
    val BodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = Dim)
    val Label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Primary)
    val Caption = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, color = Dim)
}
