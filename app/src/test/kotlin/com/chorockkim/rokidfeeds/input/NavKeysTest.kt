package com.chorockkim.rokidfeeds.input

import android.view.KeyEvent
import com.chorockkim.rokidfeeds.input.GlassesInput.Action
import com.chorockkim.rokidfeeds.nav.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavKeysTest {
    @Test
    fun everyGlassesActionHasOneMeaning() {
        assertEquals(NavKey.Previous, Action.PREVIOUS.toNavKey())
        assertEquals(NavKey.Previous, Action.UP.toNavKey())
        assertEquals(NavKey.Next, Action.NEXT.toNavKey())
        assertEquals(NavKey.Next, Action.DOWN.toNavKey())
        assertEquals(NavKey.Select, Action.SELECT.toNavKey())
        assertEquals(NavKey.Back, Action.BACK.toNavKey())
        assertNull(Action.NONE.toNavKey())
        assertNull(Action.DUPLICATE.toNavKey())
        assertNull(Action.VOLUME_UP.toNavKey())
    }

    @Test
    fun rokidKeysAndRingMediaKeysReachTheNavigator() {
        var now = 0L
        val input = GlassesInput { now }
        fun press(code: Int): NavKey? = input.map(KeyEvent.ACTION_DOWN, code, 0).toNavKey().also { now += 1_000 }

        assertEquals(NavKey.Next, press(GlassesInput.KEYCODE_ROKID_NEXT))
        assertEquals(NavKey.Previous, press(GlassesInput.KEYCODE_ROKID_PREVIOUS))
        assertEquals(NavKey.Select, press(GlassesInput.KEYCODE_ROKID_DOUBLE_TAP))
        assertEquals(NavKey.Next, press(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(NavKey.Select, press(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(NavKey.Next, press(KeyEvent.KEYCODE_DPAD_DOWN))
    }
}
