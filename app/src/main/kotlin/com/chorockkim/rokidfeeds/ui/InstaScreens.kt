package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chorockkim.rokidfeeds.insta.StoryReel
import com.chorockkim.rokidfeeds.threads.ThreadImages
import com.chorockkim.rokidfeeds.threads.rememberThreadImage

/** 스토리 동그라미: 머리글자, 안 본 스토리면 굵은 밝은 테두리(인스타 앱처럼). */
@Composable
fun StoryAvatar(name: String, size: Dp, unseen: Boolean) {
    Box(
        Modifier.size(size).clip(CircleShape).border(if (unseen) 2.dp else 1.dp, if (unseen) Glass.Primary else Glass.Primary40, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(name.take(1).uppercase(), style = ThreadsText.Username.copy(fontSize = (size.value * 0.42f).sp))
    }
}

/**
 * 인스타 스토리 목록(안 본 사람 먼저는 인스타가 정해 준 순서 그대로). 한 줄에 한 사람: 동그라미·아이디·올린 시각.
 */
@Composable
fun InstaTrayScreen(
    reels: List<StoryReel>,
    selected: Int,
    loading: Boolean,
    loggedIn: Boolean,
    now: Long = System.currentTimeMillis() / 1000,
) {
    when {
        !loggedIn -> return ErrorView("Instagram 로그인 필요", "폰에서 링크를 열어 instagram.com에 로그인합니다", hint = "탭: 로그인 · 뒤로: 종료")
        reels.isEmpty() -> return ErrorView(
            if (loading) "스토리 불러오는 중…" else "볼 스토리가 없습니다",
            if (loading) "instagram.com을 여는 중" else "잠시 뒤 다시 시도해 주세요",
            hint = if (loading) "뒤로: 종료" else "탭: 다시 · 뒤로: 종료",
        )
    }
    val listState = rememberLazyListState()
    LaunchedEffect(selected, reels.size) { listState.scrollToItem(selected.coerceIn(0, reels.lastIndex)) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText("Instagram 스토리", style = ThreadsText.Logo, modifier = Modifier.weight(1f))
            BasicText("${selected + 1}/${reels.size}" + if (loading) " · 불러오는 중" else "", style = Glass.Caption)
        }
        Divider()
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(reels, key = { _, r -> r.userPk }) { index, reel ->
                val on = index == selected
                Row(
                    Modifier.fillMaxWidth().alpha(if (on) 1f else 0.45f).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(3.dp).height(if (on) 36.dp else 0.dp).background(Glass.Primary, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(7.dp))
                    StoryAvatar(reel.username, 36.dp, unseen = !reel.seen)
                    Spacer(Modifier.width(10.dp))
                    BasicText(reel.username, style = ThreadsText.Username, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    BasicText((if (reel.seen) "봄 · " else "") + shortAgo(now, reel.latest), style = ThreadsText.Time)
                }
            }
        }
        BasicText("◀ ▶ 이동 · 탭 보기 · 뒤로 종료", style = Glass.Caption, maxLines = 1)
    }
}

/**
 * 스토리 한 장: 위에 장 수만큼 진행 막대(지난 장 꽉, 지금 장 [progress]), 아이디·시각, 가운데 사진(녹색 단색) 또는 동영상.
 * [content]가 사진·동영상 칸을 그린다(동영상은 [VideoPlayer]).
 */
@Composable
fun InstaStoryScreen(
    reel: StoryReel,
    item: Int,
    progress: Float,
    paused: Boolean,
    loading: Boolean,
    error: String?,
    now: Long = System.currentTimeMillis() / 1000,
    content: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val count = reel.items.size
        if (count > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(count) { i ->
                    val fill = when { i < item -> 1f; i == item -> progress.coerceIn(0f, 1f); else -> 0f }
                    Box(Modifier.weight(1f).height(2.dp).background(Glass.Primary40, RoundedCornerShape(1.dp))) {
                        if (fill > 0f) Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(Glass.Primary, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StoryAvatar(reel.username, 22.dp, unseen = false)
            Spacer(Modifier.width(8.dp))
            BasicText(reel.username, style = ThreadsText.Username, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            reel.items.getOrNull(item)?.let { BasicText(shortAgo(now, it.takenAt), style = ThreadsText.Time) }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                error != null && count == 0 -> BasicText(error, style = Glass.BodySmall)
                count == 0 -> BasicText(if (loading) "스토리 불러오는 중…" else "스토리가 없습니다", style = Glass.BodySmall)
                else -> content(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            BasicText(
                "◀ ▶ 이전/다음 · 탭 " + (if (paused) "계속" else "멈춤") + " · 뒤로 목록",
                style = Glass.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (count > 0) BasicText("${item + 1}/$count", style = Glass.Caption)
        }
    }
}

/** 스토리 사진(녹색 단색, 비율 유지). */
@Composable
fun StoryPhoto(url: String?, modifier: Modifier, paused: Boolean, image: ImageSource = { rememberThreadImage(it) }) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val state = url?.let { image(it) }
        val bitmap = state?.image
        when {
            bitmap != null -> Image(bitmap, null, contentScale = ContentScale.Fit, colorFilter = ThreadImages.GreenFilter, modifier = Modifier.fillMaxSize())
            state?.failed == true || url == null -> BasicText("사진을 불러오지 못했습니다", style = Glass.BodySmall)
            else -> BasicText("사진 불러오는 중…", style = Glass.BodySmall)
        }
        if (paused) PlayBadge(40.dp)
    }
}
