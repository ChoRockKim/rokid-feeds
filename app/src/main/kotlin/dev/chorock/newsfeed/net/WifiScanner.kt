package dev.chorock.newsfeed.net

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.location.LocationManager
import android.net.wifi.WifiManager
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 주변 Wi-Fi 목록을 가져온다. 스캔에는 `ACCESS_FINE_LOCATION` 런타임 권한이 필요하고 기기의 위치 서비스도 켜져 있어야 한다
 * (안드로이드 13+에서도 `NEARBY_WIFI_DEVICES`로는 `startScan`/`getScanResults`를 쓸 수 없다). 앱은 위치 자체를 쓰지 않는다.
 * 권한 창은 시스템 UI라 글래스에서 눌러지는지는 확인하지 못했다.
 */
class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val wifi = appContext.getSystemService(WifiManager::class.java)

    /** 한 번에 요청할 권한. 안드로이드 12+는 정밀과 대략을 함께 요청해야 하며, 스캔에 필요한 것은 정밀(FINE)이다. */
    val requiredPermissions: Array<String>
        get() = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val location = appContext.getSystemService(LocationManager::class.java)

    /**
     * 스캔을 시작하고 결과를 기다린다. 시간 안에 결과가 오지 않으면(스캔 횟수 제한 등) 마지막으로 알려진 결과를 쓴다.
     */
    @Suppress("DEPRECATION") // startScan은 deprecated지만 일반 앱이 쓸 수 있는 유일한 스캔 요청이다
    suspend fun scan(): Result<List<WifiNetwork>> {
        if (!hasPermission()) return Result.failure(SecurityException("Wi-Fi 목록을 보려면 권한이 필요합니다"))
        if (!location.isLocationEnabled) {
            return Result.failure(IllegalStateException("기기의 위치 서비스를 켜야 Wi-Fi 목록을 볼 수 있습니다"))
        }
        if (!wifi.isWifiEnabled) return Result.failure(IllegalStateException("Wi-Fi가 꺼져 있습니다. 먼저 켜 주세요"))

        // 스캔 요청·수신 중 시스템이 던지는 예외(권한 누락, 스캔 제한 등)가 앱을 죽이지 않도록 모두 결과로 바꾼다.
        return try {
            withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                    appContext.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                    continuation.invokeOnCancellation { runCatching { appContext.unregisterReceiver(receiver) } }
                    if (!wifi.startScan() && continuation.isActive) continuation.resume(Unit) // 요청이 거절돼도 기존 결과를 쓴다
                }
            }
            Result.success(toNetworks(wifi.scanResults.map { it.toRaw() }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Wi-Fi 목록을 가져오지 못했습니다 (${e.javaClass.simpleName})", e))
        }
    }

    @Suppress("DEPRECATION")
    private fun ScanResult.toRaw() = RawScan(ssid = SSID.orEmpty(), capabilities = capabilities.orEmpty(), rssi = level)

    private companion object {
        const val SCAN_TIMEOUT_MS = 8_000L
    }
}
