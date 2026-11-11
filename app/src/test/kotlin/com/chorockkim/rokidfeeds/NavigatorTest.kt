package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.nav.NavState
import com.chorockkim.rokidfeeds.nav.Navigator
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    /** 기사 5개, 기사마다 쪽 수는 index + 1. */
    private val nav = Navigator(articleCount = 5, pageCountOf = { it + 1 })

    private fun NavState.press(vararg keys: NavKey, navigator: Navigator = nav): NavState =
        keys.fold(this) { state, key -> navigator.reduce(state, key).state }

    @Test
    fun `목록에서 이전은 맨 앞에서 마지막으로 돌아간다`() {
        assertEquals(4, NavState().press(NavKey.Previous).selected)
    }

    @Test
    fun `목록에서 다음은 마지막에서 처음으로 돌아간다`() {
        assertEquals(0, NavState(selected = 4).press(NavKey.Next).selected)
    }

    @Test
    fun `다섯 번 다음을 누르면 제자리로 돌아온다`() {
        assertEquals(0, NavState().press(*Array(5) { NavKey.Next }).selected)
    }

    @Test
    fun `기사가 0개면 모든 키가 무시된다`() {
        val empty = Navigator(0) { 1 }
        for (key in listOf(NavKey.Previous, NavKey.Next, NavKey.Select)) {
            assertEquals(NavState(), empty.reduce(NavState(), key).state)
        }
        assertTrue(empty.reduce(NavState(), NavKey.Back).exit)
    }

    @Test
    fun `기사가 1개면 이전과 다음이 제자리다`() {
        val one = Navigator(1) { 1 }
        assertEquals(0, one.reduce(NavState(), NavKey.Next).state.selected)
        assertEquals(0, one.reduce(NavState(), NavKey.Previous).state.selected)
    }

    @Test
    fun `선택하면 상세의 첫 쪽이 열린다`() {
        val state = NavState(selected = 2, page = 9).press(NavKey.Select)
        assertEquals(2, state.openIndex)
        assertEquals(0, state.page)
    }

    @Test
    fun `상세의 쪽 이동은 양 끝에서 멈춘다`() {
        // 기사 2는 쪽이 3개(0..2)
        val opened = NavState(selected = 2).press(NavKey.Select)
        assertEquals(0, opened.press(NavKey.Previous).page)
        assertEquals(2, opened.press(NavKey.Next, NavKey.Next, NavKey.Next, NavKey.Next).page)
    }

    @Test
    fun `상세에서 선택은 다음 쪽으로 넘기고 마지막 쪽에서 멈춘다`() {
        val opened = NavState(selected = 1).press(NavKey.Select) // 쪽 2개
        assertEquals(1, opened.press(NavKey.Select).page)
        assertEquals(1, opened.press(NavKey.Select, NavKey.Select, NavKey.Select).page)
    }

    @Test
    fun `상세에서 Back은 목록으로 돌아가며 선택 위치를 유지한다`() {
        val back = NavState(selected = 3).press(NavKey.Select, NavKey.Next, NavKey.Back)
        assertNull(back.openIndex)
        assertEquals(3, back.selected)
        assertEquals(0, back.page)
    }

    @Test
    fun `목록에서 Back은 종료한다`() {
        val result = nav.reduce(NavState(selected = 2), NavKey.Back)
        assertTrue(result.exit)
        assertEquals(2, result.state.selected)
    }

    @Test
    fun `상세에서 Back은 종료하지 않는다`() {
        assertFalse(nav.reduce(NavState(selected = 2, openIndex = 2), NavKey.Back).exit)
    }

    @Test
    fun `기사 수가 줄면 선택을 범위 안으로 되돌린다`() {
        val shrunk = Navigator(3) { 1 }
        assertEquals(2, shrunk.normalize(NavState(selected = 4)).selected)
    }

    @Test
    fun `열린 기사가 사라지면 목록으로 돌아간다`() {
        val shrunk = Navigator(3) { 1 }
        val state = shrunk.normalize(NavState(selected = 4, openIndex = 4, page = 2))
        assertNull(state.openIndex)
        assertEquals(0, state.page)
    }

    @Test
    fun `쪽 수가 줄면 쪽 번호를 마지막 쪽으로 되돌린다`() {
        val fewer = Navigator(5) { 2 }
        assertEquals(1, fewer.normalize(NavState(selected = 1, openIndex = 1, page = 7)).page)
    }

    @Test
    fun `어떤 키 순서 뒤에도 Back을 두 번 이내로 눌러 종료에 닿는다`() {
        val random = Random(20261019)
        val keys = NavKey.entries
        repeat(1_000) {
            var state = NavState()
            repeat(random.nextInt(0, 60)) {
                state = nav.reduce(state, keys[random.nextInt(keys.size)]).state
            }
            var exited = false
            var backs = 0
            while (!exited && backs < 2) {
                val result = nav.reduce(state, NavKey.Back)
                state = result.state
                exited = result.exit
                backs++
            }
            assertTrue("Back ${backs}번으로 종료에 닿지 못함: $state", exited)
        }
    }

    @Test
    fun `임의 키 순서 뒤에도 상태는 항상 범위 안이다`() {
        val random = Random(7)
        val keys = NavKey.entries
        var state = NavState()
        repeat(5_000) {
            state = nav.reduce(state, keys[random.nextInt(keys.size)]).state
            assertTrue(state.selected in 0 until 5)
            state.openIndex?.let { open ->
                assertTrue(open in 0 until 5)
                assertTrue(state.page in 0 until (open + 1))
            }
        }
    }
}

class WrapIndexTest {
    @Test fun `양끝에서 순환한다`() {
        assertEquals(1, com.chorockkim.rokidfeeds.nav.wrapIndex(0, 1, 2))
        assertEquals(0, com.chorockkim.rokidfeeds.nav.wrapIndex(1, 1, 2))
        assertEquals(1, com.chorockkim.rokidfeeds.nav.wrapIndex(0, -1, 2))
        assertEquals(4, com.chorockkim.rokidfeeds.nav.wrapIndex(0, -1, 5))
    }

    @Test fun `큰 이동량도 범위 안으로 돌린다`() {
        assertEquals(2, com.chorockkim.rokidfeeds.nav.wrapIndex(1, 11, 5))
        assertEquals(3, com.chorockkim.rokidfeeds.nav.wrapIndex(1, -13, 5))
    }

    @Test fun `개수가 0 이하면 그대로 둔다`() {
        assertEquals(3, com.chorockkim.rokidfeeds.nav.wrapIndex(3, 1, 0))
        assertEquals(0, com.chorockkim.rokidfeeds.nav.wrapIndex(0, -1, -2))
    }
}
