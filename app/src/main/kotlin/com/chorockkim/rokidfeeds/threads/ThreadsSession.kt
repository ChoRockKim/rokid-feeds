package com.chorockkim.rokidfeeds.threads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Threads 화면 상태: 목록(받은 순서, 중복 제거), 열린 글의 상세(본문·이어 쓴 글·댓글), 불러오는 중 표시.
 * [ThreadsWeb]이 넘겨준 JSON을 백그라운드에서 [ThreadsParser]로 읽어 메인 스레드에서 상태를 바꾼다.
 */
class ThreadsSession(private val web: ThreadsWeb, private val scope: CoroutineScope) {
    var items by mutableStateOf<List<ThreadItem>>(emptyList())
        private set
    var feedLoading by mutableStateOf(false)
        private set
    var openItem by mutableStateOf<ThreadItem?>(null)
        private set
    var detail by mutableStateOf<ThreadDetail?>(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    var loggedIn by mutableStateOf(ThreadsWeb.isLoggedIn())
        private set

    /** 연 글의 상세(글 코드별). 댓글의 답글을 보고 돌아올 때 다시 받지 않는다. */
    private val details = HashMap<String, ThreadDetail>()
    private var started = false
    private var lastMoreAt = 0L
    private var feedTimeout: Job? = null
    private var detailTimeout: Job? = null

    /** Threads 화면에 처음 들어올 때 한 번 홈을 연다. */
    fun start() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        loggedIn = ThreadsWeb.isLoggedIn()
        items = emptyList()
        feedLoading = true
        web.openHome()
        feedTimeout?.cancel()
        feedTimeout = scope.launch { delay(LOAD_TIMEOUT_MS); feedLoading = false }
    }

    /** 목록 끝에 가까워졌을 때. 너무 자주 내리지 않는다. */
    fun loadMore() {
        val now = System.currentTimeMillis()
        if (now - lastMoreAt < MORE_INTERVAL_MS) return
        lastMoreAt = now
        web.loadMore()
    }

    fun open(item: ThreadItem) {
        openItem = item
        detail = details[item.head.code]
        detailTimeout?.cancel()
        if (detail != null) {
            detailLoading = false
            return
        }
        detailLoading = true
        web.openPost(item.head)
        detailTimeout?.cancel()
        detailTimeout = scope.launch { delay(LOAD_TIMEOUT_MS); detailLoading = false }
    }

    fun close() {
        openItem = null
        detail = null
        detailLoading = false
        web.post.loadUrl("about:blank")
    }

    /** 로그인 화면에서 돌아왔을 때: 로그인 상태가 바뀌었으면 목록을 새로 받는다. */
    fun afterLogin() {
        val now = ThreadsWeb.isLoggedIn()
        if (now != loggedIn || started) {
            loggedIn = now
            started = true
            refresh()
        }
    }

    fun logout() = ThreadsWeb.logout { scope.launch { loggedIn = false; if (started) refresh() } }

    /** [ThreadsWeb]이 넘긴 JSON(메인 스레드). */
    fun onJson(source: ThreadsWeb.Source, pageUrl: String, json: String) {
        when (source) {
            ThreadsWeb.Source.Feed -> scope.launch {
                val found = withContext(Dispatchers.Default) { ThreadsParser.items(json) }
                if (found.isEmpty()) return@launch
                val seen = items.mapTo(HashSet()) { it.head.pk }
                items = items + found.filter { seen.add(it.head.pk) }
                feedLoading = false
            }
            ThreadsWeb.Source.Post -> {
                val item = openItem ?: return
                val code = item.head.code
                if (!pageUrl.contains(code)) return // 이전 글 페이지에서 늦게 온 응답
                scope.launch {
                    val part = withContext(Dispatchers.Default) { ThreadsParser.detailPart(json, code) }
                    if (openItem?.head?.code != code) return@launch
                    detail = ThreadsParser.merge(detail, part, fallbackMain = item.head)?.also { details[code] = it }
                    detailLoading = false
                }
            }
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 15_000L
        const val MORE_INTERVAL_MS = 3_000L
    }
}
