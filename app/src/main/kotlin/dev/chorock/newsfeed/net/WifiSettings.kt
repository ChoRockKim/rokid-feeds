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
