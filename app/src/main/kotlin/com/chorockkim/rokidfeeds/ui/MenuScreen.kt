package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.nav.MenuItem

/** 메인 메뉴. 줄 이름과 설명만 보여 주고 입력은 Activity → [com.chorockkim.rokidfeeds.nav.AppNav]가 처리한다. */
@Composable
fun MenuScreen(selected: Int, threadsLoggedIn: Boolean) {
    // 네 줄이 띠에 들어가게 줄마다 이름과 설명을 한 줄에(두 줄로 두면 마지막 줄과 안내가 잘렸다).
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText("Rokid Feeds", style = Glass.Heading)
        MenuItem.entries.forEachIndexed { index, item ->
            val (title, sub) = when (item) {
                MenuItem.News -> "뉴스" to "연합뉴스 · 동아일보 · 경향신문 · GeekNews"
                MenuItem.Threads -> "Threads" to if (threadsLoggedIn) "내 피드 · 글 · 댓글 · 사진" else "로그인 안 함 · 일부 글만 · 설정에서 로그인"
                MenuItem.Instagram -> "Instagram" to "스토리 보기"
                MenuItem.Settings -> "설정" to "Threads 계정 · 앱 버전"
            }
            val on = index == selected
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .border(if (on) 2.dp else 1.dp, if (on) Glass.Primary else Glass.Primary40, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                BasicText(title, style = if (on) Glass.Body else Glass.Body.copy(color = Glass.Dim), modifier = Modifier.width(92.dp))
                BasicText(sub, style = Glass.BodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.weight(1f)) {}
        BasicText("◀ ▶ 이동 · 탭 선택 · 뒤로 종료", style = Glass.Caption, maxLines = 1)
    }
}

/**
 * 설정: Threads 계정 칸들. 로그인 안 한 칸 = 탭하면 폰으로 로그인, 다른 계정 = 탭하면 전환, 지금 계정 = 탭 두 번 로그아웃
 * (실수로 누르지 않게 한 번 더 탭해야 한다, [confirmLogout]).
 */
@Composable
fun SettingsScreen(selected: Int, accounts: List<com.chorockkim.rokidfeeds.nav.AccountRow>, confirmLogout: Boolean, version: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("설정 · Threads 계정", style = Glass.Heading)
        accounts.forEachIndexed { index, row ->
            val who = row.name?.let { " · @$it" } ?: if (row.loggedIn) " · 로그인됨" else ""
            val title = "계정 ${index + 1}$who" + if (row.active) "  (사용 중)" else ""
            val sub = when {
                !row.available -> "이 안경의 WebView는 계정 여러 개를 지원하지 않습니다"
                !row.loggedIn -> "탭: 폰에서 링크를 열어 로그인"
                !row.active -> "탭: 이 계정으로 전환"
                confirmLogout && index == selected -> "한 번 더 탭하면 로그아웃 · 쓸기·뒤로는 취소"
                else -> "탭 두 번: 로그아웃"
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
