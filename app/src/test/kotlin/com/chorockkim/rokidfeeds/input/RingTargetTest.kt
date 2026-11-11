package com.chorockkim.rokidfeeds.input

import android.view.accessibility.AccessibilityNodeInfo
import app.cash.paparazzi.Paparazzi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * R08 Access Bridge drives non-integrated apps through accessibility: focus the node, scroll
 * forward/backward when there is no next node, click on tap. Check RingTarget answers those.
 */
class RingTargetTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun bridgeActionsBecomeGlassesActions() {
        val received = mutableListOf<GlassesInput.Action>()
        val target = RingTarget(paparazzi.context) { received += it }

        assertTrue(target.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertTrue(target.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
        assertTrue(target.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))

        assertEquals(
            listOf(GlassesInput.Action.NEXT, GlassesInput.Action.PREVIOUS, GlassesInput.Action.SELECT),
            received
        )
    }

    @Test
    fun nodeLooksLikeAScrollableClickableCandidate() {
        val target = RingTarget(paparazzi.context) {}
        assertTrue(target.isFocusable)
        assertTrue(target.isClickable)
        assertEquals(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES, target.importantForAccessibility)
    }

    @Test
    fun focusDoesNotTintTheWholeScreen() {
        // 화면 전체를 덮는 View라 기본 포커스 강조가 켜져 있으면 링으로 움직일 때 화면 전체가 뿌옇게(청록) 덮인다.
        assertEquals(false, RingTarget(paparazzi.context) {}.defaultFocusHighlightEnabled)
    }

    @Test
    fun focusIsKeptForTheBridgeUnlessReleased() {
        val target = RingTarget(paparazzi.context) {}
        assertTrue(target.keepFocus())
        target.keepFocus = { false }
        assertEquals(false, target.keepFocus())
    }
}
