package dev.chorock.newsfeed.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Rokid 글래스 단색(녹색) 디스플레이 규격. 두 번째 색은 쓰지 않고 투명도로만 위계를 만든다. */
object Glass {
    val Primary = Color(0xFF40FF5E)
    val Primary60 = Primary.copy(alpha = 0.6f)
    val Primary40 = Primary.copy(alpha = 0.4f)
    val Primary08 = Primary.copy(alpha = 0.08f)
    val Background = Color.Black

    const val WIDTH_DP = 480
    /** 원래 352. 안경 시야에서 위쪽이 잘 안 보여 박스 높이를 20% 줄였다(352 × 0.8 ≈ 282). */
    const val HEIGHT_DP = 282

    val Heading = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
    val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = Primary)
    val BodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = Primary60)
    val Label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Primary)
    val Caption = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, color = Primary60)
}
