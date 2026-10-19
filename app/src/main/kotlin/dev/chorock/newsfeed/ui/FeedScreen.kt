package dev.chorock.newsfeed.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
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
    onTap: (Int) -> Unit,
) {
    val articles = feed.articles
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val lastIndex = (articles.size - 1).coerceAtLeast(0)
    val now = System.currentTimeMillis() / 1000

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex.coerceIn(0, lastIndex)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event -> navKeyOf(event)?.let { onKey(it); true } ?: false },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText("뉴스  ${selectedIndex + 1}/${articles.size}", style = Glass.Heading)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(articles, key = { index, _ -> index }) { index, article ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Glass.Primary else Glass.Primary40,
                            shape = RoundedCornerShape(12.dp),
                        )
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
