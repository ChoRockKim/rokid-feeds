package dev.chorock.newsfeed.ui

import androidx.compose.foundation.focusable
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
import androidx.compose.ui.input.key.onKeyEvent
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
    val focus = remember { FocusRequester() }
    val page = pages[pageIndex.coerceIn(0, pages.lastIndex)]

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event -> navKeyOf(event)?.let { onKey(it); true } ?: false },
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
