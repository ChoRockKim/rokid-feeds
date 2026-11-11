package com.chorockkim.rokidfeeds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.chorockkim.rokidfeeds.data.NewsApi
import com.chorockkim.rokidfeeds.input.GlassesInput
import com.chorockkim.rokidfeeds.input.RingTarget
import com.chorockkim.rokidfeeds.input.toNavKey
import com.chorockkim.rokidfeeds.nav.NavKey
import com.chorockkim.rokidfeeds.net.WifiEnabler
import com.chorockkim.rokidfeeds.ui.GlassScreen
import com.chorockkim.rokidfeeds.ui.Glass
import com.chorockkim.rokidfeeds.nav.Screen
import com.chorockkim.rokidfeeds.threads.ThreadsSession
import com.chorockkim.rokidfeeds.threads.ThreadsWeb
import com.chorockkim.rokidfeeds.ui.AppRoot
import com.chorockkim.rokidfeeds.ui.StatusBar
import com.chorockkim.rokidfeeds.update.InstallResultReceiver
import com.chorockkim.rokidfeeds.update.Updater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 하나뿐인 Activity. 다른 Rokid 앱(rokid-youtube·rokid-lens)과 같은 틀이다:
 * - 화면: 480×282 띠를 화면 아래 가운데에([attachBaseContext]의 밀도 조정 + [GlassScreen]), 위는 검정(투명).
 * - 입력: 터치패드 키는 [GlassesInput]으로, R08 링은 띠 위의 노드 하나 [RingTarget]으로 받아 [NavKey]로 바꾼다.
 * - 네트워크: [WifiEnabler]가 Wi-Fi를 켜 두고 고르기는 시스템에 맡긴다. 상태는 띠 위 알약으로만 보여 준다.
 * - 업데이트: 켤 때마다 OTA 서버를 확인해 새 버전이면 받아서 확인 창 없이 설치한다([Updater]).
 */
class MainActivity : ComponentActivity() {
    private val app get() = application as RokidFeedsApp
    private val input = GlassesInput()
    private val keys = Channel<NavKey>(Channel.UNLIMITED)

    private var online by mutableStateOf(true)
    private var wifiStatus by mutableStateOf<WifiEnabler.Status?>(null)
    private var updateText by mutableStateOf<String?>(null)
    private var crashText by mutableStateOf<String?>(null)
    private var clock by mutableStateOf("")
    private var battery by mutableStateOf<Int?>(null)
    private var charging by mutableStateOf(false)

    /** 지금 화면(로그인 화면이면 키를 웹으로 넘긴다). */
    private var screen = Screen.Menu
    private var threadsLoggedIn by mutableStateOf(ThreadsWeb.isLoggedIn())
    private var loginFocus by mutableStateOf<String?>(null)
    private var threadsWeb: ThreadsWeb? = null
    /** Threads를 처음 고를 때 만든다(WebView). 화면은 이 값이 생기면 그린다. */
    private var threadsSession by mutableStateOf<ThreadsSession?>(null)
    private lateinit var root: FrameLayout
    private lateinit var ringTarget: RingTarget

    private val wifiEnabler by lazy { WifiEnabler(this) { status -> runOnUiThread { wifiStatus = status } } }

    /** rokid-lens와 같다: 480×282 띠가 실제 화면에 꽉 차게 밀도를 맞추고 글꼴 배율은 무시한다. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        val metrics = newBase.resources.displayMetrics
        val (w, h) = realScreenSize(newBase, metrics.widthPixels, metrics.heightPixels)
        val scale = minOf(w / Glass.WIDTH_DP.toFloat(), h / Glass.HEIGHT_DP.toFloat())
        applyOverrideConfiguration(Configuration().apply {
            densityDpi = (scale * DisplayMetrics.DENSITY_DEFAULT).toInt().coerceAtLeast(1)
            fontScale = 1f
        })
    }

    private fun realScreenSize(context: Context, fallbackW: Int, fallbackH: Int): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java) ?: return fallbackW to fallbackH
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 읽는 동안 화면이 꺼지지 않게 한다. 앱이 보이는 동안만 적용된다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 미리보기용: `adb shell am start -n com.chorockkim.rokidfeeds/.MainActivity --es feed geeknews`면 GeekNews만.
        val api = if (intent.getStringExtra("feed") == "geeknews") NewsApi(sources = listOf(NewsApi.GEEKNEWS)) else NewsApi()
        val keyFlow = keys.receiveAsFlow()

        val compose = ComposeView(this).apply {
            // 접근성에는 아래 RingTarget 하나만 보인다(R08 브리지가 노드를 하나만 고르도록).
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setContent {
                GlassScreen(StatusBar(updateText, notice(), clock, battery, charging)) {
                    AppRoot(
                        keys = keyFlow,
                        online = online,
                        api = api,
                        threads = threadsSession,
                        threadsLoggedIn = threadsLoggedIn,
                        onScreen = ::onScreen,
                        onLogout = ::logout,
                        onExit = { finish() },
                        loginFocus = loginFocus,
                    )
                }
            }
        }
        ringTarget = RingTarget(this) { action ->
            if (screen == Screen.ThreadsLogin) loginAction(action) else action.toNavKey()?.let(keys::trySend)
        }
        ringTarget.keepFocus = { screen != Screen.ThreadsLogin }
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(compose, FrameLayout.LayoutParams(-1, -1))
            addView(ringTarget, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(root)
        ringTarget.requestFocus()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                keys.trySend(NavKey.Back)
            }
        })

        app.takeLastCrash()?.let { reason ->
            crashText = "지난번 종료 이유: $reason"
            lifecycleScope.launch { delay(CRASH_NOTICE_MS); crashText = null }
        }
        watchNetwork()
        checkForUpdate()
    }

    /** 가운데 알약: Wi-Fi·인터넷 상태가 우선이고, 없으면 지난번 튕긴 이유. */
    private fun notice(): String? = WifiEnabler.label(wifiStatus, online) ?: crashText

