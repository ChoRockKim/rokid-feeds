package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Success<T>(val value: T) : Load<T>
    data class Failure(val message: String) : Load<Nothing>
}

@Composable
fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText("뉴스 불러오는 중…", style = Glass.Body)
    }
}

/** 오류도 두 번째 색 없이 녹색 글자로만 보여 준다. */
@Composable
fun ErrorView(title: String, message: String, hint: String = "탭: 다시 시도") {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(title, style = Glass.Heading)
        BasicText(message, style = Glass.BodySmall.copy(textAlign = TextAlign.Center), maxLines = 2)
        BasicText(hint, style = Glass.Label)
    }
}
