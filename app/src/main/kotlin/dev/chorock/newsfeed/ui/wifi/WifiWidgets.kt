package dev.chorock.newsfeed.ui.wifi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.ui.Glass

/** 선택된 줄은 2dp 실선, 아니면 1dp 옅은 선. 비활성은 항상 옅다. */
@Composable
fun WifiRow(
    selected: Boolean,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onFocused: () -> Unit = {},
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = if (selected && enabled) BorderStroke(2.dp, Glass.Primary) else BorderStroke(1.dp, Glass.Primary40)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(border, RoundedCornerShape(12.dp))
            .then(
                if (focusable) {
                    Modifier
                        .onFocusChanged { if (it.isFocused) onFocused() }
                        .clickable(enabled = enabled, onClick = onClick)
                } else Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        content = content,
    )
}

/**
 * 입력칸. 일반 안드로이드 텍스트 입력이라 폰의 키보드 앱이 보내는 글자(IME·접근성)를 그대로 받는다.
 * 비밀번호 칸이면 글자를 가리고(자동 완성·추천 없음), [masked]가 false일 때만 보여 준다.
 */
@Composable
fun WifiTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    selected: Boolean,
    focusRequester: FocusRequester,
    password: Boolean,
    masked: Boolean,
    onDone: () -> Unit,
) {
    // 입력칸은 BasicTextField가 포커스 정지점이다. 감싸는 줄까지 클릭 가능하면 링이 한 칸에서 두 번 멈춘다.
    WifiRow(selected = selected, focusable = false, onClick = {}) {
        BasicText(label, style = Glass.Caption)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            textStyle = Glass.Body,
            singleLine = true,
            cursorBrush = SolidColor(Glass.Primary),
            visualTransformation = if (password && masked) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
    }
}
