package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.chorockkim.rokidfeeds.nav.DetailState
import com.chorockkim.rokidfeeds.nav.Mode
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.NavState
import com.chorockkim.rokidfeeds.nav.Navigator
import com.chorockkim.rokidfeeds.nav.ThreadDetailNav
import com.chorockkim.rokidfeeds.threads.ThreadItem
import com.chorockkim.rokidfeeds.threads.ThreadsSession
import com.chorockkim.rokidfeeds.threads.buildPostPages
import kotlinx.coroutines.flow.Flow

/** 글 본문이 뜨고 이만큼 뒤 댓글을 미리 받는다(본문 보는 동안 버벅이지 않게 조금 늦춘다). */
private const val REPLY_PREFETCH_DELAY_MS = 1_500L

/** 열어 둔 글 하나와 그 안의 위치. 댓글을 열면 위에 쌓이고 뒤로 하면 빠진다. */
private data class Opened(val item: ThreadItem, val state: DetailState = DetailState())

/**
 * Threads 화면.
 * - 목록: [Navigator] 규칙(이전/다음 순환, 탭 = 열기, 뒤로 = 메뉴). 끝 세 줄 안으로 오면 다음 글을 더 받는다.
 * - 글: [ThreadDetailNav] — 본문(아래 사진) → 탭하면 사진 크게(위아래로 다른 사진, 뒤로 글) → 아래로 넘기면 댓글 목록 →
 *   댓글 탭 = 그 댓글을 열기(답글·사진), 댓글에서 뒤로 = 글, 글에서 뒤로 = 이전 글 → 목록.
 */
@Composable
fun ThreadsApp(session: ThreadsSession, keys: Flow<NavKey>, onBack: () -> Unit, online: Boolean = true) {
    var list by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }
    val stack = remember { mutableStateListOf<Opened>() }
    LaunchedEffect(Unit) { session.start() }
    // 연결이 끊겼다 돌아오면 다시 받는 것은 Activity가 한다(어느 화면에 있든, MainActivity.watchNetwork).

    val items = session.items
    val listNav = remember(items) { Navigator(items.size) { 1 } }
    val shownList = listNav.normalize(list.copy(openIndex = null))
    val top = stack.lastOrNull()

    // 맨 위 글을 연다(캐시가 있으면 바로). 모두 닫히면 상세를 버린다.
    LaunchedEffect(top?.item) {
        val item = top?.item
        if (item != null) { if (session.openItem != item) session.open(item) } else if (session.openItem != null) session.close()
    }
    LaunchedEffect(shownList.selected, items.size) {
        session.selectedIndex = shownList.selected
        if (top == null && items.isNotEmpty() && shownList.selected >= items.size - 3) session.loadMore()
    }


    val detail = session.detail.takeIf { top != null && session.openItem == top.item }
    val split = rememberPostSplitter()
    val pages = remember(top?.item, detail, split) { top?.let { buildPostPages(it.item, detail, split) }.orEmpty() }
    val photosPerPage = pages.map { it.photos.size }
    val comments = detail?.replies.orEmpty()
    // 아직 댓글을 못 받았어도 댓글이 있는 글이면 댓글 화면으로 넘어갈 수 있게(거기서 불러오는 중·실패·다시 시도를 보여 준다).
    val expected = top?.item?.head?.replyCount ?: 0
    val navComments = if (comments.isEmpty() && expected > 0) 1 else comments.size

    // 미리 받기는 글 상세에서 댓글만(목록에서 미리 받으니 피드를 넘길 때도 버벅였다, 사용자 요청): 글을 열어 본문이 뜨면
    // 댓글이 전체보다 적게 왔을 때 뒤에서 다음 댓글을 한 번 받아 둔다.
    LaunchedEffect(top?.item, detail != null) {
        val opened = top ?: return@LaunchedEffect
        if (detail == null || opened.item.head.replyCount <= detail.replies.size) return@LaunchedEffect
        kotlinx.coroutines.delay(REPLY_PREFETCH_DELAY_MS)
        session.loadMoreReplies()
    }
    // 댓글 목록 끝 두 줄 안으로 오면, 전체 댓글 수보다 적게 받았을 때 글 페이지를 내려 더 받는다.
    val shownState = top?.let { ThreadDetailNav.normalize(it.state, photosPerPage, navComments) }
    LaunchedEffect(shownState?.mode, shownState?.comment, comments.size) {
        val s = shownState ?: return@LaunchedEffect
        if (s.mode == Mode.Comments && s.comment >= comments.size - 2 && (top?.item?.head?.replyCount ?: 0) > comments.size) {
            session.loadMoreReplies()
        }
    }

    val onKey by rememberUpdatedState { key: NavKey ->
        val opened = stack.lastOrNull()
        when {
            opened != null -> {
                val r = ThreadDetailNav.reduce(opened.state, key, photosPerPage, navComments) { p, i -> pages.getOrNull(p)?.tall?.getOrNull(i) == true }
                when {
                    r.back -> stack.removeAt(stack.lastIndex)
                    r.openComment != null -> {
                        stack[stack.lastIndex] = opened.copy(state = r.state)
                        val c = comments.getOrNull(r.openComment)
                        if (c != null) stack += Opened(ThreadItem(c))
                        else if (!session.detailLoading) session.retryDetail() // 댓글을 못 받은 채로 탭 = 다시 시도
                    }
                    else -> stack[stack.lastIndex] = opened.copy(state = r.state)
                }
            }
            items.isNotEmpty() -> {
                val r = listNav.reduce(shownList, key)
                val open = r.state.openIndex
                list = r.state.copy(openIndex = null, page = 0)
                if (open != null) items.getOrNull(open)?.let { stack += Opened(it) }
                if (r.exit) onBack()
            }
            key == NavKey.Back -> onBack()
            key == NavKey.Select && !session.feedLoading -> session.refresh()
        }
    }
    LaunchedEffect(keys) { keys.collect { onKey(it) } }

    if (top != null && pages.isNotEmpty()) {
        val s = ThreadDetailNav.normalize(top.state, photosPerPage, navComments)
        val page = pages[s.page]
        when (s.mode) {
            Mode.Post -> ThreadPostScreen(
                page = page,
                pageIndex = s.page,
                pageCount = pages.size,
                comments = if (comments.isNotEmpty()) comments.size else top.item.head.replyCount,
                depth = stack.size,
            )
            Mode.Photos -> ThreadPhotoScreen(page.post, page.photos[s.photo], s.photo, page.photos.size, tall = page.tall.getOrElse(s.photo) { false })
            Mode.Zoom -> ThreadPhotoZoomScreen(page.photos[s.photo], s.half)
            Mode.Comments -> ThreadCommentsScreen(
                comments = comments,
                selected = s.comment,
                loading = session.detailLoading || session.loadingMoreReplies,
                error = session.detailError,
                expected = top.item.head.replyCount,
                depth = stack.size,
            )
        }
    } else {
        ThreadsFeedScreen(
            items = items,
            selectedIndex = shownList.selected,
            loading = session.feedLoading || session.loadingMore,
            loggedIn = session.loggedIn,
            atEnd = session.feedEnd,
        )
    }
}
