package dev.chorock.newsfeed.net

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 시스템 Wi-Fi 설정 화면을 연다. 네트워크 선택과 비밀번호 입력은 그 화면에서 한다.
 * 이 기기에 해당 화면이 없으면 실패를 돌려준다(글래스 OS에 설정 앱이 있는지는 확인하지 못했다).
 */
fun openWifiSettings(context: Context): Result<Unit> =
    try {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Result.success(Unit)
    } catch (e: ActivityNotFoundException) {
        Result.failure(IllegalStateException("이 기기에서 Wi-Fi 설정 화면을 열 수 없습니다", e))
    }

/**
 * Wi-Fi를 켜는 시스템 패널(인터넷 연결 패널, Android 10+)을 연다. 앱이 Wi-Fi를 직접 켤 수는 없어서 사용자가 그 패널에서 켠다.
 * 패널이 없으면 Wi-Fi 설정 화면으로 대신한다. 글래스 OS에 이 패널이 있는지는 확인하지 못했다.
 */
fun openWifiPanel(context: Context): Result<Unit> =
    try {
        context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Result.success(Unit)
    } catch (e: ActivityNotFoundException) {
        openWifiSettings(context)
    }
