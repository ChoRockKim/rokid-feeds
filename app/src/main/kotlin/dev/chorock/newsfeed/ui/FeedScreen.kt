package dev.chorock.newsfeed.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chorock.newsfeed.nav.nearestVisible
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.data.Feed
import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.util.timeAgo

@Composable
fun FeedScreen(
    feed: Feed,
    selectedIndex: Int,
    onKey: (NavKey) -> Unit,
    onFocused: (Int) -> Unit,
    onTap: (Int) -> Unit,
) {
    val articles = feed.articles
    val listState = rememberLazyListState()
    val itemFocus = rememberItemFocus(articles.size)
    val lastIndex = (articles.size - 1).coerceAtLeast(0)
    val now = System.currentTimeMillis() / 1000

    // 선택 위치로 실제 포커스를 맞춘다. 링이 포커스를 옮기면 onFocused가 선택을 갱신해 같은 값이 되므로 되돌리지 않는다.
    LaunchedEffect(selectedIndex, articles.size) {
        // 바로 위 행도 화면에 남겨 둔다. 선택 행을 맨 위에 붙이면 위 행이 합성되지 않아 ↑/링 뒤로가 포커스를 못 옮긴다.
        itemFocus.focus(selectedIndex) { listState.animateScrollToItem((it - 1).coerceAtLeast(0)) }
    }
    // 링이 목록을 끝까지 스크롤해 포커스된 행이 사라지면 가장 가까운 보이는 행으로 되돌린다.
    val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
    LaunchedEffect(visible, selectedIndex) {
        if (visible.isNotEmpty() && selectedIndex !in visible) {
            nearestVisible(selectedIndex, visible)?.let { itemFocus.focus(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event -> navKeyOf(event)?.let { onKey(it); true } ?: false },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText("뉴스  ${selectedIndex + 1}/${articles.size}", style = Glass.Heading)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(articles, key = { index, _ -> index }) { index, article ->
                var focused by remember { mutableStateOf(false) }
                val selected = focused
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Glass.Primary else Glass.Primary40,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .focusRequester(itemFocus.requesters[index])
                        .onFocusChanged { state ->
                            focused = state.isFocused
                            if (state.isFocused) onFocused(index)
                        }
                        .clickable { onTap(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    BasicText(article.title, style = Glass.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = buildList {
                        add(article.source)
                        if (article.publishedAt > 0) add(timeAgo(now, article.publishedAt))
                    }
                    BasicText(meta.joinToString(" · "), style = Glass.BodySmall)
                }
            }
        }
        val hint = "◀ ▶ 이동 · Enter 열기"
        val footer = if (feed.failedSources.isEmpty()) hint else "$hint · ${feed.failedSources.joinToString(", ")} 실패"
        BasicText(footer, style = Glass.Caption, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}
