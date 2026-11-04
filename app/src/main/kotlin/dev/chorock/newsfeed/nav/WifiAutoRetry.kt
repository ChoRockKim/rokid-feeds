package dev.chorock.newsfeed.nav

/** 자동 재시도 간격. 시스템이 연결 확인 창을 띄울 수 있어 너무 짧게 잡지 않는다. */
const val AUTO_RETRY_INTERVAL_MS = 10_000L

/** Wi-Fi를 켠 직후 첫 시도까지의 대기. 시스템이 저장된 네트워크에 스스로 붙을 시간을 먼저 준다. */
const val AUTO_RETRY_FIRST_DELAY_MS = 5_000L

/** 연속 자동 시도 상한. 넘으면 멈추고 사용자가 버튼을 눌러야 다시 시작한다. */
const val AUTO_RETRY_MAX = 5

/**
 * 저장된 네트워크로 자동 연결을 다시 시도할지(순수).
 * Wi-Fi가 켜져 있고, 저장된 네트워크가 있고, 홈 화면이며(목록·폼에서 사용자가 조작 중이면 방해하지 않는다),
 * 연결 중이 아니고, 상한을 넘기지 않았을 때만 시도한다.
 * 사용자가 화면을 만졌으면([userActive]) 하지 않는다: 시스템 확인 창이 앱을 덮어 버튼이 안 눌리는 것처럼 보이기 때문이다.
 */
fun shouldAutoRetry(
    wifiOn: Boolean, hasSaved: Boolean, onHome: Boolean, connecting: Boolean, attempts: Int, userActive: Boolean = false,
): Boolean = wifiOn && hasSaved && onHome && !connecting && !userActive && attempts < AUTO_RETRY_MAX
