package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.chorockkim.rokidfeeds.insta.InstaSession
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.StoryNav
import com.chorockkim.rokidfeeds.nav.StoryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/** 사진 스토리가 넘어가기까지(인스타 앱은 5초, 안경에서 읽기 쉽게 조금 길게). */
private const val PHOTO_MS = 6_000L
private const val TICK_MS = 100L

/**
 * 인스타 스토리 화면(사용자 요청 2026-11-15: 인스타는 스토리부터). 이동 규칙은 [StoryNav].
 * 로그인 안 했으면 탭 = 폰 로그인([onLogin]). 목록에서 뒤로 = 앱 종료([onBack]).
 */
@Composable
fun InstaApp(session: InstaSession, keys: Flow<NavKey>, onBack: () -> Unit, onLogin: () -> Unit) {
    var state by remember { mutableStateOf(StoryState()) }
    LaunchedEffect(Unit) { session.start() }
    val reels = session.reels
    val itemsOf = { i: Int -> reels.getOrNull(i)?.items?.takeIf { it.isNotEmpty() }?.size }
    val shown = state.copy(reel = state.reel.coerceIn(0, (reels.size - 1).coerceAtLeast(0)))
    val current = reels.getOrNull(shown.reel)
    val item = current?.items?.getOrNull(shown.item)

    // 보기에 들어간 사람의 장을 받는다(고른 사람만).
    LaunchedEffect(shown.viewing, current?.userPk) { if (shown.viewing && current != null) session.open(current) }

    var progress by remember(shown.viewing, shown.reel, shown.item) { mutableFloatStateOf(0f) }
    val latestState by rememberUpdatedState(state)
    val advance = { state = StoryNav.advance(latestState, session.reels.size) { i -> session.reels.getOrNull(i)?.items?.takeIf { it.isNotEmpty() }?.size } }
    // 사진은 시간이 지나면 다음 장(멈춤이면 그대로).
    LaunchedEffect(shown.viewing, shown.reel, shown.item, shown.paused, item?.pk) {
        if (!shown.viewing || item == null || item.video != null || shown.paused) return@LaunchedEffect
        while (progress < 1f) { delay(TICK_MS); progress += TICK_MS.toFloat() / PHOTO_MS }
        advance()
    }

    val onKey by rememberUpdatedState { key: NavKey ->
        when {
            !session.loggedIn -> when (key) {
                NavKey.Select -> onLogin()
                NavKey.Back -> onBack()
                else -> Unit
            }
            !shown.viewing && reels.isEmpty() -> when (key) {
                NavKey.Select -> if (!session.trayLoading) session.refresh()
                NavKey.Back -> onBack()
                else -> Unit
            }
            shown.viewing && key == NavKey.Select && current != null && current.items.isEmpty() && session.reelError != null -> session.retry(current)
            else -> {
                val r = StoryNav.reduce(shown, key, reels.size, itemsOf)
                if (r.exit) onBack() else state = r.state
            }
        }
    }
    LaunchedEffect(keys) { keys.collect { onKey(it) } }

    if (shown.viewing && current != null) {
        InstaStoryScreen(
            reel = current,
            item = shown.item,
            progress = progress,
            paused = shown.paused,
            loading = session.loadingReel == current.userPk,
            error = session.reelError,
        ) { modifier ->
            if (item?.video != null) {
                key(item.pk) {
                    VideoPlayer(
                        item.video, shown.paused, modifier, endedText = null,
                        onProgress = { pos, dur -> if (dur > 0) progress = pos.toFloat() / dur },
                        onEnded = { advance() },
                    )
                }
            } else StoryPhoto(item?.image, modifier, shown.paused)
        }
    } else {
        InstaTrayScreen(reels, shown.reel, session.trayLoading, session.loggedIn)
    }
}
