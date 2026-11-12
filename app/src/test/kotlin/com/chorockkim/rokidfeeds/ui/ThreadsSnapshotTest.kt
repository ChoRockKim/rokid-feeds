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
import com.chorockkim.rokidfeeds.threads.buildPostPages
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
            ThreadPost("6", "R2", "someone", "[예시] 두 번째 댓글, 사진과 답글이 있습니다", now - 200, replyCount = 4, images = listOf("r1", "r2")),
            ThreadPost("7", "R3", "third.reply", "[예시] 세 번째 댓글", now - 100, likeCount = 1),
        ),
    )
    private val pages = buildPostPages(items[0], detail)

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
    fun settings_confirmLogout() = shot { SettingsScreen(selected = 0, threadsLoggedIn = true, confirmLogout = true, version = "0.2.11") }

    @Test
    fun threads_feed() = shot { ThreadsFeedScreen(items, selectedIndex = 0, loading = false, loggedIn = true, now = now, image = fakePhoto, lazyThumbs = false) }

    @Test
    fun threads_feed_longPost() = shot {
        val long = ThreadPost("9", "L1", "long.writer", List(14) { "[예시] 긴 글의 ${it + 1}번째 문장입니다." }.joinToString(" "), now - 900, likeCount = 12, replyCount = 5)
        ThreadsFeedScreen(listOf(ThreadItem(long)) + items, selectedIndex = 0, loading = false, loggedIn = true, now = now, image = fakePhoto, lazyThumbs = false)
    }

    @Test
    fun threads_empty_notLoggedIn() = shot { ThreadsFeedScreen(emptyList(), selectedIndex = 0, loading = false, loggedIn = false, now = now) }

    @Test
    fun threads_post() = shot { ThreadPostScreen(pages[0], 0, pages.size, comments = 3, now = now, image = fakePhoto) }

    @Test
    fun threads_post_continuation() = shot { ThreadPostScreen(pages.last(), pages.lastIndex, pages.size, comments = 3, now = now, image = fakePhoto) }

    private val longWithPhotos = ThreadPost(
        "20", "LP", "photo.writer",
        List(24) { "[예시] 사진이 붙은 긴 글의 ${it + 1}번째 문장입니다." }.joinToString(" "),
        now - 1200, likeCount = 31, replyCount = 7, images = listOf("a", "b", "c"),
    )

    /** 앱과 같은 줄 재기로 나눈 쪽(글꼴이 달라도 넘치지 않는지 본다). */
    private fun measuredShot(post: ThreadPost, page: (List<com.chorockkim.rokidfeeds.threads.PostPage>) -> Int) = paparazzi.snapshot {
        val p = buildPostPages(ThreadItem(post), null, rememberPostSplitter())
        val i = page(p)
        GlassScreen(status) { ThreadPostScreen(p[i], i, p.size, comments = 7, now = now, image = fakePhoto) }
    }

    @Test
    fun threads_post_longWithPhotos_firstPage() = measuredShot(longWithPhotos) { 0 }

    @Test
    fun threads_post_longWithPhotos_secondPage() = measuredShot(longWithPhotos) { 1 }

    @Test
    fun threads_post_longTextOnly_firstPage() = measuredShot(longWithPhotos.copy(images = emptyList(), thumbs = emptyList())) { 0 }

    @Test
    fun threads_post_longTextOnly_lastPage() = measuredShot(longWithPhotos.copy(images = emptyList(), thumbs = emptyList())) { it.lastIndex }

    /** 세로로 긴 가짜 사진: 위에 "위", 아래에 "아래"를 써서 확대한 절반이 맞는지 본다. */
    private val tallPhoto: @Composable (String) -> ImageState = {
        val bitmap = Bitmap.createBitmap(400, 1200, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, 1200f, 0xFF4060FF.toInt(), 0xFFFFFFFF.toInt(), Shader.TileMode.CLAMP) })
        val text = Paint().apply { color = 0xFF000000.toInt(); textSize = 90f; isAntiAlias = true }
        c.drawText("TOP", 120f, 110f, text)
        c.drawText("MIDDLE", 60f, 640f, text)
        c.drawText("BOTTOM", 50f, 1170f, text)
        ImageState(bitmap.asImageBitmap(), failed = false)
    }

    @Test
    fun threads_zoom_top() = shot { ThreadPhotoZoomScreen("tall", part = 0, image = tallPhoto) }

    @Test
    fun threads_zoom_middle() = shot { ThreadPhotoZoomScreen("tall", part = 1, image = tallPhoto) }

    @Test
    fun threads_zoom_bottom() = shot { ThreadPhotoZoomScreen("tall", part = 2, image = tallPhoto) }

    @Test
    fun threads_photo() = shot { ThreadPhotoScreen(main, "photo", 0, 2, image = fakePhoto) }

    @Test
    fun threads_comments() = shot {
        ThreadCommentsScreen(detail.replies, selected = 1, loading = false, expected = 3, now = now, image = fakePhoto, lazyThumbs = false)
    }

    @Test
    fun threads_login_waiting() = shot {
        ThreadsLoginScreen(com.chorockkim.rokidfeeds.threads.ThreadsLoginFlow.Ui.Waiting("server-v2-production-8f97.up.railway.app/login", "482913"))
    }

    @Test
    fun threads_login_needCode() = shot {
        ThreadsLoginScreen(com.chorockkim.rokidfeeds.threads.ThreadsLoginFlow.Ui.NeedCode("server-v2-production-8f97.up.railway.app/login", "482913", "보안 코드"))
    }
}
