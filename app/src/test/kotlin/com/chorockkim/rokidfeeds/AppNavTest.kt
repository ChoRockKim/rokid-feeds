package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.AccountRow
import com.chorockkim.rokidfeeds.nav.AppNav
import com.chorockkim.rokidfeeds.nav.Reduction2
import com.chorockkim.rokidfeeds.nav.AppState
import com.chorockkim.rokidfeeds.nav.MenuItem
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavTest {
    @Test
    fun `메뉴는 뉴스·Threads·설정이고 양끝에서 순환한다`() {
        assertEquals(listOf(Screen.News, Screen.Threads, Screen.Insta, Screen.Settings), MenuItem.entries.map { it.screen })
        assertEquals(Screen.Insta, AppNav.backFromLogin(AppState(Screen.InstaLogin)).screen)
        val last = MenuItem.entries.lastIndex
        assertEquals(last, AppNav.reduceMenu(AppState(), NavKey.Previous).state.menuSelected)
        assertEquals(0, AppNav.reduceMenu(AppState(menuSelected = last), NavKey.Next).state.menuSelected)
        assertEquals(Screen.Settings, AppNav.reduceMenu(AppState(menuSelected = 3), NavKey.Select).state.screen)
    }

    @Test
    fun `메뉴에서 뒤로는 종료, 화면에서 메뉴로 돌아오면 고른 줄은 그대로`() {
        assertTrue(AppNav.reduceMenu(AppState(), NavKey.Back).exit)
        assertFalse(AppNav.reduceMenu(AppState(), NavKey.Next).exit)
        assertEquals(AppState(Screen.Menu, 1), AppNav.backToMenu(AppState(Screen.Threads, menuSelected = 1)))
    }

    private fun rows(vararg r: AccountRow) = r.toList()
    private val oneIn = rows(AccountRow(loggedIn = true, active = true), AccountRow(loggedIn = false, active = false))

    @Test
    fun `로그인 안 한 계정을 고르면 그 계정으로 바꾸고 로그인 화면, 로그인 화면에서 뒤로 = 설정`() {
        val r = AppNav.reduceSettings(AppState(Screen.Settings, settingsSelected = 1), NavKey.Select, oneIn)
        assertEquals(Screen.ThreadsLogin, r.state.screen)
        assertEquals(1, r.switchTo)
        assertFalse(r.logout)
        assertEquals(Screen.Settings, AppNav.backFromLogin(r.state).screen)
        // 지금 계정이 로그인 안 돼 있으면 바꾸지 않고 로그인만
        val self = AppNav.reduceSettings(AppState(Screen.Settings), NavKey.Select, rows(AccountRow(false, true), AccountRow(false, false)))
        assertEquals(Screen.ThreadsLogin, self.state.screen)
        assertNull(self.switchTo)
    }

    @Test
    fun `로그인한 다른 계정을 고르면 바로 전환, 지원 안 하는 계정은 아무것도 안 함`() {
        val both = rows(AccountRow(true, true), AccountRow(true, false))
        val r = AppNav.reduceSettings(AppState(Screen.Settings, settingsSelected = 1), NavKey.Select, both)
        assertEquals(1, r.switchTo)
        assertEquals(Screen.Settings, r.state.screen)
        assertFalse(r.logout || r.state.confirmLogout)
        val off = rows(AccountRow(true, true), AccountRow(false, false, available = false))
        assertEquals(Reduction2(AppState(Screen.Settings, settingsSelected = 1)), AppNav.reduceSettings(AppState(Screen.Settings, settingsSelected = 1), NavKey.Select, off))
    }

    @Test
    fun `지금 계정 로그아웃은 두 번 눌러야 하고 쓸기·뒤로는 확인을 취소한다`() {
        val settings = AppState(Screen.Settings)
        val first = AppNav.reduceSettings(settings, NavKey.Select, oneIn)
        assertTrue(first.state.confirmLogout)
        assertFalse(first.logout)
        val second = AppNav.reduceSettings(first.state, NavKey.Select, oneIn)
        assertTrue(second.logout)
        assertNull(second.switchTo)
        assertFalse(second.state.confirmLogout)
        assertFalse(AppNav.reduceSettings(first.state, NavKey.Next, oneIn).state.confirmLogout)
        assertEquals(1, AppNav.reduceSettings(settings, NavKey.Next, oneIn).state.settingsSelected)
        assertEquals(0, AppNav.reduceSettings(AppState(Screen.Settings, settingsSelected = 1), NavKey.Next, oneIn).state.settingsSelected)
        val back = AppNav.reduceSettings(first.state, NavKey.Back, oneIn)
        assertEquals(Screen.Menu, back.state.screen)
        assertFalse(back.state.confirmLogout || back.logout)
    }
}
