package com.chorockkim.rokidfeeds.input

import android.os.SystemClock
import android.view.KeyEvent

/**
 * Rokid glasses touchpad -> app actions. Same mapping as RokidPipe's RokidKeyMapper, which was
 * validated on the glasses: swipe = DPAD left/right (plus Rokid keys 183/184), tap = ENTER,
 * the Rokid double-tap key (202) = select, system back = back. The R08 ring's raw media keys
 * (next / previous / play-pause) map the same way for when R08 Access Bridge is not running;
 * with the bridge running its accessibility service consumes them and drives [RingTarget].
 */
class GlassesInput(private val clock: () -> Long = SystemClock::elapsedRealtime) {
    /**
     * [UP] / [DOWN] are vertical swipes (DPAD up/down). Screens that don't handle them get them
     * as [PREVIOUS] / [NEXT] (see MainActivity), so lists move the same as before.
     */
    enum class Action { NONE, DUPLICATE, PREVIOUS, NEXT, UP, DOWN, SELECT, BACK, VOLUME_UP, VOLUME_DOWN }

    private var lastDirectionAt = 0L

    fun map(event: KeyEvent): Action = map(event.action, event.keyCode, event.repeatCount)

    fun map(eventAction: Int, keyCode: Int, repeatCount: Int): Action {
        if (eventAction != KeyEvent.ACTION_DOWN) return Action.NONE
        // Volume keys repeat while held: every repeat is one more step.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) return Action.VOLUME_UP
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) return Action.VOLUME_DOWN
        if (repeatCount > 0) return Action.DUPLICATE
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> debounce(Action.UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> debounce(Action.DOWN)

            KeyEvent.KEYCODE_DPAD_LEFT, KEYCODE_ROKID_PREVIOUS -> debounce(Action.PREVIOUS)
            KeyEvent.KEYCODE_DPAD_RIGHT, KEYCODE_ROKID_NEXT -> debounce(Action.NEXT)

            // R08 링 미디어 키(브리지가 꺼져 있을 때): 터치패드용 240ms는 빨리 넘길 때 입력을 버려서 길었고(0.2.2),
            // 아예 안 거르면 한 번에 두 번 오는 경우 두 칸씩 넘어갈 수 있다(0.2.3). 짧게 120ms만 거른다.
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> debounce(Action.PREVIOUS, MEDIA_DEBOUNCE_MS)
            KeyEvent.KEYCODE_MEDIA_NEXT -> debounce(Action.NEXT, MEDIA_DEBOUNCE_MS)

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE, KEYCODE_ROKID_DOUBLE_TAP,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> Action.SELECT

            KeyEvent.KEYCODE_BACK -> Action.BACK
            else -> Action.NONE
        }
    }

    /** One physical swipe can arrive as two directional events; drop the second one. */
    private fun debounce(action: Action, window: Long = DIRECTION_DEBOUNCE_MS): Action {
        val now = clock()
        if (lastDirectionAt > 0L && now - lastDirectionAt < window) {
            return Action.DUPLICATE
        }
        lastDirectionAt = now
        return action
    }

    companion object {
        const val KEYCODE_ROKID_NEXT = 183
        const val KEYCODE_ROKID_PREVIOUS = 184
        const val KEYCODE_ROKID_DOUBLE_TAP = 202
        const val DIRECTION_DEBOUNCE_MS = 240L
        const val MEDIA_DEBOUNCE_MS = 120L
    }
}
