package dev.chorock.newsfeed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Success<T>(val value: T) : Load<T>
    data class Failure(val message: String) : Load<Nothing>
}

@Composable
fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText("뉴스 불러오는 중...", style = Glass.Body)
    }
}

/** 오류도 두 번째 색 없이 같은 녹색 계열의 약한 테두리 대신 문구로 표현한다. */
@Composable
fun ErrorView(message: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText("불러오지 못했습니다", style = Glass.Heading)
        BasicText(message, style = Glass.BodySmall.copy(textAlign = TextAlign.Center), maxLines = 2)
        BasicText("Enter: 다시 시도", style = Glass.Label)
    }
}
