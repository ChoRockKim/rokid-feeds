package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.nav.MenuItem

/** 메인 메뉴. 줄 이름과 설명만 보여 주고 입력은 Activity → [com.chorockkim.rokidfeeds.nav.AppNav]가 처리한다. */
@Composable
fun MenuScreen(selected: Int, threadsLoggedIn: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("Rokid Feeds", style = Glass.Heading)
        MenuItem.entries.forEachIndexed { index, item ->
            val (title, sub) = when (item) {
                MenuItem.News -> "뉴스" to "연합뉴스 · 동아일보 · 경향신문 · GeekNews"
                MenuItem.Threads -> "Threads" to if (threadsLoggedIn) "내 피드 · 글 · 댓글 · 사진" else "로그인하지 않으면 일부 글만 보입니다 · 설정에서 로그인"
                MenuItem.Settings -> "설정" to "Threads 로그인·로그아웃 · 앱 버전"
            }
            val on = index == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(if (on) 2.dp else 1.dp, if (on) Glass.Primary else Glass.Primary40, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(title, style = if (on) Glass.Body else Glass.Body.copy(color = Glass.Dim))
                BasicText(sub, style = Glass.BodySmall, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f)) {}
        BasicText("◀ ▶ 이동 · 탭 선택 · 뒤로 종료", style = Glass.Caption, maxLines = 1)
    }
}

/**
 * 설정: Threads 계정(로그인 / 로그아웃). 로그아웃은 실수로 누르지 않게 한 번 더 탭해야 한다([confirmLogout]).
 */
@Composable
fun SettingsScreen(selected: Int, threadsLoggedIn: Boolean, confirmLogout: Boolean, version: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("설정", style = Glass.Heading)
        com.chorockkim.rokidfeeds.nav.SettingsItem.entries.forEachIndexed { index, item ->
            val (title, sub) = when (item) {
                com.chorockkim.rokidfeeds.nav.SettingsItem.ThreadsAccount -> when {
                    !threadsLoggedIn -> "Threads 로그인" to "폰에서 링크를 열어 로그인합니다"
                    confirmLogout -> "한 번 더 탭하면 로그아웃" to "쓸기·뒤로를 누르면 취소합니다"
                    else -> "Threads 로그아웃" to "로그인돼 있습니다 · 탭하면 확인을 묻습니다"
                }
            }
            val on = index == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(if (on) 2.dp else 1.dp, if (on) Glass.Primary else Glass.Primary40, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(title, style = if (on) Glass.Body else Glass.Body.copy(color = Glass.Dim))
                BasicText(sub, style = Glass.BodySmall, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f)) {}
        BasicText("Rokid Feeds v$version", style = Glass.Caption)
        BasicText("◀ ▶ 이동 · 탭 선택 · 뒤로 메뉴", style = Glass.Caption, maxLines = 1)
    }
}
