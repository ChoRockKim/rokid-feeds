package com.chorockkim.rokidfeeds.input

import com.chorockkim.rokidfeeds.input.GlassesInput.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** R08 브리지가 노드를 못 찾을 때 보내는 스와이프 제스처(GestureDispatcher)를 링 동작으로 바꾼다. */
class SwipeActionTest {
    private val min = 48f

    @Test
    fun bridgeVerticalSwipes() {
        // 640px 화면: 다음 = 74% → 28%(위로), 이전 = 28% → 74%(아래로)
        assertEquals(Action.NEXT, swipeAction(0f, 179f - 474f, min))
        assertEquals(Action.PREVIOUS, swipeAction(0f, 474f - 179f, min))
    }

    @Test
    fun bridgeHorizontalSwipes() {
        // 480px 화면: 다음 = 78% → 22%(왼쪽으로)
        assertEquals(Action.NEXT, swipeAction(106f - 374f, 3f, min))
        assertEquals(Action.PREVIOUS, swipeAction(374f - 106f, -3f, min))
    }

    @Test
    fun shortMovesAreTaps() {
        assertNull(swipeAction(5f, -10f, min))
        assertNull(swipeAction(0f, 0f, min))
    }
}
