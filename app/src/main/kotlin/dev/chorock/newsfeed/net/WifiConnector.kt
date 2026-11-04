package dev.chorock.newsfeed.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 지정한 Wi-Fi에 연결을 요청한다([WifiNetworkSpecifier], API 29+).
 * 연결된 네트워크는 이 앱 프로세스에만 묶인다(시스템 전체 Wi-Fi 설정은 바꾸지 않는다).
 * 시스템이 처음 연결을 허용할지 묻는 창을 띄울 수 있다.
 */
class WifiConnector(context: Context) {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null

    /** 연결 요청을 보냈지만 아직 결과(성공/실패)를 받지 못한 상태. */
    private var pending = false

    /** 인터넷이 실제로 되는 네트워크(검증됨)가 있으면 true. */
    fun isOnline(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Wi-Fi 라디오가 켜져 있으면 true. 앱은 Android 10부터 Wi-Fi를 직접 켜고 끌 수 없어 상태만 읽는다. */
    fun isWifiEnabled(): Boolean = wifi.isWifiEnabled

    suspend fun connect(config: WifiConfig): Result<Unit> {
        if (!config.isConfigured) {
            return Result.failure(IllegalStateException("Wi-Fi 정보가 설정되지 않았습니다"))
        }
        validatePassphrase(config.effectiveSecurity, config.password)?.let {
            return Result.failure(IllegalStateException(it))
        }
        if (!wifi.isWifiEnabled) {
            return Result.failure(IllegalStateException("Wi-Fi가 꺼져 있습니다. 먼저 켜 주세요"))
        }
        return suspendCancellableCoroutine { continuation ->
            release()
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(config.ssid)
                .apply {
                    when (config.effectiveSecurity) {
                        WifiSecurity.Wpa2 -> setWpa2Passphrase(config.password)
                        WifiSecurity.Wpa3 -> setWpa3Passphrase(config.password)
                        WifiSecurity.Owe -> setIsEnhancedOpen(true)
                        WifiSecurity.Open, WifiSecurity.Unsupported -> Unit
                    }
                }
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    pending = false
                    connectivity.bindProcessToNetwork(network)
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }

                override fun onUnavailable() {
                    pending = false
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException("${config.ssid}에 연결하지 못했습니다")))
                    }
                }

                override fun onLost(network: Network) {
                    connectivity.bindProcessToNetwork(null)
                }
            }
            callback = networkCallback
            pending = true
            connectivity.requestNetwork(request, networkCallback, CONNECT_TIMEOUT_MS)
            continuation.invokeOnCancellation { release() }
        }
    }

    /**
     * 결과를 못 받은 연결 요청이 있으면 취소한다(시스템이 먼저 연결했을 때 확인 창이 남지 않게).
     * 이미 연결된 요청은 그대로 둔다. 취소하면 앱이 그 네트워크에서 떨어진다.
     */
    fun cancelPending() {
        if (pending) release()
    }

    fun release() {
        pending = false
        callback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }
        callback = null
        connectivity.bindProcessToNetwork(null)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
    }
}
