package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.AppNav
import com.chorockkim.rokidfeeds.nav.AppState
import com.chorockkim.rokidfeeds.nav.MenuItem
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavTest {
    @Test
    fun `메뉴는 양끝에서 순환하고 선택하면 그 화면`() {
        val last = MenuItem.entries.lastIndex
        assertEquals(last, AppNav.reduceMenu(AppState(), NavKey.Previous).state.menuSelected)
        assertEquals(0, AppNav.reduceMenu(AppState(menuSelected = last), NavKey.Next).state.menuSelected)
        assertEquals(Screen.News, AppNav.reduceMenu(AppState(menuSelected = 0), NavKey.Select).state.screen)
        assertEquals(Screen.Threads, AppNav.reduceMenu(AppState(menuSelected = 1), NavKey.Select).state.screen)
        assertEquals(Screen.ThreadsLogin, AppNav.reduceMenu(AppState(menuSelected = 2), NavKey.Select).state.screen)
    }

    @Test
    fun `메뉴에서 뒤로는 종료, 화면에서 메뉴로 돌아오면 고른 줄은 그대로`() {
        assertTrue(AppNav.reduceMenu(AppState(), NavKey.Back).exit)
        assertFalse(AppNav.reduceMenu(AppState(), NavKey.Next).exit)
        val back = AppNav.backToMenu(AppState(Screen.Threads, menuSelected = 1))
        assertEquals(AppState(Screen.Menu, 1), back)
    }
}
