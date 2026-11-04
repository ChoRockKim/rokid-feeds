package dev.chorock.newsfeed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.android.awaitFrame

/**
 * 항목마다 [FocusRequester]를 하나씩 들고 있다. **포커스가 선택의 단일 기준**이다.
 *
 * R08 링은 키를 앱에 보내지 않고 접근성 서비스가 화면의 클릭 가능한 노드에 `ACTION_FOCUS`/`ACTION_CLICK`을 보낸다.
 * 그래서 하이라이트·선택 위치를 앱 상태가 아니라 실제 포커스에서 파생시키고, 키보드·adb로 움직일 때도
 * 상태를 바꾸는 대신 같은 포커스를 옮긴다.
 */
class ItemFocus(val requesters: List<FocusRequester>) {
    /**
     * [index] 항목으로 포커스를 옮긴다. 목록이면 먼저 [bringIntoView]로 그 항목을 화면에 올린다
     * (화면 밖 항목은 합성되지 않아 포커스를 줄 수 없다).
     */
    suspend fun focus(index: Int, bringIntoView: suspend (Int) -> Unit = {}): Boolean {
        if (requesters.isEmpty()) return false
        val target = index.coerceIn(0, requesters.lastIndex)
        bringIntoView(target)
        repeat(3) {
            if (runCatching { requesters[target].requestFocus() }.getOrDefault(false)) return true
            awaitFrame()
        }
        return false
    }
}

@Composable
fun rememberItemFocus(count: Int): ItemFocus = remember(count) { ItemFocus(List(count) { FocusRequester() }) }

@Composable
fun rememberClearFocus(): () -> Unit {
    val manager = LocalFocusManager.current
    return { manager.clearFocus() }
}
