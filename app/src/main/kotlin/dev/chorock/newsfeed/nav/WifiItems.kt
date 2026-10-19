package dev.chorock.newsfeed.nav

import dev.chorock.newsfeed.net.WifiNetwork
import dev.chorock.newsfeed.net.WifiSecurity
import dev.chorock.newsfeed.net.validatePassphrase

/** 네트워크 목록 화면의 한 줄. 순서가 곧 ←/→로 지나가는 순서다. */
sealed interface PickerItem {
    /** 스캔 권한을 요청하는 줄(권한이 없을 때만 맨 위에 나온다). */
    data object Permission : PickerItem
    data class Network(val network: WifiNetwork) : PickerItem
    /** 목록에 없는(숨김) 네트워크를 SSID와 비밀번호로 직접 입력한다. */
    data object Manual : PickerItem
    data object Rescan : PickerItem
    /** 시스템 Wi-Fi 설정 화면. 이 앱의 화면이 막혔을 때를 위한 보험이다. */
    data object SystemSettings : PickerItem
}

fun pickerItems(networks: List<WifiNetwork>, needsPermission: Boolean): List<PickerItem> = buildList {
    if (needsPermission) add(PickerItem.Permission)
    networks.forEach { add(PickerItem.Network(it)) }
    add(PickerItem.Manual)
    add(PickerItem.Rescan)
    add(PickerItem.SystemSettings)
}

/** 입력 폼의 한 줄. */
enum class FormItem { Ssid, Security, Password, Reveal, Connect, Cancel }

/**
 * 폼에 나오는 줄. [manual]이면 SSID와 보안 종류도 직접 정하고, 아니면 목록에서 고른 네트워크의 값을 쓴다.
 * 비밀번호가 필요 없는 보안이면 비밀번호·표시 줄을 뺀다.
 */
fun formItems(manual: Boolean, security: WifiSecurity): List<FormItem> = buildList {
    if (manual) {
        add(FormItem.Ssid)
        add(FormItem.Security)
    }
    if (security.needsPassword) {
        add(FormItem.Password)
        add(FormItem.Reveal)
    }
    add(FormItem.Connect)
    add(FormItem.Cancel)
}

/** 직접 입력에서 고를 수 있는 보안 종류를 돌린다: 개방 → WPA2 → WPA3 → 개방. */
fun nextManualSecurity(current: WifiSecurity): WifiSecurity = when (current) {
    WifiSecurity.Open -> WifiSecurity.Wpa2
    WifiSecurity.Wpa2 -> WifiSecurity.Wpa3
    else -> WifiSecurity.Open
}

/** 연결 버튼을 눌렀을 때 입력이 올바른지 검사한다. 올바르면 null, 아니면 보여 줄 문구. */
fun formError(manual: Boolean, ssid: String, security: WifiSecurity, password: String): String? {
    if (manual && ssid.isBlank()) return "SSID를 입력하세요"
    return validatePassphrase(security, password)
}
