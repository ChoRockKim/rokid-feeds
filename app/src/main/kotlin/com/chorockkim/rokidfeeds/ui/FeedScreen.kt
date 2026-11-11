package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.data.Feed
import com.chorockkim.rokidfeeds.util.timeAgo

/**
 * 통합 기사 목록. 보여 주기만 한다: 선택 위치는 [com.chorockkim.rokidfeeds.nav.Navigator] 상태에서 오고,
 * 입력(터치패드·R08 링·키)은 Activity가 받아 그 상태머신으로 넘긴다. 행마다 포커스를 두지 않는다
 * (R08 링은 화면 위 노드 하나 [com.chorockkim.rokidfeeds.input.RingTarget]로 받는다).
 */
@Composable
fun FeedScreen(feed: Feed, selectedIndex: Int, now: Long = System.currentTimeMillis() / 1000) {
    val articles = feed.articles
    val listState = rememberLazyListState()

    // 바로 위 행도 보이게 한 칸 위에서 멈춘다(앞뒤 문맥이 보이도록).
    LaunchedEffect(selectedIndex, articles.size) {
        // 애니메이션 없이 바로: 빨리 넘길 때 애니메이션이 취소되면 선택한 줄이 화면 밖에 남았다.
        if (articles.isNotEmpty()) listState.scrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    BasicText(
                        article.title,
                        style = if (selected) Glass.Body else Glass.Body.copy(color = Glass.Dim),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildList {
                        add(article.source)
                        if (article.publishedAt > 0) add(timeAgo(now, article.publishedAt))
                    }
                    BasicText(meta.joinToString(" · "), style = Glass.BodySmall)
                }
            }
        }
        val hint = "◀ ▶ 이동 · 탭 열기"
        val footer = if (feed.failedSources.isEmpty()) hint else "$hint · ${feed.failedSources.joinToString(", ")} 실패"
        BasicText(footer, style = Glass.Caption, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}
