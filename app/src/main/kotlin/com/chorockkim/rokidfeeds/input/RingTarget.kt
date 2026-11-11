package com.chorockkim.rokidfeeds.input

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction

/**
 * Bridge target for R08 Access Bridge (github.com/Anezium/R08-Access-Bridge).
 *
 * The ring sends media keys that the bridge's accessibility service consumes. For apps without a
 * dedicated integration, the bridge walks the window's clickable/focusable accessibility nodes:
 * it input-focuses one, clicks it on a single tap, and when there is no next node it performs
 * ACTION_SCROLL_FORWARD / BACKWARD on the focused node; double tap is the global Back action.
 *
 * So the app exposes exactly one node: this transparent view over the band (the screens below it
 * are hidden from accessibility). Ring next/previous arrive as scroll forward/backward and ring
 * tap as a click, and all of them are fed into the same one-axis navigation as the touchpad.
 */
class RingTarget(context: Context, private val onAction: (GlassesInput.Action) -> Unit) : View(context) {
    /**
     * 창이 다시 포커스를 받을 때 이 노드가 포커스를 되찾을지. R08 브리지는 지금 포커스된 노드가 이 노드가 아니면 첫 링 입력을
     * "이 노드로 포커스 옮기기"에 써 버린다. 로그인 화면처럼 다른 View가 키 입력을 받아야 할 때만 false.
     * (0.2.3에서는 포커스를 잃을 때마다 바로 되찾게 했는데, 다른 View와 포커스를 서로 뺏으며 링이 거의 안 먹었다. 그래서 뺐다.)
     */
    var keepFocus: () -> Boolean = { true }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        // 화면 전체를 덮는 View라 포커스를 받으면 Android 기본 포커스 강조(테마 강조색, 반투명 청록)가 화면 전체에 깔린다.
        if (android.os.Build.VERSION.SDK_INT >= 26) defaultFocusHighlightEnabled = false
        contentDescription = "Rokid Feeds"
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && keepFocus() && !hasFocus()) requestFocus()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = View::class.java.name
        info.isScrollable = true
        info.isClickable = true
        info.isFocusable = true
        info.addAction(AccessibilityAction.ACTION_CLICK)
        info.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        val mapped = when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> GlassesInput.Action.NEXT
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> GlassesInput.Action.PREVIOUS
            AccessibilityNodeInfo.ACTION_CLICK -> GlassesInput.Action.SELECT
            else -> null
        }
        if (mapped != null) {
            onAction(mapped)
            return true
        }
        return super.performAccessibilityAction(action, arguments)
    }

    /** A plain click (e.g. a tap gesture the bridge falls back to) is a select too. */
    override fun performClick(): Boolean {
        super.performClick()
        onAction(GlassesInput.Action.SELECT)
        return true
    }
}
