package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.util.timeAgo
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeAgoTest {
    private val now = 1_000_000L

    @Test fun `1분 미만은 방금`() = assertEquals("방금", timeAgo(now, now - 59))

    @Test fun `미래 시각도 방금`() = assertEquals("방금", timeAgo(now, now + 100))

    @Test fun `분 단위`() = assertEquals("5분 전", timeAgo(now, now - 5 * 60))

    @Test fun `시간 단위`() = assertEquals("3시간 전", timeAgo(now, now - 3 * 3_600))

    @Test fun `일 단위`() = assertEquals("2일 전", timeAgo(now, now - 2 * 86_400))

    @Test fun `경계값`() {
        assertEquals("1분 전", timeAgo(now, now - 60))
        assertEquals("1시간 전", timeAgo(now, now - 3_600))
        assertEquals("1일 전", timeAgo(now, now - 86_400))
    }
}
