package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chorockkim.rokidfeeds.threads.ImageState
import com.chorockkim.rokidfeeds.threads.ThreadImages
import com.chorockkim.rokidfeeds.threads.ThreadPost
import com.chorockkim.rokidfeeds.util.stripEmoji
import com.chorockkim.rokidfeeds.util.timeAgo

/*
 * Threads 앱 모양을 녹색 단색으로: 상자 대신 얇은 구분선, 왼쪽 둥근 아바타와 이어진 글을 잇는 세로선, 굵은 사용자 이름과
 * 오른쪽 시각, 본문, 사진 줄, 아래 아이콘 줄(좋아요·댓글·다시 올리기·보내기). 아이콘은 이모지 대신 선으로 그린다.
 */

/** Threads 화면에서 사진을 받는 함수(스냅샷에서는 가짜를 넣는다). */
typealias ImageSource = @Composable (String) -> ImageState

object ThreadsText {
    val Username = Glass.Body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
    val Time = Glass.BodySmall.copy(fontSize = 11.sp)
    val Body = Glass.Body.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val Count = Glass.BodySmall.copy(fontSize = 11.sp)
    val Logo = Glass.Heading.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontSize = 15.sp)
}

/** "5분", "2시간"처럼 Threads 앱이 쓰는 짧은 시각. */
fun shortAgo(now: Long, then: Long): String = timeAgo(now, then).removeSuffix(" 전")

/** 둥근 아바타: 머리글자. 프로필 사진은 받지 않는다(줄마다 받으면 느려서). */
@Composable
fun Avatar(post: ThreadPost, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).border(1.dp, Glass.Primary40, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(post.author.take(1).uppercase(), style = ThreadsText.Username.copy(fontSize = (size.value * 0.42f).sp))
    }
}

enum class ThreadIconKind { Heart, Comment, Repost, Share }

/** 선으로 그린 Threads 아이콘(단색). */
@Composable
fun ThreadIcon(kind: ThreadIconKind, size: Dp = 14.dp, color: Color = Glass.Dim) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            ThreadIconKind.Heart -> drawPath(Path().apply {
                moveTo(w * 0.5f, h * 0.9f)
                cubicTo(w * 0.05f, h * 0.6f, w * 0.0f, h * 0.2f, w * 0.28f, h * 0.15f)
                cubicTo(w * 0.42f, h * 0.12f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.3f)
                cubicTo(w * 0.5f, h * 0.25f, w * 0.58f, h * 0.12f, w * 0.72f, h * 0.15f)
                cubicTo(w * 1.0f, h * 0.2f, w * 0.95f, h * 0.6f, w * 0.5f, h * 0.9f)
                close()
            }, color, style = stroke)
            ThreadIconKind.Comment -> {
                drawCircle(color, radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.48f), style = stroke)
                drawLine(color, Offset(w * 0.2f, h * 0.75f), Offset(w * 0.08f, h * 0.95f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ThreadIconKind.Repost -> {
                drawArc(color, 200f, 230f, false, Offset(w * 0.12f, h * 0.15f), Size(w * 0.76f, h * 0.7f), style = stroke)
                drawLine(color, Offset(w * 0.78f, h * 0.12f), Offset(w * 0.86f, h * 0.32f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.86f, h * 0.32f), Offset(w * 0.64f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            ThreadIconKind.Share -> drawPath(Path().apply {
                moveTo(w * 0.08f, h * 0.45f)
                lineTo(w * 0.92f, h * 0.1f)
                lineTo(w * 0.62f, h * 0.92f)
                lineTo(w * 0.48f, h * 0.55f)
                close()
                moveTo(w * 0.48f, h * 0.55f)
                lineTo(w * 0.92f, h * 0.1f)
            }, color, style = stroke)
        }
    }
}

/** 아이콘 줄: 좋아요 수 · 댓글 수 · 다시 올리기 · 보내기. */
@Composable
fun ActionRow(post: ThreadPost, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Counted(ThreadIconKind.Heart, post.likeCount)
        Counted(ThreadIconKind.Comment, post.replyCount)
        ThreadIcon(ThreadIconKind.Repost)
        ThreadIcon(ThreadIconKind.Share)
    }
}

@Composable
private fun Counted(kind: ThreadIconKind, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        ThreadIcon(kind)
        if (count > 0) BasicText(compactCount(count), style = ThreadsText.Count)
    }
}

