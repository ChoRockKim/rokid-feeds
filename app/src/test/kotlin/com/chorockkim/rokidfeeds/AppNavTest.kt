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
    fun `메뉴는 뉴스·Threads·설정이고 양끝에서 순환한다`() {
        assertEquals(listOf(Screen.News, Screen.Threads, Screen.Settings), MenuItem.entries.map { it.screen })
        val last = MenuItem.entries.lastIndex
        assertEquals(last, AppNav.reduceMenu(AppState(), NavKey.Previous).state.menuSelected)
        assertEquals(0, AppNav.reduceMenu(AppState(menuSelected = last), NavKey.Next).state.menuSelected)
        assertEquals(Screen.Settings, AppNav.reduceMenu(AppState(menuSelected = 2), NavKey.Select).state.screen)
    }

    @Test
    fun `메뉴에서 뒤로는 종료, 화면에서 메뉴로 돌아오면 고른 줄은 그대로`() {
        assertTrue(AppNav.reduceMenu(AppState(), NavKey.Back).exit)
        assertFalse(AppNav.reduceMenu(AppState(), NavKey.Next).exit)
        assertEquals(AppState(Screen.Menu, 1), AppNav.backToMenu(AppState(Screen.Threads, menuSelected = 1)))
    }

    @Test
    fun `설정에서 로그인 안 했으면 선택 = 로그인 화면, 로그인 화면에서 뒤로 = 설정`() {
        val settings = AppState(Screen.Settings)
        val r = AppNav.reduceSettings(settings, NavKey.Select, loggedIn = false)
        assertEquals(Screen.ThreadsLogin, r.state.screen)
        assertFalse(r.logout)
        assertEquals(Screen.Settings, AppNav.backFromLogin(r.state).screen)
    }

    @Test
    fun `설정에서 로그아웃은 두 번 눌러야 하고 쓸기·뒤로는 확인을 취소한다`() {
        val settings = AppState(Screen.Settings)
        val first = AppNav.reduceSettings(settings, NavKey.Select, loggedIn = true)
        assertTrue(first.state.confirmLogout)
        assertFalse(first.logout)
        val second = AppNav.reduceSettings(first.state, NavKey.Select, loggedIn = true)
        assertTrue(second.logout)
        assertFalse(second.state.confirmLogout)
        assertFalse(AppNav.reduceSettings(first.state, NavKey.Next, loggedIn = true).state.confirmLogout)
        val back = AppNav.reduceSettings(first.state, NavKey.Back, loggedIn = true)
        assertEquals(Screen.Menu, back.state.screen)
        assertFalse(back.state.confirmLogout || back.logout)
    }
}
