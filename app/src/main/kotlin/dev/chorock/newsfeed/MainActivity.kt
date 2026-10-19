package dev.chorock.newsfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import dev.chorock.newsfeed.data.Feed
import dev.chorock.newsfeed.data.NewsApi
import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.nav.NavState
import dev.chorock.newsfeed.nav.Navigator
import dev.chorock.newsfeed.net.WifiConfig
import dev.chorock.newsfeed.net.WifiConnector
import dev.chorock.newsfeed.ui.DetailScreen
import dev.chorock.newsfeed.ui.ErrorView
import dev.chorock.newsfeed.ui.FeedScreen
import dev.chorock.newsfeed.ui.GlassFrame
import dev.chorock.newsfeed.ui.Load
import dev.chorock.newsfeed.ui.LoadingView
import dev.chorock.newsfeed.ui.NavStateSaver
import dev.chorock.newsfeed.ui.WifiScreen
import dev.chorock.newsfeed.ui.WifiState
import dev.chorock.newsfeed.ui.handleNavigationKey
import dev.chorock.newsfeed.util.buildDetailPages
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GlassFrame { NewsApp(connector = remember { WifiConnector(this) }, onExit = { finish() }) } }
    }
}

@Composable
private fun NewsApp(connector: WifiConnector, onExit: () -> Unit, api: NewsApi = remember { NewsApi() }) {
    val wifiConfig = remember { WifiConfig.fromBuild() }
    val scope = rememberCoroutineScope()
    // 시작할 때 인터넷이 없으면 Wi-Fi 연결 화면부터 보여 준다.
    var needsWifi by remember { mutableStateOf(!connector.isOnline()) }
    var wifiState by remember { mutableStateOf<WifiState>(WifiState.Idle) }
    var feed by remember { mutableStateOf<Load<Feed>>(Load.Loading) }
    var reload by remember { mutableIntStateOf(0) }
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }

    LaunchedEffect(reload, needsWifi) {
        if (needsWifi) return@LaunchedEffect
        feed = Load.Loading
        feed = runCatching { api.feed() }
            .fold({ Load.Success(it) }, { Load.Failure(it.message ?: it.javaClass.simpleName) })
        // 받기에 실패했는데 연결도 끊겨 있으면 오류 대신 Wi-Fi 연결 화면으로 보낸다.
        if (feed is Load.Failure && !connector.isOnline()) needsWifi = true
    }

    val loaded = (feed as? Load.Success)?.value
    val articles = loaded?.articles.orEmpty()
    // 기사마다 상세 쪽을 한 번만 나눈다. 내비게이션은 이 쪽 수를 보고 끝에서 멈춘다.
    val pages = remember(articles) { articles.map { buildDetailPages(it.summary, it.link) } }
    val navigator = remember(articles) { Navigator(articles.size) { pages[it].size } }
    val shown = navigator.normalize(nav)
    val openIndex = shown.openIndex
    val feedVisible = !needsWifi && articles.isNotEmpty()

    fun dispatch(key: NavKey) {
        val result = navigator.reduce(nav, key)
        nav = result.state
        if (result.exit) onExit()
    }

    // 기사 목록이 보일 때만 Back을 상태머신으로 넘긴다(상세 → 목록, 목록 → 종료). 그 밖의 화면은 시스템 기본(종료).
    BackHandler(enabled = feedVisible) { dispatch(NavKey.Back) }

    when {
        needsWifi -> WifiScreen(
            config = wifiConfig,
            state = wifiState,
            onConnect = {
                scope.launch {
                    wifiState = WifiState.Connecting
                    connector.connect(wifiConfig).fold(
                        onSuccess = {
                            wifiState = WifiState.Idle
                            needsWifi = false
                            reload++
                        },
                        onFailure = { wifiState = WifiState.Failed(it.message ?: "연결하지 못했습니다") },
                    )
                }
            },
        )
        feedVisible && openIndex != null -> DetailScreen(
            article = articles[openIndex],
            pages = pages[openIndex],
            pageIndex = shown.page,
            onKey = ::dispatch,
        )
        feed is Load.Loading -> LoadingView()
        feed is Load.Failure -> RetryOnEnter(onRetry = { reload++ }) { ErrorView((feed as Load.Failure).message) }
        loaded == null || loaded.articles.isEmpty() ->
            RetryOnEnter(onRetry = { reload++ }) { ErrorView("표시할 기사가 없습니다") }
        else -> FeedScreen(
            feed = loaded,
            selectedIndex = shown.selected,
            onKey = ::dispatch,
            onTap = { index ->
                nav = shown.copy(selected = index)
                dispatch(NavKey.Select)
            },
        )
    }
}

/** 오류 화면에서 Enter(탭)로 다시 시도하도록 포커스를 잡는다. */
@Composable
private fun RetryOnEnter(onRetry: () -> Unit, content: @Composable () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { handleNavigationKey(it, onEnter = onRetry) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
