package dev.chorock.newsfeed.net

/** 앱에 미리 넣어 둔 Wi-Fi 정보. 글래스에서는 비밀번호를 입력할 수 없으므로 빌드 시점에 정한다. */
data class WifiConfig(val ssid: String, val password: String) {
    val isConfigured: Boolean get() = ssid.isNotBlank()
    val isOpen: Boolean get() = password.isBlank()

    companion object {
        fun fromBuild() = WifiConfig(
            ssid = dev.chorock.newsfeed.BuildConfig.WIFI_SSID,
            password = dev.chorock.newsfeed.BuildConfig.WIFI_PASSWORD,
        )
    }
}
