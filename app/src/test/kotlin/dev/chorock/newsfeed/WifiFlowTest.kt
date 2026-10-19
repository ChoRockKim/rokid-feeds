package dev.chorock.newsfeed

import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.nav.WifiFlow
import dev.chorock.newsfeed.nav.WifiFlowState
import dev.chorock.newsfeed.nav.WifiPage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiFlowTest {
    private fun WifiFlowState.press(key: NavKey, count: Int) = WifiFlow.reduce(this, key, count)

    @Test
    fun `홈에서 이전과 다음은 두 버튼 사이를 순환한다`() {
        val s = WifiFlowState()
        assertEquals(1, s.press(NavKey.Next, 2).state.homeSelected)
        assertEquals(0, s.press(NavKey.Next, 2).state.press(NavKey.Next, 2).state.homeSelected)
        assertEquals(1, s.press(NavKey.Previous, 2).state.homeSelected)
    }

    @Test
    fun `목록과 폼도 양방향 순환이고 쪽마다 선택을 따로 기억한다`() {
        var s = WifiFlow.open(WifiFlowState(), WifiPage.Picker)
        s = s.press(NavKey.Previous, 5).state
        assertEquals(4, s.pickerSelected)
        assertEquals(0, s.homeSelected)
        s = WifiFlow.open(s, WifiPage.Form).press(NavKey.Next, 3).state
        assertEquals(1, s.formSelected)
        assertEquals(4, s.pickerSelected) // 폼에서 움직여도 목록 위치는 그대로
    }

    @Test
    fun `선택은 지금 항목의 번호를 알려 준다`() {
        val s = WifiFlowState(homeSelected = 1)
        val result = s.press(NavKey.Select, 2)
        assertEquals(1, result.activated)
        assertFalse(result.exit)
    }

    @Test
    fun `항목이 없으면 선택은 아무 일도 하지 않는다`() {
        assertNull(WifiFlowState().press(NavKey.Select, 0).activated)
        assertEquals(WifiFlowState(), WifiFlowState().press(NavKey.Next, 0).state)
    }

    @Test
    fun `Back은 한 쪽씩 돌아가고 홈에서만 종료한다`() {
        val form = WifiFlowState(page = WifiPage.Form)
        val picker = form.press(NavKey.Back, 3)
        assertEquals(WifiPage.Picker, picker.state.page)
        val home = picker.state.press(NavKey.Back, 5)
        assertEquals(WifiPage.Home, home.state.page)
        assertFalse(home.exit)
        assertTrue(home.state.press(NavKey.Back, 2).exit)
    }

    @Test
    fun `폼을 열면 첫 항목에서 시작한다`() {
        val s = WifiFlow.open(WifiFlowState(formSelected = 2), WifiPage.Form)
        assertEquals(0, s.formSelected)
    }

    @Test
    fun `항목 수가 줄면 선택을 범위 안으로 되돌린다`() {
        val s = WifiFlow.clamp(WifiFlowState(page = WifiPage.Picker, pickerSelected = 9), 4)
        assertEquals(3, s.pickerSelected)
    }

    @Test
    fun `어떤 키 순서 뒤에도 Back을 세 번 이내로 누르면 종료에 닿는다`() {
        val random = Random(20261019)
        val keys = NavKey.entries
        repeat(1_000) {
            var s = WifiFlowState()
            repeat(random.nextInt(0, 60)) {
                val key = keys[random.nextInt(keys.size)]
                val count = random.nextInt(0, 8)
                val result = WifiFlow.reduce(s, key, count)
                s = result.state
                // 항목을 눌렀을 때 UI가 쪽을 여는 것을 흉내 낸다
                if (result.activated != null && random.nextBoolean()) {
                    s = WifiFlow.open(s, if (s.page == WifiPage.Home) WifiPage.Picker else WifiPage.Form)
                }
            }
            var exited = false
            var backs = 0
            while (!exited && backs < 3) {
                val result = WifiFlow.reduce(s, NavKey.Back, 3)
                s = result.state
                exited = result.exit
                backs++
            }
            assertTrue("Back ${backs}번으로 종료에 닿지 못함: $s", exited)
        }
    }
}
