package dev.chorock.newsfeed

import dev.chorock.newsfeed.net.RawScan
import dev.chorock.newsfeed.net.WifiSecurity
import dev.chorock.newsfeed.net.signalBars
import dev.chorock.newsfeed.net.signalLevel
import dev.chorock.newsfeed.net.toNetworks
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiNetworksTest {
    private fun scan(ssid: String, rssi: Int, caps: String = "[ESS]") = RawScan(ssid, caps, rssi)

    @Test
    fun `신호가 강한 순으로 정렬한다`() {
        val networks = toNetworks(listOf(scan("약함", -80), scan("강함", -40), scan("중간", -60)))
        assertEquals(listOf("강함", "중간", "약함"), networks.map { it.ssid })
    }

    @Test
    fun `같은 SSID는 가장 강한 하나만 남기고 그 보안을 쓴다`() {
        val networks = toNetworks(
            listOf(
                scan("집", -70, "[WPA2-PSK-CCMP][ESS]"),
                scan("집", -45, "[RSN-SAE-CCMP][ESS]"), // 공유기가 여러 대: 강한 쪽 기준
                scan("집", -90, "[ESS]"),
            ),
        )
        assertEquals(1, networks.size)
        assertEquals(-45, networks.single().rssi)
        assertEquals(WifiSecurity.Wpa3, networks.single().security)
    }

    @Test
    fun `숨김(SSID가 빈) 네트워크는 제외한다`() {
        assertEquals(listOf("보임"), toNetworks(listOf(scan("", -30), scan("  ", -30), scan("보임", -70))).map { it.ssid })
    }

    @Test
    fun `신호가 같으면 이름순`() {
        assertEquals(listOf("가", "나", "다"), toNetworks(listOf(scan("다", -50), scan("가", -50), scan("나", -50))).map { it.ssid })
    }

    @Test
    fun `빈 입력이면 빈 목록`() = assertEquals(emptyList<Any>(), toNetworks(emptyList()))

    @Test
    fun `신호 단계 경계값`() {
        assertEquals(4, signalLevel(-55)); assertEquals(3, signalLevel(-56))
        assertEquals(3, signalLevel(-65)); assertEquals(2, signalLevel(-66))
        assertEquals(2, signalLevel(-75)); assertEquals(1, signalLevel(-76))
        assertEquals(1, signalLevel(-85)); assertEquals(0, signalLevel(-86))
        assertEquals(4, signalLevel(-20)); assertEquals(0, signalLevel(-120))
    }

    @Test
    fun `신호 막대는 단계만큼 채운다`() {
        assertEquals("▂▄▆█", signalBars(4))
        assertEquals("▂▄▆ ", signalBars(3))
        assertEquals("    ", signalBars(0))
        assertEquals(4, signalBars(2).length)
    }
}
