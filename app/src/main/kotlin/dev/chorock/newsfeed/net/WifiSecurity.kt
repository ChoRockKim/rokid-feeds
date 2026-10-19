package dev.chorock.newsfeed.net

/** 앱이 다루는 Wi-Fi 보안 종류. 기업망(802.1X)과 WEP는 지원하지 않는다. */
enum class WifiSecurity {
    Open,
    /** Enhanced Open(OWE): 비밀번호 없이 암호화 */
    Owe,
    Wpa2,
    Wpa3,
    Unsupported,
    ;

    val needsPassword: Boolean get() = this == Wpa2 || this == Wpa3
    val isSupported: Boolean get() = this != Unsupported

    /** 목록에 보여 줄 짧은 표시. */
    val label: String
        get() = when (this) {
            Open -> "개방"
            Owe -> "개방(암호화)"
            Wpa2 -> "WPA2"
            Wpa3 -> "WPA3"
            Unsupported -> "미지원"
        }
}

/**
 * `ScanResult.capabilities` 문자열(예: `[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]`)에서 보안 종류를 정한다.
 * WPA2/WPA3 혼합(transition)은 호환성이 넓은 WPA2로 본다. 개방형과 OWE가 함께 광고되는 transition은 개방형으로 본다.
 */
fun securityOf(capabilities: String): WifiSecurity {
    val caps = capabilities.uppercase()
    return when {
        "EAP" in caps -> WifiSecurity.Unsupported
        "WEP" in caps -> WifiSecurity.Unsupported
        "PSK" in caps -> WifiSecurity.Wpa2
        "SAE" in caps -> WifiSecurity.Wpa3
        "OWE_TRANSITION" in caps -> WifiSecurity.Open
        "OWE" in caps -> WifiSecurity.Owe
        // 보안 표시가 있는데 위에서 걸리지 않은 경우(예: 구형 WPA 단독)는 안전하게 미지원으로 둔다
        "WPA" in caps || "RSN" in caps -> WifiSecurity.Unsupported
        else -> WifiSecurity.Open
    }
}

/**
 * 비밀번호가 보안 종류의 규칙에 맞는지 검사한다. 맞으면 null, 아니면 사용자에게 보여 줄 문구.
 * WPA2: 8~63자 ASCII 또는 64자리 16진수(PSK), WPA3: 1~128자. 안드로이드는 규칙에 어긋나면 예외를 던지므로 미리 막는다.
 */
fun validatePassphrase(security: WifiSecurity, passphrase: String): String? = when (security) {
    WifiSecurity.Wpa2 -> when {
        passphrase.isEmpty() -> "비밀번호를 입력하세요"
        passphrase.length in 8..63 -> null
        passphrase.length == 64 && passphrase.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } -> null
        passphrase.length < 8 -> "WPA2 비밀번호는 8자 이상이어야 합니다"
        else -> "WPA2 비밀번호는 63자 이하여야 합니다"
    }
    WifiSecurity.Wpa3 -> when {
        passphrase.isEmpty() -> "비밀번호를 입력하세요"
        passphrase.length > 128 -> "WPA3 비밀번호는 128자 이하여야 합니다"
        else -> null
    }
    WifiSecurity.Unsupported -> "지원하지 않는 보안 방식입니다"
    WifiSecurity.Open, WifiSecurity.Owe -> null
}
