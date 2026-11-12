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
 * [threads]는 Activity가 처음 Threads를 고를 때 만든다(WebView를 쓰므로). 만들어지기 전 한 프레임은 불러오는 중으로 그린다. [onScreen]은 Activity가 화면에 맞춰 WebView·로그인 흐름을 켜고 끄려고 받는다.
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
    login: com.chorockkim.rokidfeeds.threads.ThreadsLoginFlow.Ui?,
    onLoginRetry: () -> Unit,
) {
    var app by rememberSaveable(stateSaver = AppStateSaver) { mutableStateOf(AppState()) }
    LaunchedEffect(app.screen) { onScreen(app.screen) }
    val toMenu = { app = AppNav.backToMenu(app) }

    when (app.screen) {
        Screen.Menu -> {
            val onKey by rememberUpdatedState { key: NavKey ->
                val result = AppNav.reduceMenu(app, key)
                if (result.exit) onExit() else app = result.state
            }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            MenuScreen(selected = app.menuSelected, threadsLoggedIn = threadsLoggedIn)
        }
        Screen.Settings -> {
            val onKey by rememberUpdatedState { key: NavKey ->
                val result = AppNav.reduceSettings(app, key, threadsLoggedIn)
                app = result.state
                if (result.logout) onLogout()
            }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            SettingsScreen(app.settingsSelected, threadsLoggedIn, app.confirmLogout, com.chorockkim.rokidfeeds.BuildConfig.VERSION_NAME)
        }
        Screen.News -> NewsApp(keys = keys, online = online, onBack = toMenu, api = api)
        Screen.Threads -> if (threads != null) ThreadsApp(session = threads, keys = keys, onBack = toMenu, online = online) else {
            val onKey by rememberUpdatedState { key: NavKey -> if (key == NavKey.Back) toMenu() }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            LoadingView()
        }
        Screen.ThreadsLogin -> {
            // 뒤로 = 설정, 탭 = (실패했으면) 새 코드로 다시 시작.
            val onKey by rememberUpdatedState { key: NavKey ->
                when (key) {
                    NavKey.Back -> app = AppNav.backFromLogin(app)
                    NavKey.Select -> if (login is com.chorockkim.rokidfeeds.threads.ThreadsLoginFlow.Ui.Failed) onLoginRetry()
                    else -> Unit
                }
            }
            LaunchedEffect(keys) { keys.collect { onKey(it) } }
            ThreadsLoginScreen(login)
        }
    }
}
