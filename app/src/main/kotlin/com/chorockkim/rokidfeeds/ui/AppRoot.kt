package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.chorockkim.rokidfeeds.data.NewsApi
import com.chorockkim.rokidfeeds.nav.AppNav
import com.chorockkim.rokidfeeds.nav.AppState
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.Screen
import com.chorockkim.rokidfeeds.threads.ThreadsSession
import kotlinx.coroutines.flow.Flow

/**
 * 앱의 맨 위: 메인 메뉴 → 뉴스 / Threads / Threads 로그인. 한 번에 한 화면만 그려지고 그 화면만 [keys]를 받는다.
 * [threads]는 Activity가 처음 Threads를 고를 때 만든다(WebView를 쓰므로). 만들어지기 전 한 프레임은 불러오는 중으로 그린다. [onScreen]은 Activity가 로그인 화면에서 키를 웹으로 넘기려고 받는다.
 */
@Composable
fun AppRoot(
    keys: Flow<NavKey>,
    online: Boolean,
    api: NewsApi,
    threads: ThreadsSession?,
    threadsLoggedIn: Boolean,
    onScreen: (Screen) -> Unit,
    onLogout: () -> Unit,
    onExit: () -> Unit,
    loginFocus: String?,
    loginEditing: com.chorockkim.rokidfeeds.threads.LoginField? = null,
    loginText: String = "",
    onLoginText: (String) -> Unit = {},
    onLoginDone: () -> Unit = {},
) {
    var app by rememberSaveable(stateSaver = AppStateSaver) { mutableStateOf(AppState()) }
    LaunchedEffect(app.screen) { onScreen(app.screen) }
    val toMenu = { app = AppNav.backToMenu(app) }

    when (app.screen) {
        Screen.Menu -> {
            val onKey by rememberUpdatedState { key: NavKey ->
                val result = AppNav.reduceMenu(app, key)
                when {
                    result.exit -> onExit()
                    // 로그인돼 있으면 "로그아웃" 줄: 화면을 바꾸지 않고 로그아웃만 한다.
                    result.state.screen == Screen.ThreadsLogin && threadsLoggedIn -> onLogout()
                    else -> app = result.state
                }
            }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            MenuScreen(selected = app.menuSelected, threadsLoggedIn = threadsLoggedIn)
        }
        Screen.News -> NewsApp(keys = keys, online = online, onBack = toMenu, api = api)
        Screen.Threads -> if (threads != null) ThreadsApp(session = threads, keys = keys, onBack = toMenu) else {
            val onKey by rememberUpdatedState { key: NavKey -> if (key == NavKey.Back) toMenu() }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            LoadingView()
        }
        Screen.ThreadsLogin -> {
            // 쓸기·탭은 Activity가 웹으로 바로 넘기고, 여기로는 뒤로만 온다.
            val onKey by rememberUpdatedState { key: NavKey -> if (key == NavKey.Back) toMenu() }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            ThreadsLoginBar(loginFocus, loginEditing, loginText, onLoginText, onLoginDone)
        }
    }
}