/** 1234 → "1,234", 12500 → "1.2만". */
fun compactCount(n: Int): String = when {
    n >= 10_000 -> "%.1f만".format(n / 10_000.0).replace(".0만", "만")
    else -> "%,d".format(n)
}

/** 사용자 이름 줄: 굵은 이름 + 오른쪽 시각. */
@Composable
fun NameLine(post: ThreadPost, now: Long, extra: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText(post.author, style = ThreadsText.Username, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            extra?.let { BasicText("  $it", style = ThreadsText.Time, maxLines = 1) }
        }
        if (post.takenAt > 0) BasicText(shortAgo(now, post.takenAt), style = ThreadsText.Time, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * 글 한 개: 왼쪽 아바타(+ 이어진 글이면 아래로 세로선), 오른쪽 이름 줄·본문·사진 줄·아이콘 줄.
 * [maxLines]로 본문 줄 수를 줄이고, [thumbs]면 사진을 작은 줄로 보여 준다(선택한 글만, 사진 받기를 줄이려고).
 */
@Composable
fun PostBlock(
    post: ThreadPost,
    now: Long,
    image: ImageSource,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    threadLine: Boolean = false,
    thumbs: Boolean = false,
    actions: Boolean = true,
    avatarSize: Dp = 28.dp,
    note: String? = null,
) {
    // 이어진 글 세로선은 줄 전체 높이를 다시 재지 않고(IntrinsicSize는 줄마다 두 번 측정해 무거웠다) 그릴 때 한 번에 긋는다.
    val lineColor = Glass.Primary40
    Row(
        modifier.fillMaxWidth().drawBehind {
            if (threadLine) {
                val x = avatarSize.toPx() / 2
                drawLine(lineColor, Offset(x, avatarSize.toPx() + 4.dp.toPx()), Offset(x, size.height), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        },
    ) {
        Avatar(post, avatarSize)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NameLine(post, now, extra = if (!actions && post.likeCount > 0) "좋아요 ${compactCount(post.likeCount)}" else null)
            val text = stripEmoji(post.text)
            if (text.isNotEmpty()) BasicText(text, style = ThreadsText.Body, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
            if (post.images.isNotEmpty()) {
                if (thumbs) ThumbRow(post.thumbs.ifEmpty { post.images }, image) else BasicText("사진 ${post.images.size}장", style = ThreadsText.Time)
            } else if (post.hasVideo) BasicText("동영상", style = ThreadsText.Time)
            note?.let { BasicText(it, style = ThreadsText.Time) }
            if (actions) ActionRow(post, Modifier.padding(top = 2.dp))
        }
    }
}

/** 사진 작은 줄(최대 3장, 둥근 모서리, 녹색 단색). */
@Composable
fun ThumbRow(urls: List<String>, image: ImageSource, height: Dp = 64.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.take(3).forEach { url ->
            Box(
                Modifier.size(width = height * 1.25f, height = height).clip(RoundedCornerShape(8.dp)).border(1.dp, Glass.Primary40, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = image(url).image
                if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, colorFilter = ThreadImages.GreenFilter, modifier = Modifier.fillMaxHeight().fillMaxWidth())
            }
        }
        if (urls.size > 3) BasicText("+${urls.size - 3}", style = ThreadsText.Time, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

/** 얇은 구분선(Threads 목록처럼 상자 대신). */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Glass.Primary40.copy(alpha = 0.3f)))
}
