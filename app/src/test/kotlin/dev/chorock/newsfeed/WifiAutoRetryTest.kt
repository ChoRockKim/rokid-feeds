package dev.chorock.newsfeed

import dev.chorock.newsfeed.nav.AUTO_RETRY_MAX
import dev.chorock.newsfeed.nav.shouldAutoRetry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiAutoRetryTest {
    private fun retry(
        wifiOn: Boolean = true, saved: Boolean = true, home: Boolean = true, connecting: Boolean = false, attempts: Int = 0,
        userActive: Boolean = false,
    ) = shouldAutoRetry(wifiOn, saved, home, connecting, attempts, userActive)

    @Test fun `조건이 모두 맞으면 시도한다`() = assertTrue(retry())

    @Test fun `Wi-Fi가 꺼져 있으면 시도하지 않는다`() = assertFalse(retry(wifiOn = false))

    @Test fun `저장된 네트워크가 없으면 시도하지 않는다`() = assertFalse(retry(saved = false))

    @Test fun `목록이나 폼에서 조작 중이면 시도하지 않는다`() = assertFalse(retry(home = false))

    @Test fun `연결 중이면 겹쳐 시도하지 않는다`() = assertFalse(retry(connecting = true))

    @Test fun `사용자가 화면을 만졌으면 시도하지 않는다`() = assertFalse(retry(userActive = true))

    @Test fun `상한 직전까지 시도하고 상한에서 멈춘다`() {
        assertTrue(retry(attempts = AUTO_RETRY_MAX - 1))
        assertFalse(retry(attempts = AUTO_RETRY_MAX))
    }
}
