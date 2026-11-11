package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.data.Article
import com.chorockkim.rokidfeeds.util.DetailPage
import com.chorockkim.rokidfeeds.util.timeAgo

/**
 * 상세 한 쪽. 스크롤 없이 쪽 단위로 넘긴다(쪽 나누기는 [com.chorockkim.rokidfeeds.util.buildDetailPages]).
 * 보여 주기만 하고 입력은 Activity가 받는다(쓸기 = 쪽 이동, 탭 = 다음 쪽, 뒤로 = 목록).
 */
@Composable
fun DetailScreen(
    article: Article,
    pages: List<DetailPage>,
    pageIndex: Int,
    loading: Boolean = false,
    now: Long = System.currentTimeMillis() / 1000,
) {
    val page = pages[pageIndex.coerceIn(0, pages.lastIndex)]
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (page.showHeader) {
            BasicText(article.title, style = Glass.Heading, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                add(article.source)
                article.author?.let { add(it) }
                if (article.publishedAt > 0) add(timeAgo(now, article.publishedAt))
            }
            BasicText(meta.joinToString(" · "), style = Glass.BodySmall, maxLines = 1)
        }
        if (page.body.isNotBlank()) {
            BasicText(page.body, style = Glass.Body, overflow = TextOverflow.Clip)
        }
        page.link?.let { BasicText(it, style = Glass.Caption, maxLines = 3, overflow = TextOverflow.Ellipsis) }
        Column(Modifier.weight(1f)) {}
        BasicText(
            if (loading) "${pageIndex + 1}/${pages.size} · 본문 불러오는 중…" else "${pageIndex + 1}/${pages.size} · ◀ ▶ 쪽 · 탭 다음 · 뒤로 목록",
            style = Glass.Caption,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
