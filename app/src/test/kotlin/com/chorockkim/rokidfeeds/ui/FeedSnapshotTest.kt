package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.chorockkim.rokidfeeds.data.Article
import com.chorockkim.rokidfeeds.data.Feed
import com.chorockkim.rokidfeeds.util.buildDetailPages
import org.junit.Rule
import org.junit.Test

/**
 * 안경 화면(480×640, 아래쪽 띠)을 실제 Compose 코드로 Paparazzi가 그린다(다른 Rokid 앱과 같은 방식).
 * `./gradlew :app:recordPaparazziDebug` → app/src/test/snapshots/images. 기사는 화면 확인용 예시 문구다.
 */
class FeedSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 480,
            screenHeight = 640,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.PORTRAIT,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private val now = 1_800_000_000L
    private val status = StatusBar(clock = "14:32", battery = 82)

    private val articles = listOf(
        Article("[예시] 첫 번째 기사 제목이 두 줄까지 보이는지 확인하기 위한 조금 긴 제목입니다", "https://example.com/1", SAMPLE_BODY, "홍길동 기자", now - 5 * 60, "연합뉴스"),
        Article("[예시] 두 번째 기사", "https://example.com/2", "요약", null, now - 40 * 60, "GeekNews"),
        Article("[예시] 세 번째 기사", "https://example.com/3", "요약", null, now - 2 * 3600, "동아일보"),
        Article("[예시] 네 번째 기사", "https://example.com/4", "요약", null, now - 3 * 3600, "경향신문"),
    )

    private fun shot(status: StatusBar = this.status, content: @Composable () -> Unit) =
        paparazzi.snapshot { GlassScreen(status) { content() } }

    @Test
    fun feed() = shot { FeedScreen(Feed(articles), selectedIndex = 1, now = now) }

    @Test
    fun feed_partialFailure() = shot { FeedScreen(Feed(articles, failedSources = listOf("경향신문")), selectedIndex = 0, now = now) }

    @Test
    fun detail_firstPage() {
        val pages = buildDetailPages(SAMPLE_BODY, articles[0].link)
        shot { DetailScreen(articles[0], pages, pageIndex = 0, now = now) }
    }

    @Test
    fun detail_secondPage_loadingBody() {
        val pages = buildDetailPages(SAMPLE_BODY, articles[0].link)
        shot { DetailScreen(articles[0], pages, pageIndex = 1, loading = true, now = now) }
    }

    @Test
    fun loading() = shot { LoadingView() }

    @Test
    fun battery_low() = shot(status.copy(battery = 9)) { LoadingView() }

    @Test
    fun battery_charging() = shot(status.copy(battery = 47, charging = true)) { LoadingView() }

    @Test
    fun offline() = shot(status.copy(notice = "Wi-Fi 켜는 중…")) {
        ErrorView("인터넷 연결 없음", "Wi-Fi가 연결되면 자동으로 다시 불러옵니다", hint = "뒤로: 종료")
    }

    @Test
    fun error_withUpdatePill() = shot(status.copy(update = "새 버전 v0.2.0", charging = true)) {
        ErrorView("불러오지 못했습니다", "connect timed out")
    }

    private companion object {
        val SAMPLE_BODY = List(6) { i ->
            "예시 문단 ${i + 1}입니다. 상세 화면은 스크롤 없이 쪽 단위로 넘기며, 문단과 문장 끝을 기준으로 나눕니다. " +
                "글자 크기와 줄 수가 띠 안에 넘치지 않는지 이 스냅샷으로 확인합니다."
        }.joinToString("\n\n")
    }
}