    override fun onStart() {
        super.onStart()
        if (waitingForInstallPermission && app.updater.canInstall()) {
            waitingForInstallPermission = false
            checkForUpdate()
        }
        wifiEnabler.start()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        renderClock()
        // 배터리는 sticky 방송이라 등록하자마자 현재 값이 돌아온다.
        registerStatusReceiver(filter)?.let(::renderBattery)
    }

    override fun onStop() {
        wifiEnabler.stop()
        runCatching { unregisterReceiver(statusReceiver) }
        super.onStop()
    }

    override fun onDestroy() {
        threadsWeb?.destroy()
        networkCallback?.let { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        super.onDestroy()
    }

    // region 입력

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // 뒤로는 시스템 Back으로 받아 onBackPressedDispatcher가 넘긴다.
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        if (screen == Screen.ThreadsLogin) return dispatchLoginKey(event)
        val action = input.map(event)
        if (action == GlassesInput.Action.DUPLICATE) return true
        val key = action.toNavKey() ?: return super.dispatchKeyEvent(event)
        keys.trySend(key)
        return true
    }

    /**
     * 로그인 화면: 쓸기(방향 키·183/184·링) = 입력칸·버튼 이동, 탭(ENTER·DPAD 가운데·202·링) = 누르기.
     * 그 밖의 키(글자·스페이스 등 폰 키보드가 보내는 것)는 포커스된 웹 입력칸으로 그대로 보낸다.
     */
    private fun dispatchLoginKey(event: KeyEvent): Boolean {
        if (threadsWeb == null || event.keyCode !in LOGIN_NAV_KEYS) return super.dispatchKeyEvent(event)
        loginAction(input.map(event))
        return true
    }

    private fun loginAction(action: GlassesInput.Action) {
        val web = threadsWeb ?: return
        when (action) {
            GlassesInput.Action.PREVIOUS, GlassesInput.Action.UP -> web.focus(-1) { loginFocus = it }
            GlassesInput.Action.NEXT, GlassesInput.Action.DOWN -> web.focus(1) { loginFocus = it }
            GlassesInput.Action.SELECT -> web.click()
            GlassesInput.Action.BACK -> keys.trySend(NavKey.Back)
            else -> Unit
        }
    }

    // endregion

    // region Threads (WebView는 처음 Threads를 고를 때 만든다)

    private fun threads(): ThreadsSession {
        threadsSession?.let { return it }
        lateinit var session: ThreadsSession
        val web = ThreadsWeb(this) { source, url, json -> session.onJson(source, url, json) }
        session = ThreadsSession(web, lifecycleScope)
        // 보통은 띠 안에, Compose 화면 **뒤**에 둬서(검정으로 가려짐) 페이지가 보이는 것처럼 계속 돌게 하고
        // (숨김 상태면 무한 스크롤이 멈출 수 있다), 로그인할 때만 화면 세로 전체로 늘려 앞으로 꺼낸다(translationZ).
        listOf(web.feed, web.post).forEach { view ->
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            root.addView(view, 0, bandParams())
        }
        threadsWeb = web
        threadsSession = session
        return session
    }

