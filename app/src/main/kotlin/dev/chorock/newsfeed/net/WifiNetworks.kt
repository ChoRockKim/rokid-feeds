package dev.chorock.newsfeed.net

/** 스캔 결과에서 뽑은 원시 값. Android 타입과 분리해 JVM에서 테스트한다. */
data class RawScan(val ssid: String, val capabilities: String, val rssi: Int)

data class WifiNetwork(
    val ssid: String,
    val security: WifiSecurity,
    val rssi: Int,
) {
    /** 신호 세기 0..4. */
    val level: Int get() = signalLevel(rssi)
}

/** dBm을 0..4단계로 바꾼다. */
fun signalLevel(rssi: Int): Int = when {
    rssi >= -55 -> 4
    rssi >= -65 -> 3
    rssi >= -75 -> 2
    rssi >= -85 -> 1
    else -> 0
}

/** 막대 4칸으로 그린 신호 표시. 예: 신호 3 → "▂▄▆ ". */
fun signalBars(level: Int): String {
    val bars = listOf("▂", "▄", "▆", "█")
    return bars.mapIndexed { index, bar -> if (index < level) bar else " " }.joinToString("")
}

/**
 * 스캔 결과를 목록으로 정리한다: SSID가 비었으면(숨김) 제외하고, 같은 SSID는 신호가 가장 강한 하나만 남기고,
 * 신호가 강한 순으로 정렬한다. 신호가 같으면 이름순이다.
 */
fun toNetworks(raw: List<RawScan>): List<WifiNetwork> =
    raw.filter { it.ssid.isNotBlank() }
        .groupBy { it.ssid }
        .map { (ssid, group) ->
            val best = group.maxBy { it.rssi }
            WifiNetwork(ssid = ssid, security = securityOf(best.capabilities), rssi = best.rssi)
        }
        .sortedWith(compareByDescending<WifiNetwork> { it.rssi }.thenBy { it.ssid })
