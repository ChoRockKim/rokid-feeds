package dev.chorock.newsfeed.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import dev.chorock.newsfeed.nav.NavKey

/**
 * 글래스 입력 가정: 좌/우(안경다리 전·후 스와이프), 확정(탭 = Enter), Back뿐이다. ↑↓ 키는 처리하지 않는다.
 * 키를 누르는 순간(KeyDown)에만 반응한다. Back은 시스템 Back으로 Activity가 [NavKey.Back]으로 넘긴다.
 * 입력 매핑은 이 파일 한 곳에 둔다. 실제 글래스의 키 코드가 확인되면 여기만 고친다.
 */
fun navKeyOf(event: KeyEvent): NavKey? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionLeft -> NavKey.Previous
        Key.DirectionRight -> NavKey.Next
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> NavKey.Select
        else -> null
    }
}

/** 키를 처리했으면 true. Enter만 필요한 화면(Wi-Fi·오류)에서 쓴다. */
fun handleNavigationKey(
    event: KeyEvent,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onEnter: () -> Unit = {},
): Boolean = when (navKeyOf(event)) {
    NavKey.Previous -> { onPrevious(); true }
    NavKey.Next -> { onNext(); true }
    NavKey.Select -> { onEnter(); true }
    else -> false
}
