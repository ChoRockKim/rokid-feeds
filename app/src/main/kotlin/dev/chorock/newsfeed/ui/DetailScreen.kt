package dev.chorock.newsfeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import dev.chorock.newsfeed.nav.pageKeyForScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chorock.newsfeed.data.Article
import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.util.DetailPage
import dev.chorock.newsfeed.util.timeAgo

/** 상세 한 쪽. 스크롤 없이 쪽 단위로 넘긴다(쪽 나누기는 [dev.chorock.newsfeed.util.buildDetailPages]). */
@Composable
fun DetailScreen(
    article: Article,
    pages: List<DetailPage>,
    pageIndex: Int,
    onKey: (NavKey) -> Unit,
) {
    val currentOnKey by rememberUpdatedState(onKey)
    val focus = remember { FocusRequester() }
    val page = pages[pageIndex.coerceIn(0, pages.lastIndex)]

    LaunchedEffect(Unit) { focus.requestFocus() }

    // 쪽 전체가 포커스 가능한 노드 하나다. 링의 탭(ACTION_CLICK)은 다음 쪽, 스크롤 동작은 쪽 이동으로 받는다.
    val scrollState = remember {
        ScrollAxisRange(value = { 0f }, maxValue = { 1f })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .onPreviewKeyEvent { event -> navKeyOf(event)?.let { currentOnKey(it); true } ?: false }
            .semantics {
                verticalScrollAxisRange = scrollState
                scrollBy { x, y ->
                    pageKeyForScroll(x, y)?.let { currentOnKey(it) }
                    true
                }
            }
            .clickable { currentOnKey(NavKey.Select) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (page.showHeader) {
            BasicText(article.title, style = Glass.Heading, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                add(article.source)
                article.author?.let { add(it) }
                if (article.publishedAt > 0) add(timeAgo(System.currentTimeMillis() / 1000, article.publishedAt))
            }
            BasicText(meta.joinToString(" · "), style = Glass.BodySmall, maxLines = 1)
        }
        if (page.body.isNotBlank()) {
            BasicText(page.body, style = Glass.Body, overflow = TextOverflow.Clip)
        }
        page.link?.let { BasicText(it, style = Glass.Caption, maxLines = 3, overflow = TextOverflow.Ellipsis) }
        Column(Modifier.weight(1f)) {}
        BasicText(
            "${pageIndex + 1}/${pages.size} · ◀ ▶ 쪽 · Enter 다음 · Back 목록",
            style = Glass.Caption,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
