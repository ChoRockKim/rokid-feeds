package dev.chorock.newsfeed.ui.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.nav.FormItem
import dev.chorock.newsfeed.net.WifiSecurity
import dev.chorock.newsfeed.ui.Glass

/** 폼: 비밀번호(직접 입력이면 SSID·보안도) 입력과 연결/취소. 비밀번호 칸에는 폰 앱에서 글자를 보낼 수 있다. */
@Composable
fun WifiFormPage(
    title: String,
    items: List<FormItem>,
    selected: Int,
    manual: Boolean,
    ssid: String,
    onSsid: (String) -> Unit,
    security: WifiSecurity,
    password: String,
    onPassword: (String) -> Unit,
    reveal: Boolean,
    ssidFocus: FocusRequester,
    passwordFocus: FocusRequester,
    state: WifiState,
    error: String?,
    onClick: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNextField: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(title, style = Glass.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selected
                when (item) {
                    FormItem.Ssid -> WifiTextField(
                        label = "SSID", value = ssid, onValueChange = onSsid, selected = isSelected,
                        focusRequester = ssidFocus, password = false, masked = false, onDone = onNextField,
                    )
                    FormItem.Security -> WifiRow(isSelected, onClick = { onClick(index) }) {
                        BasicText("보안: ${security.label}  (Enter로 변경)", style = Glass.Body)
                    }
                    FormItem.Password -> WifiTextField(
                        label = "비밀번호", value = password, onValueChange = onPassword, selected = isSelected,
                        focusRequester = passwordFocus, password = true, masked = !reveal, onDone = onSubmit,
                    )
                    FormItem.Reveal -> WifiRow(isSelected, onClick = { onClick(index) }) {
                        BasicText("비밀번호 표시: ${if (reveal) "켬" else "끔"}", style = Glass.Body)
                    }
                    FormItem.Connect -> WifiRow(isSelected, enabled = state !is WifiState.Connecting, onClick = { onClick(index) }) {
                        BasicText(if (state is WifiState.Connecting) "연결 중..." else "연결", style = Glass.Label)
                    }
                    FormItem.Cancel -> WifiRow(isSelected, onClick = { onClick(index) }) {
                        BasicText("취소", style = Glass.Body)
                    }
                }
            }
        }
        val status = error ?: (state as? WifiState.Failed)?.message
        BasicText(
            status ?: "◀ ▶ 이동 · Enter 확인 · Back 뒤로",
            style = if (status == null) Glass.Caption else Glass.BodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
