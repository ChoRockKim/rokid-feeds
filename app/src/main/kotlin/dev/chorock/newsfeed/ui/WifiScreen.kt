package dev.chorock.newsfeed.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.net.WifiConfig

sealed interface WifiState {
    data object Idle : WifiState
    data object Connecting : WifiState
    data class Failed(val message: String) : WifiState
}

/** 시작 시 인터넷이 없으면 보이는 화면. Enter(탭)로 미리 정해 둔 Wi-Fi에 자동 연결한다. 비밀번호는 화면에 표시하지 않는다. */
@Composable
fun WifiScreen(config: WifiConfig, state: WifiState, onConnect: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val connecting = state is WifiState.Connecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                handleNavigationKey(event, onEnter = { if (!connecting) onConnect() })
            },
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText("Wi-Fi 연결이 필요합니다", style = Glass.Heading)
        if (config.isConfigured) {
            BasicText("네트워크: ${config.ssid}", style = Glass.Body)
        } else {
            BasicText(
                "Wi-Fi 정보가 설정되지 않았습니다\nlocal.properties의 wifi.ssid를 채워 다시 빌드하세요",
                style = Glass.BodySmall.copy(textAlign = TextAlign.Center),
            )
        }
        BasicText(
            text = if (connecting) "연결 중..." else "Wi-Fi 연결",
            style = Glass.Label,
            modifier = Modifier
                .border(2.dp, if (connecting) Glass.Primary40 else Glass.Primary, RoundedCornerShape(12.dp))
                .clickable(enabled = !connecting && config.isConfigured, onClick = onConnect)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        )
        when (state) {
            is WifiState.Failed -> BasicText(state.message, style = Glass.BodySmall.copy(textAlign = TextAlign.Center), maxLines = 3)
            else -> BasicText("Enter: 연결", style = Glass.Caption)
        }
        // 출처: Rokid Nexus 문서(제3자). 글래스에서 직접 확인한 사실은 아니다.
        BasicText(
            "Hi Rokid 앱에서도 안경 Wi-Fi를 연결할 수 있습니다",
            style = Glass.Caption.copy(textAlign = TextAlign.Center),
        )
    }
}
