package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chorockkim.rokidfeeds.threads.ImageState
import com.chorockkim.rokidfeeds.threads.ThreadImages
import com.chorockkim.rokidfeeds.threads.ThreadItem
import com.chorockkim.rokidfeeds.threads.ThreadPage
import com.chorockkim.rokidfeeds.threads.ThreadPost
import com.chorockkim.rokidfeeds.threads.rememberThreadImage
import com.chorockkim.rokidfeeds.util.stripEmoji
import com.chorockkim.rokidfeeds.util.timeAgo

/** 선택한 줄에 이만큼 머물면 썸네일을 받는다. */
const val THUMB_DWELL_MS = 700L

/** Threads 위 막대: 가운데 "Threads", 오른쪽 위치·로그인 상태. */
@Composable
private fun TopBar(right: String) {
    Box(Modifier.fillMaxWidth()) {
        BasicText("Threads", style = ThreadsText.Logo, modifier = Modifier.align(Alignment.Center))
        BasicText(right, style = ThreadsText.Time, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

/**
 * Threads 목록. 앱처럼 상자 없이 구분선으로 나눈 글 줄이고, 선택한 글만 밝게(나머지는 흐리게) + 왼쪽 녹색 막대.
 * 선택한 글은 사진을 작은 줄로 보여 주고, 나머지는 "사진 n장"만(보이는 사진만 받으려고).
 */
@Composable
fun ThreadsFeedScreen(
    items: List<ThreadItem>,
    selectedIndex: Int,
    loading: Boolean,
    loggedIn: Boolean,
    now: Long = System.currentTimeMillis() / 1000,
    image: ImageSource = { rememberThreadImage(it) },
    /** false면 바로 썸네일을 그린다(스냅샷). */
    lazyThumbs: Boolean = true,
) {
    if (items.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (loggedIn) "" else "로그인 안 함")
            ErrorView(
                title = if (loading) "불러오는 중…" else "글을 불러오지 못했습니다",
                message = if (loggedIn) "잠시 뒤 다시 시도해 주세요" else "로그인하지 않으면 일부 글만 보입니다 · 메뉴에서 로그인",
                hint = if (loading) "뒤로: 메뉴" else "탭: 다시 시도 · 뒤로: 메뉴",
            )
        }
        return
    }
    val listState = rememberLazyListState()
    // 선택한 글을 곧바로 맨 위에 둔다. 애니메이션은 링으로 빨리 넘길 때 중간에 취소되어 선택과 화면이 어긋났다
    // (선택한 줄만 길어져 목표 위치도 바뀜).
    LaunchedEffect(selectedIndex, items.size) { listState.scrollToItem(selectedIndex) }
    // 사진 썸네일은 선택한 줄에 잠시 머물 때만 받는다(빨리 넘길 때는 받지 않음).
    var settled by remember(selectedIndex) { mutableStateOf(!lazyThumbs) }
    LaunchedEffect(selectedIndex) { if (lazyThumbs) { delay(THUMB_DWELL_MS); settled = true } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TopBar("${selectedIndex + 1}/${items.size}" + if (loggedIn) "" else " · 로그인 안 함")
        Divider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(items, key = { _, item -> item.head.pk }) { index, item ->
                val selected = index == selectedIndex
                Row(Modifier.fillMaxWidth().alpha(if (selected) 1f else 0.45f)) {
                    Box(Modifier.width(3.dp).height(if (selected) 56.dp else 0.dp).background(Glass.Primary, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(7.dp))
                    PostBlock(
                        post = item.head,
                        now = now,
                        image = image,
                        maxLines = if (selected) 4 else 2,
                        threadLine = item.continuation.isNotEmpty(),
                        thumbs = selected && settled,
                        note = if (item.continuation.isNotEmpty()) "이어진 글 ${item.continuation.size}개" else null,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                Divider()
            }
        }
        BasicText(if (loading) "◀ ▶ 이동 · 탭 열기 · 더 불러오는 중…" else "◀ ▶ 이동 · 탭 열기 · 뒤로 메뉴", style = Glass.Caption, maxLines = 1)
    }
}

/**
 * Threads 글 상세 한 쪽. 앱의 글 화면처럼: 작성자 머리(아바타·이름·시각) → 본문 → 아이콘 줄, 사진은 한 쪽에 크게(녹색 단색),
 * 댓글은 아바타 줄 목록(짧은 댓글은 한 쪽에 여러 개).
 */
@Composable
fun ThreadsDetailScreen(
    pages: List<ThreadPage>,
    pageIndex: Int,
    /** 댓글 쪽에서 고른 댓글(그 쪽 안 순서). 탭하면 그 댓글의 답글을 연다. */
    selectedReply: Int? = null,
    /** 1 = 목록에서 연 글, 2 이상 = 댓글을 열어 들어온 글. */
    depth: Int = 1,
    now: Long = System.currentTimeMillis() / 1000,
    image: ImageSource = { rememberThreadImage(it) },
) {
    val page = pages[pageIndex.coerceIn(0, pages.lastIndex)]
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (page) {
                is ThreadPage.Text -> {
                    page.label?.let { BasicText(it, style = Glass.Label) }
                    val post = page.post
                    if (post != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(post, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            NameLine(post, now)
                        }
                    }
                    BasicText(page.body, style = ThreadsText.Body.copy(fontSize = 14.sp, lineHeight = 20.sp), overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false))
                    if (post != null && page.label == null) ActionRow(post)
                }
                is ThreadPage.Image -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(page.post, 20.dp)
                        Spacer(Modifier.width(8.dp))
                        BasicText(page.post.author, style = ThreadsText.Username, maxLines = 1)
                        Spacer(Modifier.weight(1f))
                        if (page.total > 1) BasicText("${page.index + 1}/${page.total}", style = ThreadsText.Time)
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        val state = image(page.url)
                        val bitmap = state.image
                        when {
                            bitmap != null -> Image(bitmap, null, contentScale = ContentScale.Fit, colorFilter = ThreadImages.GreenFilter, modifier = Modifier.fillMaxSize())
                            state.failed -> BasicText("사진을 불러오지 못했습니다", style = Glass.BodySmall)
                            else -> BasicText("사진 불러오는 중…", style = Glass.BodySmall)
                        }
                    }
                }
                is ThreadPage.Replies -> {
                    BasicText(
                        (if (depth > 1) "답글" else "댓글") + "  ${page.first + 1}–${page.first + page.replies.size} / ${page.total}",
                        style = Glass.Label,
                    )
                    Divider()
                    page.replies.forEachIndexed { i, reply ->
                        val on = i == selectedReply
                        Row(Modifier.fillMaxWidth().alpha(if (selectedReply == null || on) 1f else 0.45f)) {
                            Box(Modifier.width(3.dp).height(if (on) 40.dp else 0.dp).background(Glass.Primary, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(7.dp))
                            PostBlock(
                                reply, now, image, maxLines = 2, avatarSize = 24.dp, actions = false,
                                note = if (reply.replyCount > 0) "답글 ${reply.replyCount}개" + if (on) " · 탭하면 보기" else "" else null,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        if (i < page.replies.lastIndex) Divider()
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            val hint = when {
                page is ThreadPage.Replies -> "◀ ▶ 댓글 고르기 · 탭 답글 보기 · 뒤로 " + if (depth > 1) "이전 글" else "목록"
                else -> "◀ ▶ 쪽 · 탭 다음 · 뒤로 " + if (depth > 1) "이전 글" else "목록"
            }
            BasicText(hint, style = Glass.Caption, maxLines = 1, modifier = Modifier.weight(1f))
            BasicText("${pageIndex + 1}/${pages.size}", style = Glass.Caption)
        }
    }
}

/** 로그인 화면 위에 겹치는 안내(웹 화면은 그 아래 WebView). */
@Composable
fun ThreadsLoginHint(focused: String?) {
    Box(Modifier.fillMaxSize()) {
        BasicText(
            "◀ ▶ 칸 이동 · 탭 누르기 · 글자는 폰 키보드 · 뒤로 메뉴" + (focused?.takeIf { it.isNotBlank() }?.let { "  [$it]" } ?: ""),
            style = Glass.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}
