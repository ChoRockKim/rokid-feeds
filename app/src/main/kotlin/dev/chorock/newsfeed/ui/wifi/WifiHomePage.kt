package dev.chorock.newsfeed.ui.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.net.WifiConfig
import dev.chorock.newsfeed.ui.Glass

/** 홈: 지금 연결할 네트워크 이름과 두 버튼("Wi-Fi 연결" / "Wi-Fi 선택"). */
@Composable
fun WifiHomePage(
    active: WifiConfig?,
    state: WifiState,
    selected: Int,
    wifiOn: Boolean,
    autoNote: String?,
    onClick: (Int) -> Unit,
    onFocused: (Int) -> Unit,
) {
    val connecting = state is WifiState.Connecting
    val labels = listOf(if (connecting) "연결 중..." else if (!wifiOn) "Wi-Fi 켜기" else "Wi-Fi 연결", "Wi-Fi 선택")
    // Wi-Fi가 꺼져 있으면 저장된 네트워크가 없어도 "켜기"는 누를 수 있다.
    // "선택"은 연결 중에도 눌러 빠져나갈 수 있어야 한다(시스템 확인 창에서 막히면 갇힌다).
    val enabled = listOf(!connecting && (!wifiOn || active?.isConfigured == true), true)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText("Wi-Fi 연결이 필요합니다", style = Glass.Heading)
        if (!wifiOn) {
            BasicText(
                "Wi-Fi가 꺼져 있습니다\n'Wi-Fi 켜기'를 누르면 설정 패널이 열립니다",
                style = Glass.BodySmall.copy(textAlign = TextAlign.Center),
            )
        } else if (active?.isConfigured == true) {
            BasicText("네트워크: ${active.ssid}", style = Glass.Body)
        } else {
            BasicText(
                "저장된 Wi-Fi가 없습니다\n'Wi-Fi 선택'에서 고르세요",
                style = Glass.BodySmall.copy(textAlign = TextAlign.Center),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                BasicText(
                    text = label,
                    style = if (enabled[index]) Glass.Label else Glass.BodySmall,
                    modifier = Modifier
                        .border(
                            width = if (isSelected && enabled[index]) 2.dp else 1.dp,
                            color = if (isSelected && enabled[index]) Glass.Primary else Glass.Primary40,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .onFocusChanged { if (it.isFocused) onFocused(index) }
                        .clickable(enabled = enabled[index]) { onClick(index) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
        when (state) {
            is WifiState.Failed -> BasicText(state.message, style = Glass.BodySmall.copy(textAlign = TextAlign.Center), maxLines = 3)
            else -> BasicText(autoNote ?: "◀ ▶ 선택 · Enter 확인", style = Glass.Caption)
        }
        // 출처: Rokid Nexus 문서(제3자). 글래스에서 직접 확인한 사실은 아니다.
        BasicText(
            "Hi Rokid 앱에서도 안경 Wi-Fi를 연결할 수 있습니다",
            style = Glass.Caption.copy(textAlign = TextAlign.Center),
        )
    }
}