    /** 평소 WebView 자리: 띠 안(안내 줄 위). */
    private fun bandParams(): FrameLayout.LayoutParams {
        val density = resources.displayMetrics.density
        return FrameLayout.LayoutParams(
            ((Glass.WIDTH_DP - 2) * density).toInt(),
            ((Glass.HEIGHT_DP - LOGIN_HINT_DP) * density).toInt(),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply { bottomMargin = (LOGIN_HINT_DP * density).toInt() }
    }

    /**
     * 로그인할 때 WebView 자리: 화면 세로 전체(맨 아래 안내 한 줄만 남김). 띠 높이(282)로는 로그인 페이지의 창·팝업이
     * 입력칸을 가려 로그인할 수 없었다.
     */
    private fun loginParams(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            bottomMargin = (LOGIN_HINT_DP * resources.displayMetrics.density).toInt()
        }

    private fun onScreen(next: Screen) {
        val previous = screen
        screen = next
        val web = threadsWeb
        val threadsScreen = next == Screen.Threads || next == Screen.ThreadsLogin
        if (threadsScreen) threads()
        threadsWeb?.let { w ->
            // Threads 밖에서는 WebView를 쉬게 한다(배터리).
            listOf(w.feed, w.post).forEach { if (threadsScreen) it.onResume() else it.onPause() }
        }
        if (next == Screen.ThreadsLogin) {
            val feed = threadsWeb!!.feed
            loginFocus = null
            threadsWeb!!.openLogin()
            feed.layoutParams = loginParams()
            feed.translationZ = LOGIN_Z
            feed.requestFocus() // 폰 키보드 글자가 웹 입력칸으로 가도록
        } else if (web != null) {
            if (web.feed.translationZ != 0f) web.feed.layoutParams = bandParams()
            web.feed.translationZ = 0f
            ringTarget.requestFocus()
        }
        if (previous == Screen.ThreadsLogin && next != Screen.ThreadsLogin) {
            threadsLoggedIn = ThreadsWeb.isLoggedIn()
            threadsSession?.afterLogin()
        }
    }

    private fun logout() {
        val session = threadsSession
        if (session != null) session.logout() else ThreadsWeb.logout {}
        threadsLoggedIn = false
    }

    // endregion

    // region 시각·배터리 (띠 위 오른쪽)

    private val clockFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) renderBattery(intent) else renderClock()
        }
    }

    private fun registerStatusReceiver(filter: IntentFilter): Intent? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(statusReceiver, filter)
    }.getOrNull()

    private fun renderClock() {
        clock = clockFormat.format(Date())
    }

    private fun renderBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level >= 0) battery = level * 100 / scale
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    // endregion

    // region 업데이트 (rokid-lens와 같음: 켤 때마다 자동)

    private var waitingForInstallPermission = false
    private var askedInstallPermission = false

    /** 설치 권한이 없으면 "이 출처 허용" 화면을 앱 실행당 한 번만 연다. 허용하고 돌아오면 이어서 한다. */
    private fun askInstallPermission(): Boolean {
        if (app.updater.canInstall()) return true
        if (!askedInstallPermission) {
            askedInstallPermission = true
            waitingForInstallPermission = runCatching { startActivity(app.updater.installPermissionIntent()) }.isSuccess
        }
        return false
    }

    private fun checkForUpdate() {
        val updater = app.updater
        lifecycleScope.launch {
            val release = (runCatching { updater.check() }.getOrNull() as? Updater.Check.Available)?.release
            updateText = release?.let { "새 버전 v${it.versionName}" }
            if (release == null) return@launch
            if (!askInstallPermission()) {
                updateText = "새 버전 v${release.versionName} · 설치 허용을 켜면 이어서 해요"
                return@launch
            }
            updateText = "업데이트 v${release.versionName} 받는 중"
            val apk = runCatching {
                updater.download(release) { percent ->
                    if (percent >= 0) updateText = "업데이트 v${release.versionName} 받는 중 $percent%"
                }
            }.getOrElse {
                updateText = "업데이트 실패 · ${it.message ?: "다시 켜면 다시 해요"}"
                return@launch
            }
            InstallResultReceiver.listener = { result ->
                runOnUiThread {
                    updateText = when (result) {
                        is InstallResultReceiver.Result.Failed -> "업데이트 실패 · ${result.message}"
                        InstallResultReceiver.Result.WaitingForUser -> "업데이트 v${release.versionName} · 설치 확인을 눌러 주세요"
                        InstallResultReceiver.Result.Installed -> "업데이트 v${release.versionName} 설치 완료 · 다시 켜는 중"
                    }
                }
            }
            updateText = "업데이트 v${release.versionName} 설치 중 · 곧 다시 켜져요"
            runCatching { updater.install(apk) }.onFailure {
                updateText = "업데이트 실패 · ${it.message ?: it.javaClass.simpleName}"
            }
        }
    }

    // endregion

    // region 네트워크 (rokid-lens와 같음)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        fun hasInternet(caps: NetworkCapabilities?) = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        fun show(isOnline: Boolean) = runOnUiThread { online = isOnline }
        fun refresh() = show(hasInternet(cm.getNetworkCapabilities(cm.activeNetwork)))
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            // 잃은 직후에는 activeNetwork가 아직 그 네트워크일 수 있어 바로 오프라인으로 본다.
            override fun onLost(network: Network) = show(false)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refresh()
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }.onSuccess { networkCallback = callback }
        refresh()
    }

    // endregion

    private companion object {
        const val CRASH_NOTICE_MS = 15_000L
        /** 로그인 화면에서 웹 아래에 남겨 두는 안내 줄 높이. */
        const val LOGIN_HINT_DP = 30
        /** 로그인 화면에서 웹을 Compose 화면 위로 꺼내는 높이. */
        const val LOGIN_Z = 10f
        val LOGIN_NAV_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            GlassesInput.KEYCODE_ROKID_NEXT, GlassesInput.KEYCODE_ROKID_PREVIOUS, GlassesInput.KEYCODE_ROKID_DOUBLE_TAP,
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        )
    }
}
