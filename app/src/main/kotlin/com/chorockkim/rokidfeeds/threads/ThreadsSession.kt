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
    /** 더 불러오는 중(한 번에 하나만). */
    var loadingMore by mutableStateOf(false)
        private set
    /** 연달아 [MAX_EMPTY_MORE]번 새 글이 없으면 목록 끝으로 보고 더 시도하지 않는다(새로 고침하면 다시). */
    var feedEnd by mutableStateOf(false)
        private set
    private var emptyMore = 0
    private var moreTimeout: Job? = null

    /** 연 글의 상세(글 코드별). 댓글의 답글을 보고 돌아올 때 다시 받지 않는다. */
    private val details = HashMap<String, ThreadDetail>()
    private var started = false
    private var lastMoreAt = 0L
    private var feedTimeout: Job? = null
    private var detailTimeout: Job? = null
    private var feedIdle: Job? = null
    private var postIdle: Job? = null

    /** 받기를 시작하면 WebView를 켜고, 데이터가 오면 [IDLE_AFTER_DATA_MS] 뒤(이어 오는 응답까지 받고) 끈다. */
    private fun feedBusy() { feedIdle?.cancel(); web.feedActive = true }
    private fun feedDone(after: Long = IDLE_AFTER_DATA_MS) {
        feedIdle?.cancel()
        feedIdle = scope.launch { delay(after); web.feedActive = false }
    }
    private fun postBusy() { postIdle?.cancel(); web.postActive = true }
    private fun postDone(after: Long = IDLE_AFTER_DATA_MS) {
        postIdle?.cancel()
        postIdle = scope.launch { delay(after); web.postActive = false }
    }

    /** Threads 화면에 처음 들어올 때 한 번 홈을 연다. */
    fun start() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        loggedIn = ThreadsWeb.isLoggedIn()
        items = emptyList()
        feedEnd = false
        emptyMore = 0
        loadingMore = false
        moreTimeout?.cancel()
        feedLoading = true
        feedBusy()
        web.openHome()
        feedTimeout?.cancel()
        feedTimeout = scope.launch { delay(LOAD_TIMEOUT_MS); feedLoading = false; feedDone(0) }
    }

    /**
     * 목록 끝에 가까워졌을 때. 한 번에 하나만, 새 글이 오면 바로 끝낸다. 새 글 없이 [MORE_TIMEOUT_MS]가 지나면 실패로 세고,
     * 연달아 [MAX_EMPTY_MORE]번이면 목록 끝으로 본다(계속 WebView를 켜며 느려지던 것을 막는다).
     */
    fun loadMore() {
        if (loadingMore || feedEnd || feedLoading) return
        val now = System.currentTimeMillis()
        if (now - lastMoreAt < MORE_INTERVAL_MS) return
        lastMoreAt = now
        loadingMore = true
        feedBusy()
        // 켜자마자 내리면 아직 그려지지 않아 페이지가 다음 글을 안 받을 수 있어 잠깐 뒤에 내린다.
        scope.launch { delay(MORE_SCROLL_DELAY_MS); web.loadMore() }
        moreTimeout?.cancel()
        moreTimeout = scope.launch {
            delay(MORE_TIMEOUT_MS)
            loadingMore = false
            emptyMore++
            if (emptyMore >= MAX_EMPTY_MORE) feedEnd = true
            feedDone(0)
        }
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
        postBusy()
        web.openPost(item.head)
        detailTimeout?.cancel()
        detailTimeout = scope.launch { delay(LOAD_TIMEOUT_MS); detailLoading = false; postDone(0) }
    }

    fun close() {
        openItem = null
        detail = null
        detailLoading = false
        web.post.loadUrl("about:blank")
        postDone(0)
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
                val fresh = found.filter { seen.add(it.head.pk) }
                items = items + fresh
                feedLoading = false
                if (fresh.isNotEmpty() && loadingMore) {
                    moreTimeout?.cancel()
                    loadingMore = false
                    emptyMore = 0
                    feedDone(MORE_IDLE_MS)
                } else feedDone()
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
                    postDone()
                }
            }
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 15_000L
        const val MORE_INTERVAL_MS = 3_000L
        /** 데이터가 온 뒤 이만큼 더 켜 두었다가 끈다(응답이 여러 개로 나뉘어 온다). */
        const val IDLE_AFTER_DATA_MS = 2_000L
        /** 더 불러오기: 새 글이 없으면 이만큼 뒤에 실패로 세고 끈다. */
        const val MORE_TIMEOUT_MS = 5_000L
        /** 더 불러오기에서 새 글이 오면 이만큼 뒤 끈다(나머지 조각까지). */
        const val MORE_IDLE_MS = 500L
        const val MORE_SCROLL_DELAY_MS = 300L
        const val MAX_EMPTY_MORE = 3
    }
}
