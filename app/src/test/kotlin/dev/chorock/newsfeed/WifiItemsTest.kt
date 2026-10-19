package dev.chorock.newsfeed

import dev.chorock.newsfeed.nav.FormItem
import dev.chorock.newsfeed.nav.PickerItem
import dev.chorock.newsfeed.nav.formError
import dev.chorock.newsfeed.nav.formItems
import dev.chorock.newsfeed.nav.nextManualSecurity
import dev.chorock.newsfeed.nav.pickerItems
import dev.chorock.newsfeed.net.WifiNetwork
import dev.chorock.newsfeed.net.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WifiItemsTest {
    private val home = WifiNetwork("집", WifiSecurity.Wpa2, -50)
    private val cafe = WifiNetwork("카페", WifiSecurity.Open, -70)

    @Test
    fun `목록은 네트워크 뒤에 직접 입력·다시 검색·시스템 설정이 온다`() {
        val items = pickerItems(listOf(home, cafe), needsPermission = false)
        assertEquals(
            listOf(PickerItem.Network(home), PickerItem.Network(cafe), PickerItem.Manual, PickerItem.Rescan, PickerItem.SystemSettings),
            items,
        )
    }

    @Test
    fun `권한이 없으면 맨 위에 권한 줄이 붙고 네트워크는 없다`() {
        val items = pickerItems(emptyList(), needsPermission = true)
        assertEquals(PickerItem.Permission, items.first())
        assertEquals(listOf(PickerItem.Permission, PickerItem.Manual, PickerItem.Rescan, PickerItem.SystemSettings), items)
    }

    @Test
    fun `네트워크가 없어도 직접 입력과 시스템 설정은 늘 남는다`() {
        val items = pickerItems(emptyList(), needsPermission = false)
        assertEquals(listOf(PickerItem.Manual, PickerItem.Rescan, PickerItem.SystemSettings), items)
    }

    @Test
    fun `목록에서 고른 보안 네트워크의 폼은 비밀번호만 입력한다`() {
        assertEquals(
            listOf(FormItem.Password, FormItem.Reveal, FormItem.Connect, FormItem.Cancel),
            formItems(manual = false, security = WifiSecurity.Wpa2),
        )
    }

    @Test
    fun `직접 입력 폼은 SSID와 보안도 정한다`() {
        assertEquals(
            listOf(FormItem.Ssid, FormItem.Security, FormItem.Password, FormItem.Reveal, FormItem.Connect, FormItem.Cancel),
            formItems(manual = true, security = WifiSecurity.Wpa3),
        )
    }

    @Test
    fun `직접 입력에서 개방형을 고르면 비밀번호 줄이 빠진다`() {
        assertEquals(
            listOf(FormItem.Ssid, FormItem.Security, FormItem.Connect, FormItem.Cancel),
            formItems(manual = true, security = WifiSecurity.Open),
        )
    }

    @Test
    fun `보안 종류는 개방 WPA2 WPA3 순으로 돈다`() {
        assertEquals(WifiSecurity.Wpa2, nextManualSecurity(WifiSecurity.Open))
        assertEquals(WifiSecurity.Wpa3, nextManualSecurity(WifiSecurity.Wpa2))
        assertEquals(WifiSecurity.Open, nextManualSecurity(WifiSecurity.Wpa3))
    }

    @Test
    fun `직접 입력은 SSID가 비면 막는다`() {
        assertNotNull(formError(manual = true, ssid = "  ", security = WifiSecurity.Open, password = ""))
        assertNull(formError(manual = true, ssid = "집", security = WifiSecurity.Open, password = ""))
    }

    @Test
    fun `목록에서 고른 경우는 SSID를 검사하지 않고 비밀번호 규칙만 본다`() {
        assertNull(formError(manual = false, ssid = "", security = WifiSecurity.Wpa2, password = "12345678"))
        assertNotNull(formError(manual = false, ssid = "집", security = WifiSecurity.Wpa2, password = "123"))
    }
}
