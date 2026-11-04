package dev.chorock.newsfeed

import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.nav.nearestVisible
import dev.chorock.newsfeed.nav.pageKeyForScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusMathTest {
    @Test fun `선택된 항목이 보이면 그것을 고른다`() = assertEquals(5, nearestVisible(5, listOf(3, 4, 5, 6, 7)))

    @Test fun `보이지 않으면 가장 가까운 항목`() {
        assertEquals(7, nearestVisible(2, listOf(7, 8, 9)))      // 아래로 스크롤돼 선택이 위로 벗어남
        assertEquals(3, nearestVisible(20, listOf(0, 1, 2, 3)))  // 위로 스크롤돼 선택이 아래로 벗어남
    }

    @Test fun `거리가 같으면 앞쪽`() = assertEquals(4, nearestVisible(5, listOf(4, 6)))

    @Test fun `보이는 항목이 없으면 null`() = assertNull(nearestVisible(3, emptyList()))

    @Test fun `앞으로 스크롤은 다음 쪽 뒤로는 이전 쪽`() {
        assertEquals(NavKey.Next, pageKeyForScroll(0f, 120f))
        assertEquals(NavKey.Previous, pageKeyForScroll(0f, -120f))
        assertEquals(NavKey.Next, pageKeyForScroll(80f, 0f))
        assertEquals(NavKey.Previous, pageKeyForScroll(-80f, 0f))
    }

    @Test fun `움직임이 없으면 아무 키도 아님`() = assertNull(pageKeyForScroll(0f, 0f))

    @Test fun `세로 값이 있으면 세로를 우선한다`() = assertEquals(NavKey.Previous, pageKeyForScroll(50f, -10f))
}
