package dev.chorock.newsfeed.net

/**
 * 연결할 Wi-Fi 정보. 글래스에서는 비밀번호를 직접 칠 수 없으므로 빌드 시점의 값([fromBuild])이나
 * 이전에 연결에 성공해 저장해 둔 값([WifiStore])을 쓴다.
 * [security]를 모르면(null) 비밀번호가 비었는지로 추정한다: 비면 개방형, 아니면 WPA2.
 */
data class WifiConfig(
    val ssid: String,
    val password: String,
    val security: WifiSecurity? = null,
) {
    val isConfigured: Boolean get() = ssid.isNotBlank()
    val isOpen: Boolean get() = effectiveSecurity.needsPassword.not()

    val effectiveSecurity: WifiSecurity
        get() = security ?: if (password.isBlank()) WifiSecurity.Open else WifiSecurity.Wpa2

    companion object {
        fun fromBuild() = WifiConfig(
            ssid = dev.chorock.newsfeed.BuildConfig.WIFI_SSID,
            password = dev.chorock.newsfeed.BuildConfig.WIFI_PASSWORD,
        )
    }
}
