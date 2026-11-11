package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.chorockkim.rokidfeeds.data.ArticleBodies
import com.chorockkim.rokidfeeds.data.Feed
import com.chorockkim.rokidfeeds.data.NewsApi
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.NavState
import com.chorockkim.rokidfeeds.nav.Navigator
import com.chorockkim.rokidfeeds.util.buildDetailPages
import kotlinx.coroutines.flow.Flow

/**
 * 뉴스 화면 상태(피드 로딩, 본문 받기, 내비게이션). 입력은 [keys]로만 들어온다(Activity가 터치패드 키·R08 링·뒤로를 넣는다).
 *
 * 네트워크는 시스템에 맡긴다(다른 Rokid 앱과 같음): 앱이 Wi-Fi를 고르거나 시스템 화면을 열지 않는다.
 * 받기에 실패했는데 오프라인이면 "인터넷 연결 없음"을 띄우고, [online]이 다시 true가 되면 저절로 다시 불러온다.
 */
@Composable
fun NewsApp(keys: Flow<NavKey>, online: Boolean, onExit: () -> Unit, api: NewsApi) {
    var feed by remember { mutableStateOf<Load<Feed>>(Load.Loading) }
    var reload by remember { mutableIntStateOf(0) }
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }

    LaunchedEffect(reload) {
        feed = Load.Loading
        feed = runCatching { api.feed() }
            .fold({ Load.Success(it) }, { Load.Failure(it.message ?: it.javaClass.simpleName) })
    }
    // 연결이 돌아오면 실패한 피드를 다시 받는다.
    LaunchedEffect(online) {
        if (online && feed is Load.Failure) reload++
    }

    val loaded = (feed as? Load.Success)?.value
    val articles = loaded?.articles.orEmpty()
    // 기사마다 상세 쪽을 한 번만 나눈다. 내비게이션은 이 쪽 수를 보고 끝에서 멈춘다.
    // RSS 요약은 언론사가 잘라 주므로 상세를 열면 원문에서 본문을 받아 쓴다. 받기 전이나 실패하면 요약을 보여 준다.
    val bodies = remember { mutableStateMapOf<String, String>() }
    var loadingBody by remember { mutableStateOf<String?>(null) }
    val bodyLoader = remember { ArticleBodies() }
    val pages = remember(articles, bodies.size) { articles.map { buildDetailPages(bodies[it.link] ?: it.summary, it.link) } }
    val navigator = remember(articles, pages) { Navigator(articles.size) { pages[it].size } }
    val shown = navigator.normalize(nav)
    val openIndex = shown.openIndex

    val openLink = openIndex?.let { articles.getOrNull(it)?.link }
    LaunchedEffect(openLink) {
        val link = openLink ?: return@LaunchedEffect
        if (link in bodies) return@LaunchedEffect
        loadingBody = link
        try {
            val body = bodyLoader.load(link)
            val summaryLength = articles.firstOrNull { it.link == link }?.summary?.length ?: 0
            if (body != null && body.length > summaryLength) bodies[link] = body
        } finally {
            loadingBody = null
        }
    }

    // 목록·상세는 상태머신이, 그 밖의 화면(불러오는 중·오류)은 뒤로 = 종료, 탭 = 다시 시도.
    val onKey by rememberUpdatedState { key: NavKey ->
        when {
            articles.isNotEmpty() -> {
                val result = navigator.reduce(nav, key)
                nav = result.state
                if (result.exit) onExit()
            }
            key == NavKey.Back -> onExit()
            key == NavKey.Select && feed !is Load.Loading -> reload++
        }
    }
    LaunchedEffect(keys) { keys.collect { onKey(it) } }

    when {
        articles.isNotEmpty() && openIndex != null -> DetailScreen(
            article = articles[openIndex],
            pages = pages[openIndex],
            pageIndex = shown.page,
            loading = loadingBody != null && loadingBody == openLink,
        )
        articles.isNotEmpty() -> FeedScreen(feed = loaded!!, selectedIndex = shown.selected)
        feed is Load.Loading -> LoadingView()
        !online -> ErrorView("인터넷 연결 없음", "Wi-Fi가 연결되면 자동으로 다시 불러옵니다", hint = "뒤로: 종료")
        feed is Load.Failure -> ErrorView("불러오지 못했습니다", (feed as Load.Failure).message)
        else -> ErrorView("표시할 기사가 없습니다", "잠시 뒤 다시 시도해 주세요")
    }
}
