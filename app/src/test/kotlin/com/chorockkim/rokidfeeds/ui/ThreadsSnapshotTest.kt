package com.chorockkim.rokidfeeds.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.chorockkim.rokidfeeds.threads.ImageState
import com.chorockkim.rokidfeeds.threads.ThreadDetail
import com.chorockkim.rokidfeeds.threads.ThreadItem
import com.chorockkim.rokidfeeds.threads.ThreadPost
import com.chorockkim.rokidfeeds.threads.buildThreadPages
import org.junit.Rule
import org.junit.Test

/** 메인 메뉴와 Threads 화면. 글·사진은 화면 확인용 예시다(사진은 색 그라데이션으로 녹색 단색 필터를 보여 준다). */
class ThreadsSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 480, screenHeight = 640, xdpi = 160, ydpi = 160,
            density = Density.MEDIUM, orientation = ScreenOrientation.PORTRAIT, softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private val now = 1_800_000_000L
    private val status = StatusBar(clock = "14:32", battery = 82)
    private fun shot(content: @Composable () -> Unit) = paparazzi.snapshot { GlassScreen(status) { content() } }

    private val main = ThreadPost("1", "A1", "example.user", "[예시] Threads 글 본문입니다. 목록에서 탭하면 이렇게 상세가 열리고, 쓸기로 사진과 댓글 쪽을 넘깁니다.", now - 600, likeCount = 1234, replyCount = 3, images = listOf("photo", "photo2"), avatar = "avatar")
    private val items = listOf(
        ThreadItem(main, listOf(ThreadPost("2", "A2", "example.user", "[예시] 이어 쓴 글", now - 590))),
        ThreadItem(ThreadPost("3", "B1", "another.user", "[예시] 사진 없이 글만 있는 두 번째 줄입니다. 두 줄까지 보이고 넘치면 말줄임으로 끝납니다.", now - 3600, likeCount = 4)),
        ThreadItem(ThreadPost("4", "C1", "third.user", "", now - 7200, images = listOf("p1", "p2"))),
    )
    private val detail = ThreadDetail(
        main,
        listOf(ThreadPost("2", "A2", "example.user", "[예시] 이어 쓴 글", now - 590)),
        listOf(
            ThreadPost("5", "R1", "commenter", "[예시] 첫 번째 댓글입니다. 짧은 댓글은 한 쪽에 여러 개가 보입니다.", now - 300, likeCount = 3, avatar = "avatar"),
            ThreadPost("6", "R2", "someone", "[예시] 두 번째 댓글, 이 댓글에는 답글이 있습니다", now - 200, replyCount = 4),
            ThreadPost("7", "R3", "third.reply", "[예시] 세 번째 댓글", now - 100, likeCount = 1),
        ),
    )
    private val pages = buildThreadPages(items[0], detail, loading = false)

    private val fakePhoto: @Composable (String) -> ImageState = {
        val bitmap = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, 640f, 400f, intArrayOf(0xFFFF4040.toInt(), 0xFF4060FF.toInt(), 0xFFFFFFFF.toInt()), null, Shader.TileMode.CLAMP)
        })
        ImageState(bitmap.asImageBitmap(), failed = false)
    }

    @Test
    fun menu() = shot { MenuScreen(selected = 1, threadsLoggedIn = false) }

    @Test
    fun threads_feed() = shot { ThreadsFeedScreen(items, selectedIndex = 0, loading = false, loggedIn = true, now = now, image = fakePhoto, lazyThumbs = false) }

    @Test
    fun threads_empty_notLoggedIn() = shot { ThreadsFeedScreen(emptyList(), selectedIndex = 0, loading = false, loggedIn = false, now = now) }

    @Test
    fun threads_detail_text() = shot { ThreadsDetailScreen(pages, pageIndex = 0, now = now, image = fakePhoto) }

    @Test
    fun threads_detail_photo() = shot { ThreadsDetailScreen(pages, pageIndex = 1, now = now, image = fakePhoto) }

    @Test
    fun threads_detail_reply() = shot { ThreadsDetailScreen(pages, pageIndex = pages.indexOfFirst { it is com.chorockkim.rokidfeeds.threads.ThreadPage.Replies }, selectedReply = 1, now = now, image = fakePhoto) }

    @Test
    fun threads_login_hint() = shot { ThreadsLoginHint("사용자 이름, 전화번호 또는 이메일") }
}
