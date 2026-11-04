package dev.chorock.newsfeed.ui.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.nav.PickerItem
import dev.chorock.newsfeed.net.signalBars
import dev.chorock.newsfeed.ui.Glass

/** 목록: 신호가 강한 순의 네트워크와 직접 입력·다시 검색·시스템 설정. */
@Composable
fun WifiPickerPage(
    items: List<PickerItem>,
    selected: Int,
    scan: ScanState,
    state: WifiState,
    notice: String?,
    onClick: (Int) -> Unit,
    onFocused: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    // 위 줄도 화면에 남겨야 ↑/링 뒤로 이동이 가능하다.
    LaunchedEffect(selected) { listState.animateScrollToItem((selected - 1).coerceIn(0, (items.size - 1).coerceAtLeast(0))) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("Wi-Fi 선택  ${selected + 1}/${items.size}", style = Glass.Heading)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(items) { index, item ->
                WifiRow(selected = index == selected, onFocused = { onFocused(index) }, onClick = { onClick(index) }) {
                    when (item) {
                        PickerItem.Permission -> {
                            BasicText("목록을 보려면 위치 권한 허용", style = Glass.Label)
                            BasicText("Enter: 권한 요청 (안드로이드 규칙, 위치는 쓰지 않음)", style = Glass.Caption)
                        }
                        is PickerItem.Network -> {
                            val network = item.network
                            BasicText(network.ssid, style = Glass.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BasicText(signalBars(network.level), style = Glass.Caption)
                                BasicText(network.security.label, style = Glass.Caption)
                            }
                        }
                        PickerItem.Manual -> BasicText("직접 입력 (숨김 네트워크)", style = Glass.Body)
                        PickerItem.Rescan -> BasicText(if (scan is ScanState.Scanning) "검색 중..." else "다시 검색", style = Glass.Body)
                        PickerItem.SystemSettings -> BasicText("시스템 Wi-Fi 설정 열기", style = Glass.BodySmall)
                    }
                }
            }
        }
        val status = notice
            ?: (state as? WifiState.Failed)?.message
            ?: (scan as? ScanState.Failed)?.message
            ?: if (state is WifiState.Connecting) "연결 중..." else if (scan is ScanState.Scanning) "검색 중..." else null
        BasicText(
            status ?: "◀ ▶ 이동 · Enter 선택 · Back 뒤로",
            style = if (status == null) Glass.Caption else Glass.BodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
