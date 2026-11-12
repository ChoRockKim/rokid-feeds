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
 * Threads 화면 상태와 데이터 받기. TanStack Query처럼:
 * - **캐시 + stale-while-revalidate**: 목록·글 상세를 기기에 저장해 두고([ThreadsCache]) 켜자마자 저장본을 보여 준 뒤 뒤에서
 *   새로 받아 바꾼다. 글 상세는 [DETAIL_FRESH_MS] 안에 받은 것이면 다시 받지 않는다.
 * - **미리 받기**: 글 상세에 들어가면 댓글만 뒤에서 미리 더 받는다(ThreadsApp). 목록에서는 미리 받지 않는다(피드가 버벅임).
 * - **무한 스크롤**: 목록·댓글 끝 근처에서만 다음 묶음을 받는다([loadMore], [loadMoreReplies]).
 * 글 페이지는 이미 떠 있는 threads.com 안에서 옮겨 간다([ThreadsWeb.openPost], 전체 다시 열기보다 빠름). 데이터가
 * [SPA_FALLBACK_MS] 안에 안 오면 전체를 다시 연다.
 */
class ThreadsSession(
    private val web: ThreadsWeb,
    private val scope: CoroutineScope,
    private val cache: ThreadsCache? = null,
) {
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
    /** 열어 둔 글의 상세(댓글)를 못 받았을 때 이유. 자동으로 [MAX_DETAIL_RETRIES]번 다시 하고, 그 뒤에는 탭으로 다시 시도. */
    var detailError by mutableStateOf<String?>(null)
        private set
    private var detailRetries = 0
    private var retryJob: Job? = null

    /** 띠 위에 보여 줄 글 상세(댓글) 상태. 글을 열어 둔 동안만. */
    val detailStatus: String?
        get() = when {
            openItem == null -> null
            detailError != null && retryJob?.isActive == true -> "댓글 불러오기 실패 · 다시 시도 중 ($detailRetries/$MAX_DETAIL_RETRIES)"
            detailError != null -> "댓글 불러오기 실패 · 탭하면 다시 시도"
            detailLoading -> "댓글 불러오는 중…"
            loadingMoreReplies -> "댓글 더 불러오는 중…"
            else -> null
        }
    var loggedIn by mutableStateOf(ThreadsWeb.isLoggedIn())
        private set
    /** 더 불러오는 중(한 번에 하나만). */
    var loadingMore by mutableStateOf(false)
        private set
    /** 연달아 [MAX_EMPTY_MORE]번 새 글이 없으면 목록 끝으로 보고 더 시도하지 않는다(새로 고침하면 다시). */
    var feedEnd by mutableStateOf(false)
        private set
    /** 목록이 저장본(새로 받기 전)인지. */
    var feedStale by mutableStateOf(false)
        private set
    /** 목록에서 지금 고른 줄(ThreadsApp이 알려 준다). 저장본을 새 목록으로 바꿀지 정할 때 쓴다. */
    var selectedIndex = 0

    private var emptyMore = 0
    private var moreTimeout: Job? = null

    /** 댓글 더 받는 중(한 번에 하나). 연달아 [MAX_EMPTY_MORE]번 새 댓글이 없으면 그 글은 더 시도하지 않는다. */
    var loadingMoreReplies by mutableStateOf(false)
        private set
    private var emptyReplies = 0
    private var repliesEnd = false
    private var repliesTimeout: Job? = null
    private var lastRepliesAt = 0L

    private data class Cached(val detail: ThreadDetail, val fetchedAt: Long)

    /** 글 상세(글 코드별). 기기 저장본으로 채워지고, 받을 때마다 갱신된다. */
    private val details = HashMap<String, Cached>()

    /** 지금 글 WebView가 받고 있는 글. 그 글의 응답만 받는다. */
    private var target: ThreadItem? = null
    private var targetLoading = false

    private var started = false
    private var lastMoreAt = 0L
    private var feedTimeout: Job? = null
    private var detailTimeout: Job? = null
    private var fallback: Job? = null
    private var feedIdle: Job? = null
    private var postIdle: Job? = null
    private var feedSave: Job? = null

    /**
     * 받기를 시작하면 WebView를 켜고, 요청한 데이터가 오면 정해진 시간 뒤 끈다. 끄는 시점은 요청 기준으로 한 번만 정한다:
     * threads.com은 보이는 동안 스스로 다음 글을 계속 받는데, 응답마다 타이머를 늘렸더니 끝없이 받아 10번째를 보는데
     * 384개까지 받았다(사용자 확인).
     */
    private var feedRequested = false
    private fun feedBusy() { feedIdle?.cancel(); feedRequested = true; web.feedActive = true }
    private fun feedDone(after: Long = IDLE_AFTER_DATA_MS) {
        feedRequested = false
        feedIdle?.cancel()
        feedIdle = scope.launch { delay(after); web.feedActive = false }
    }
    private fun feedStopNow() { feedRequested = false; feedIdle?.cancel(); web.feedActive = false }
    private var postRequested = false
    private fun postBusy() { postIdle?.cancel(); postRequested = true; web.postActive = true }
    private fun postDone(after: Long = IDLE_AFTER_DATA_MS) {
        postRequested = false
        postIdle?.cancel()
        postIdle = scope.launch { delay(after); web.postActive = false }
    }

    /** Threads 화면에 처음 들어올 때: 저장본이 있으면 바로 보여 주고, 뒤에서 홈을 새로 받는다. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            val cached = cache?.let { c -> withContext(Dispatchers.IO) { c.prune(); c.loadFeed() } }
            if (cached != null && items.isEmpty()) {
                items = cached.value
                feedStale = true
            }
            refresh(keepItems = cached != null)
        }
    }

    fun refresh(keepItems: Boolean = false) {
        loggedIn = ThreadsWeb.isLoggedIn()
        if (!keepItems) { items = emptyList(); feedStale = false }
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
        if (loadingMore || feedEnd || feedLoading || feedStale) return
        if (items.size - selectedIndex > MORE_WHEN_LEFT + 1) return // 아직 앞에 충분히 있다
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

    /** 저장본(메모리 → 기기)에서 글 상세를 찾는다. */
    private suspend fun cached(code: String): Cached? = details[code] ?: cache?.let { c ->
        withContext(Dispatchers.IO) { c.loadDetail(code) }?.let { Cached(it.value, it.savedAt).also { e -> details[code] = e } }
    }

    private fun isFresh(c: Cached?) = c != null && System.currentTimeMillis() - c.fetchedAt < DETAIL_FRESH_MS

    /**
     * 글 WebView로 [item] 글 상세를 받는다. 이미 떠 있는 threads.com이면 페이지 안에서 옮겨 가고, 늦으면 다시 연다.
     * [reload]면 처음부터 다시 연다(연결이 끊겼다 돌아와 페이지가 망가졌을 수 있을 때).
     */
    private fun load(item: ThreadItem, reload: Boolean = false) {
        target = item
        targetLoading = true
        if (openItem == item && detail == null) detailLoading = true
        postBusy()
        val inPage = if (reload) { web.reloadPost(item.head); false } else web.openPost(item.head)
        fallback?.cancel()
        if (inPage) fallback = scope.launch {
            delay(SPA_FALLBACK_MS)
            if (target == item && targetLoading) web.reloadPost(item.head)
        }
        detailTimeout?.cancel()
        detailTimeout = scope.launch {
            delay(LOAD_TIMEOUT_MS)
            if (target == item) targetLoading = false
            postDone(0)
            if (openItem == item) {
                detailLoading = false
                // 아무것도 못 받았다: 실패로 보이고, 몇 번은 저절로 다시 받는다.
                detailError = "응답 없음"
                if (detailRetries < MAX_DETAIL_RETRIES) {
                    detailRetries++
                    retryJob = scope.launch { delay(RETRY_DELAY_MS); if (openItem == item) load(item, reload = true) }
                }
            }
        }
    }

    /** 댓글 화면에서 탭: 처음부터 다시 받는다. */
    fun retryDetail() {
        val item = openItem ?: return
        retryJob?.cancel()
        detailError = null
        detailRetries = 0
        detailLoading = detail == null
        load(item, reload = true)
    }

    fun open(item: ThreadItem) {
        openItem = item
        detailError = null
        detailRetries = 0
        retryJob?.cancel()
        loadingMoreReplies = false
        emptyReplies = 0
        repliesEnd = false
        repliesTimeout?.cancel()
        val code = item.head.code
        val memory = details[code]
        detail = memory?.detail
        detailLoading = memory == null
        scope.launch {
            val c = cached(code)
            if (openItem != item) return@launch
            if (c != null) { detail = c.detail; detailLoading = false }
            // 신선하면 그대로, 아니면 저장본을 보여 주면서 뒤에서 새로 받는다(이미 이 글을 받는 중이면 그대로 둔다).
            if (isFresh(c)) return@launch
            if (target?.head?.code == code && targetLoading) return@launch
            load(item)
        }
    }

    /** 댓글 목록 끝 근처에서: 글 페이지를 내려 다음 댓글을 받는다. */
    fun loadMoreReplies() {
        val item = openItem ?: return
        if (loadingMoreReplies || repliesEnd || detailLoading) return
        val now = System.currentTimeMillis()
        if (now - lastRepliesAt < MORE_INTERVAL_MS) return
        lastRepliesAt = now
        loadingMoreReplies = true
        // 저장본으로 열었으면 글 페이지가 그 글이 아닐 수 있다: 먼저 그 글로 옮긴다.
        if (target?.head?.code != item.head.code || web.post.url?.contains(item.head.code) != true) load(item)
        postBusy()
        scope.launch { delay(MORE_SCROLL_DELAY_MS * 3); web.loadMoreReplies() }
        repliesTimeout?.cancel()
        repliesTimeout = scope.launch {
            delay(MORE_TIMEOUT_MS)
            loadingMoreReplies = false
            emptyReplies++
            if (emptyReplies >= MAX_EMPTY_MORE) repliesEnd = true
            postDone(0)
        }
    }

    /** 글을 닫는다. 글 페이지는 비우지 않는다(다음 글을 페이지 안에서 빨리 열려고). */
    fun close() {
        openItem = null
        detail = null
        detailLoading = false
        postDone(0)
    }

    /**
     * 인터넷이 다시 연결됐을 때. 끊긴 동안 요청이 실패해 "목록 끝"으로 잘못 보거나 페이지가 오류 상태로 남아 멈춰 있었다
     * (사용자 확인). 실패 횟수·끝 판정을 지우고 홈을 다시 받는다(이미 내려가 있으면 새 글은 뒤에 붙인다). 글을 열어 두었으면
     * 그 글도 처음부터 다시 받는다.
     */
    fun onReconnected() {
        if (!started) return
        emptyReplies = 0
        repliesEnd = false
        loadingMoreReplies = false
        repliesTimeout?.cancel()
        feedStale = items.isNotEmpty()
        refresh(keepItems = true)
        openItem?.let { item ->
            fallback?.cancel()
            if (detail == null) detailLoading = true
            load(item, reload = true)
        }
    }

    /** 로그인 화면에서 돌아왔을 때: 로그인 상태가 바뀌었으면 목록을 새로 받는다. */
    fun afterLogin() {
        val now = ThreadsWeb.isLoggedIn()
        if (now != loggedIn || started) {
            loggedIn = now
            started = true
            details.clear()
            refresh()
        }
    }

    fun logout() = ThreadsWeb.logout {
        scope.launch {
            loggedIn = false
            details.clear()
            withContext(Dispatchers.IO) { cache?.clear() }
            if (started) refresh()
        }
    }

    /** [ThreadsWeb]이 넘긴 JSON(메인 스레드). */
    fun onJson(source: ThreadsWeb.Source, pageUrl: String, json: String) {
        when (source) {
            ThreadsWeb.Source.Feed -> scope.launch {
                val found = withContext(Dispatchers.Default) { ThreadsParser.items(json) }
                if (found.isEmpty()) return@launch
                if (feedStale) {
                    // 저장본을 보던 중 새 목록이 왔다: 이미 본 줄(지금 고른 줄까지)만 남기고 그 뒤는 새 목록으로 바꾼다.
                    // (예전에는 내려가 있으면 새 글을 버려서 저장본만 보였다.)
                    feedStale = false
                    items = items.take(selectedIndex + 1).takeIf { selectedIndex > 0 }.orEmpty()
                }
                val seen = items.mapTo(HashSet()) { it.head.pk }
                val fresh = found.filter { seen.add(it.head.pk) }
                // 보고 있는 곳보다 너무 앞서면 받지 않고 바로 끈다(페이지가 스스로 받아 오는 것).
                val room = (selectedIndex + MAX_AHEAD - items.size).coerceAtLeast(0)
                items = items + fresh.take(room.coerceAtLeast(if (items.isEmpty()) fresh.size else 0))
                saveFeedSoon()
                when {
                    items.size - selectedIndex >= MAX_AHEAD -> { feedLoading = false; loadingMore = false; moreTimeout?.cancel(); feedStopNow() }
                    fresh.isNotEmpty() && loadingMore -> {
                        moreTimeout?.cancel()
                        loadingMore = false
                        emptyMore = 0
                        feedDone(MORE_IDLE_MS)
                    }
                    feedLoading -> { feedLoading = false; feedDone(FIRST_IDLE_MS) }
                    !feedRequested -> Unit // 요청하지 않은 응답으로는 켜 둔 시간을 늘리지 않는다
                }
            }
            ThreadsWeb.Source.Post -> {
                val item = target ?: return
                val code = item.head.code
                if (!pageUrl.contains(code)) return // 이전 글에서 늦게 온 응답
                scope.launch {
                    val part = withContext(Dispatchers.Default) { ThreadsParser.detailPart(json, code) }
                    if (target?.head?.code != code) return@launch
                    val before = details[code]?.detail?.replies?.size ?: 0
                    val merged = ThreadsParser.merge(details[code]?.detail, part, fallbackMain = item.head) ?: return@launch
                    details[code] = Cached(merged, System.currentTimeMillis())
                    targetLoading = false
                    fallback?.cancel()
                    cache?.let { c -> launch(Dispatchers.IO) { c.saveDetail(code, merged) } }
                    if (openItem?.head?.code == code) {
                        detail = merged
                        detailLoading = false
                        detailError = null
                        retryJob?.cancel()
                    }
                    if (loadingMoreReplies && merged.replies.size > before) {
                        repliesTimeout?.cancel()
                        loadingMoreReplies = false
                        emptyReplies = 0
                        postDone(MORE_IDLE_MS)
                    } else if (postRequested) postDone() // 요청하지 않은 응답(페이지가 스스로 받은 것)으로는 늘리지 않는다
                }
            }
        }
    }

    private fun saveFeedSoon() {
        val c = cache ?: return
        feedSave?.cancel()
        feedSave = scope.launch {
            delay(2_000)
            val snapshot = items
            withContext(Dispatchers.IO) { c.saveFeed(snapshot) }
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
        /** 보고 있는 글보다 이만큼 넘게 앞서 받았으면 WebView를 끈다. */
        const val MAX_AHEAD = 15
        /** 남은 글이 이만큼 이하일 때만 더 불러온다. */
        const val MORE_WHEN_LEFT = 3
        /** 처음 불러오기: 첫 데이터가 온 뒤 이만큼만 더 켜 둔다(나머지 조각). */
        const val FIRST_IDLE_MS = 1_500L
        /** 이 안에 받은 글 상세는 다시 받지 않는다(TanStack의 staleTime). */
        const val DETAIL_FRESH_MS = 2 * 60 * 1000L
        /** 페이지 안 이동 뒤 이만큼 데이터가 없으면 그 글 페이지를 새로 연다. */
        const val SPA_FALLBACK_MS = 3_000L
        /** 글 상세를 못 받았을 때 저절로 다시 받는 횟수와 간격. */
        const val MAX_DETAIL_RETRIES = 2
        const val RETRY_DELAY_MS = 3_000L
    }
}
