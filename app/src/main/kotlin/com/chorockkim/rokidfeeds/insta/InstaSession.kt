package com.chorockkim.rokidfeeds.insta

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chorockkim.rokidfeeds.threads.ThreadsWeb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 인스타 스토리 상태와 받기. Threads처럼 숨은 WebView([ThreadsWeb], [com.chorockkim.rokidfeeds.threads.Site.Instagram])가
 * 페이지를 열고 브리지가 넘긴 JSON에서 꺼낸다([InstaStories]).
 * - 목록: instagram.com 홈(위 동그라미 줄 = 스토리 목록)을 연다.
 * - 한 사람: 그 사람 스토리 페이지(`/stories/아이디/`)를 열면 그 사람(과 다음 몇 명) 장이 온다. 받으면 바로 페이지를 비운다:
 *   스토리 페이지는 스스로 다음 장·다음 사람으로 넘어가며 계속 받고 "봤음"을 남길 수 있어서(실기 미확인).
 * 사용자가 고른 사람만 받는다(미리 받지 않음). WebView는 받는 동안만 켠다(링 입력이 느려지지 않게).
 */
class InstaSession(private val web: ThreadsWeb, private val scope: CoroutineScope) {
    var reels by mutableStateOf<List<StoryReel>>(emptyList())
        private set
    var trayLoading by mutableStateOf(false)
        private set
    /** 장을 받는 중인 사람(userPk). */
    var loadingReel by mutableStateOf<String?>(null)
        private set
    /** 그 사람 장을 못 받았을 때. */
    var reelError by mutableStateOf<String?>(null)
        private set
    var loggedIn by mutableStateOf(web.isLoggedIn())
        private set

    private var started = false
    private var trayTimeout: Job? = null
    private var reelTimeout: Job? = null
    private var feedOff: Job? = null
    private var postOff: Job? = null

    fun start() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        loggedIn = web.isLoggedIn()
        if (!loggedIn) return
        trayLoading = true
        feedOff?.cancel()
        web.feedActive = true
        web.openHome()
        trayTimeout?.cancel()
        trayTimeout = scope.launch { delay(LOAD_TIMEOUT_MS); trayLoading = false; web.feedActive = false }
    }

    /** 이 사람 스토리를 본다: 장이 없으면 그 사람 스토리 페이지를 연다. */
    fun open(reel: StoryReel) {
        if (reel.items.isNotEmpty() || loadingReel == reel.userPk) return
        loadingReel = reel.userPk
        reelError = null
        postOff?.cancel()
        web.postActive = true
        web.openPage(reel.url)
        reelTimeout?.cancel()
        reelTimeout = scope.launch {
            delay(LOAD_TIMEOUT_MS)
            if (loadingReel == reel.userPk) { loadingReel = null; reelError = "응답 없음 · 탭하면 다시" }
            stopPost()
        }
    }

    fun retry(reel: StoryReel) {
        reelError = null
        loadingReel = null
        open(reel)
    }

    /** 로그인 화면에서 돌아왔을 때. */
    fun afterLogin() {
        started = true
        refresh()
    }

    fun onJson(source: ThreadsWeb.Source, json: String) {
        scope.launch {
            val found = withContext(Dispatchers.Default) { InstaStories.reels(json) }
            if (found.isEmpty()) return@launch
            val byPk = reels.associateBy { it.userPk }.toMutableMap()
            val order = reels.map { it.userPk }.toMutableList()
            found.forEach { r ->
                byPk[r.userPk] = byPk[r.userPk]?.let { InstaStories.merge(it, r) } ?: r.also { order += it.userPk }
            }
            reels = order.mapNotNull { byPk[it] }
            when (source) {
                ThreadsWeb.Source.Feed -> if (trayLoading) {
                    trayLoading = false
                    trayTimeout?.cancel()
                    feedOff?.cancel()
                    feedOff = scope.launch { delay(IDLE_AFTER_DATA_MS); web.feedActive = false }
                }
                ThreadsWeb.Source.Post -> {
                    val pk = loadingReel
                    if (pk != null && byPk[pk]?.items?.isNotEmpty() == true) {
                        loadingReel = null
                        reelTimeout?.cancel()
                        postOff?.cancel()
                        postOff = scope.launch { delay(IDLE_AFTER_DATA_MS); stopPost() }
                    }
                }
            }
        }
    }

    /** 스토리 페이지를 비워 스스로 넘기며 받지 않게 하고 WebView를 끈다. */
    private fun stopPost() {
        web.openPage("about:blank")
        web.postActive = false
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 20_000L
        const val IDLE_AFTER_DATA_MS = 1_000L
    }
}
