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
import com.chorockkim.rokidfeeds.nav.DetailPos
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.NavState
import com.chorockkim.rokidfeeds.nav.Navigator
import com.chorockkim.rokidfeeds.nav.ThreadDetailNav
import com.chorockkim.rokidfeeds.threads.ThreadItem
import com.chorockkim.rokidfeeds.threads.ThreadPage
import com.chorockkim.rokidfeeds.threads.ThreadsSession
import com.chorockkim.rokidfeeds.threads.buildThreadPages
import kotlinx.coroutines.flow.Flow

/** 열어 둔 글 하나: 글과 그 안에서의 위치. 댓글을 열면 위에 쌓이고 뒤로 하면 빠진다. */
private data class Opened(val item: ThreadItem, val pos: DetailPos = DetailPos())

/**
 * Threads 화면.
 * - 목록: [Navigator] 규칙 그대로(이전/다음 순환, 탭 = 열기, 뒤로 = 메뉴). 끝 세 줄 안으로 오면 다음 글을 더 받는다.
 * - 글: [ThreadDetailNav] — 쪽을 넘기고, 댓글 쪽에서는 댓글을 하나씩 골라 탭하면 그 댓글을 열어 답글을 본다.
 *   뒤로 = 이전 글(댓글을 열기 전) → 목록.
 */
@Composable
fun ThreadsApp(session: ThreadsSession, keys: Flow<NavKey>, onBack: () -> Unit) {
    var list by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }
    val stack = remember { mutableStateListOf<Opened>() }
    LaunchedEffect(Unit) { session.start() }

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
        if (top == null && items.isNotEmpty() && shownList.selected >= items.size - 3) session.loadMore()
    }

    val pages = remember(top?.item, session.openItem, session.detail, session.detailLoading) {
        top?.let { buildThreadPages(it.item, session.detail.takeIf { session.openItem == top.item }, session.detailLoading) }.orEmpty()
    }
    val sizes = pages.map { (it as? ThreadPage.Replies)?.replies?.size ?: 0 }

    val onKey by rememberUpdatedState { key: NavKey ->
        val opened = stack.lastOrNull()
        when {
            opened != null -> {
                val r = ThreadDetailNav.reduce(opened.pos, key, sizes)
                when {
                    r.back -> stack.removeAt(stack.lastIndex)
                    r.openReply != null -> {
                        val page = pages.getOrNull(r.pos.page) as? ThreadPage.Replies
                        stack[stack.lastIndex] = opened.copy(pos = r.pos)
                        page?.replies?.getOrNull(r.openReply)?.let { stack += Opened(ThreadItem(it)) }
                    }
                    else -> stack[stack.lastIndex] = opened.copy(pos = r.pos)
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
        val pos = ThreadDetailNav.normalize(top.pos, sizes)
        ThreadsDetailScreen(
            pages = pages,
            pageIndex = pos.page,
            selectedReply = if (sizes[pos.page] > 0) pos.reply else null,
            depth = stack.size,
        )
    } else {
        ThreadsFeedScreen(items = items, selectedIndex = shownList.selected, loading = session.feedLoading, loggedIn = session.loggedIn)
    }
}
