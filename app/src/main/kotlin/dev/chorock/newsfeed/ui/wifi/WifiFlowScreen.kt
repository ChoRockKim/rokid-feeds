package dev.chorock.newsfeed.ui.wifi

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import dev.chorock.newsfeed.nav.AUTO_RETRY_FIRST_DELAY_MS
import dev.chorock.newsfeed.nav.AUTO_RETRY_INTERVAL_MS
import dev.chorock.newsfeed.nav.AUTO_RETRY_MAX
import dev.chorock.newsfeed.nav.shouldAutoRetry
import dev.chorock.newsfeed.nav.FormItem
import dev.chorock.newsfeed.nav.NavKey
import dev.chorock.newsfeed.nav.PickerItem
import dev.chorock.newsfeed.nav.WifiFlow
import dev.chorock.newsfeed.nav.WifiFlowState
import dev.chorock.newsfeed.nav.WifiPage
import dev.chorock.newsfeed.nav.formError
import dev.chorock.newsfeed.nav.formItems
import dev.chorock.newsfeed.nav.nextManualSecurity
import dev.chorock.newsfeed.nav.pickerItems
import dev.chorock.newsfeed.net.WifiConfig
import dev.chorock.newsfeed.net.WifiNetwork
import dev.chorock.newsfeed.net.WifiScanner
import dev.chorock.newsfeed.net.WifiSecurity
import dev.chorock.newsfeed.ui.navKeyOf

sealed interface WifiState {
    data object Idle : WifiState
    data object Connecting : WifiState
    data class Failed(val message: String) : WifiState
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data object NeedsPermission : ScanState
    data class Failed(val message: String) : ScanState
}

private const val HOME_ITEM_COUNT = 2
private const val WIFI_STATE_POLL_MS = 1_500L

private val WifiFlowStateSaver = listSaver<WifiFlowState, Int>(
    save = { listOf(it.page.ordinal, it.homeSelected, it.pickerSelected, it.formSelected) },
    restore = { WifiFlowState(WifiPage.entries[it[0]], it[1], it[2], it[3]) },
)

/**
 * 인터넷이 없을 때의 Wi-Fi 화면 묶음: 홈 → 네트워크 목록 → 입력 폼. 모두 ←/→/Enter/Back만으로 조작한다.
 * 연결은 [onConnect]로 넘기고(두 번째 인자가 true면 사용자가 직접 고른 네트워크라 성공하면 저장한다),
 * 결과는 [state]로 돌려받는다.
 */
@Composable
fun WifiFlowScreen(
    active: WifiConfig?,
    scanner: WifiScanner,
    state: WifiState,
    onConnect: (WifiConfig, Boolean) -> Unit,
    isWifiEnabled: () -> Boolean,
    onEnableWifi: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onExit: () -> Unit,
) {
    // Wi-Fi 라디오 상태. 사용자가 시스템 패널에서 켜고 돌아오면 바로 반영하려고 가끔 읽는다.
    var wifiOn by remember { mutableStateOf(isWifiEnabled()) }
    LaunchedEffect(Unit) {
        while (true) {
            wifiOn = isWifiEnabled()
            delay(WIFI_STATE_POLL_MS)
        }
    }
    // 저장된 네트워크로 연결을 자동으로 다시 시도한 횟수. Wi-Fi를 껐다 켜거나 버튼을 누르면 0으로 돌아간다.
    var autoAttempts by remember { mutableIntStateOf(0) }
    LaunchedEffect(wifiOn) { if (!wifiOn) autoAttempts = 0 }

    var flow by rememberSaveable(stateSaver = WifiFlowStateSaver) { mutableStateOf(WifiFlowState()) }
    var networks by remember { mutableStateOf<List<WifiNetwork>>(emptyList()) }
    var scan by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    var rescan by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }

    var manual by rememberSaveable { mutableStateOf(false) }
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var security by rememberSaveable { mutableStateOf(WifiSecurity.Wpa2) }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val rootFocus = remember { FocusRequester() }
    val ssidFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (scanner.hasPermission()) rescan++ else scan = ScanState.NeedsPermission
    }

    val pickerItems = remember(networks, scan) { pickerItems(networks, needsPermission = scan is ScanState.NeedsPermission) }
    val formItems = remember(manual, security) { formItems(manual, security) }
    val itemCount = when (flow.page) {
        WifiPage.Home -> HOME_ITEM_COUNT
        WifiPage.Picker -> pickerItems.size
        WifiPage.Form -> formItems.size
    }

    // 목록을 열면 스캔한다. 권한이 없으면 권한 줄을 보여 준다.
    LaunchedEffect(flow.page == WifiPage.Picker, rescan) {
        if (flow.page != WifiPage.Picker) return@LaunchedEffect
        if (!scanner.hasPermission()) {
            networks = emptyList()
            scan = ScanState.NeedsPermission
            return@LaunchedEffect
        }
        scan = ScanState.Scanning
        scanner.scan().fold(
            onSuccess = { networks = it; scan = ScanState.Idle },
            onFailure = { scan = ScanState.Failed(it.message ?: "Wi-Fi 목록을 가져오지 못했습니다") },
        )
    }

    LaunchedEffect(itemCount, flow.page) { flow = WifiFlow.clamp(flow, itemCount) }

    // 입력칸을 고르면 그 칸에 실제 포커스를 줘서 폰 앱의 글자를 받게 한다. 다른 줄이면 루트가 포커스를 가져 ←/→를 받는다.
    val selectedForm = formItems.getOrNull(flow.formSelected)
    // 링이 줄에 직접 포커스를 줘서 선택이 바뀐 경우([focusDriven])엔 그 포커스를 빼앗지 않는다.
    var focusDriven by remember { mutableStateOf(false) }
    LaunchedEffect(flow.page, flow.formSelected, formItems) {
        if (focusDriven && selectedForm != FormItem.Ssid && selectedForm != FormItem.Password) {
            focusDriven = false
            keyboard?.hide()
            return@LaunchedEffect
        }
        focusDriven = false
        val target = when {
            flow.page != WifiPage.Form -> rootFocus
            selectedForm == FormItem.Ssid -> ssidFocus
            selectedForm == FormItem.Password -> passwordFocus
            else -> rootFocus
        }
        runCatching { target.requestFocus() }
        if (target === rootFocus) keyboard?.hide() else keyboard?.show()
    }

    val hasSaved = active?.isConfigured == true
    LaunchedEffect(wifiOn, hasSaved, flow.page, state, autoAttempts) {
        if (!shouldAutoRetry(wifiOn, hasSaved, flow.page == WifiPage.Home, state is WifiState.Connecting, autoAttempts)) {
            return@LaunchedEffect
        }
        delay(if (autoAttempts == 0) AUTO_RETRY_FIRST_DELAY_MS else AUTO_RETRY_INTERVAL_MS)
        autoAttempts++
        active?.let { onConnect(it, false) }
    }

    fun submit() {
        val error = formError(manual, ssid, security, password)
        formError = error
        if (error != null) return
        val name = if (manual) ssid.trim() else ssid
        onConnect(WifiConfig(ssid = name, password = if (security.needsPassword) password else "", security = security), true)
    }

    fun openForm(name: String, wifiSecurity: WifiSecurity, isManual: Boolean) {
        manual = isManual
        ssid = name
        security = wifiSecurity
        password = ""
        reveal = false
        formError = null
        flow = WifiFlow.open(flow, WifiPage.Form)
    }

    fun activate(index: Int) {
        when (flow.page) {
            WifiPage.Home -> when (index) {
                0 -> if (!wifiOn) onEnableWifi() else {
                    autoAttempts = 0
                    active?.takeIf { it.isConfigured }?.let { onConnect(it, false) }
                }
                else -> { notice = null; flow = WifiFlow.open(flow, WifiPage.Picker) }
            }
            WifiPage.Picker -> when (val item = pickerItems.getOrNull(index)) {
                PickerItem.Permission -> permission.launch(scanner.requiredPermissions)
                is PickerItem.Network -> {
                    val network = item.network
                    when {
                        !network.security.isSupported -> notice = "'${network.ssid}'은(는) 지원하지 않는 보안 방식입니다"
                        network.security.needsPassword -> openForm(network.ssid, network.security, isManual = false)
                        else -> onConnect(WifiConfig(network.ssid, "", network.security), true)
                    }
                }
                PickerItem.Manual -> openForm("", WifiSecurity.Wpa2, isManual = true)
                PickerItem.Rescan -> rescan++
                PickerItem.SystemSettings -> onOpenSystemSettings()
                null -> Unit
            }
            WifiPage.Form -> when (formItems.getOrNull(index)) {
                FormItem.Ssid -> flow = flow.copy(formSelected = (index + 1).coerceAtMost(formItems.lastIndex))
                FormItem.Security -> { security = nextManualSecurity(security); formError = null }
                FormItem.Password -> submit()
                FormItem.Reveal -> reveal = !reveal
                FormItem.Connect -> submit()
                FormItem.Cancel -> flow = WifiFlow.reduce(flow, NavKey.Back, itemCount).state
                null -> Unit
            }
        }
    }

    fun dispatch(key: NavKey) {
        val result = WifiFlow.reduce(flow, key, itemCount)
        flow = result.state
        if (key != NavKey.Select) notice = null
        if (result.exit) onExit()
        result.activated?.let { activate(it) }
    }

    fun focused(index: Int) {
        if (WifiFlow.selected(flow) == index) return
        focusDriven = true
        flow = when (flow.page) {
            WifiPage.Home -> flow.copy(homeSelected = index)
            WifiPage.Picker -> flow.copy(pickerSelected = index)
            WifiPage.Form -> flow.copy(formSelected = index)
        }
    }

    fun tap(index: Int) {
        flow = when (flow.page) {
            WifiPage.Home -> flow.copy(homeSelected = index)
            WifiPage.Picker -> flow.copy(pickerSelected = index)
            WifiPage.Form -> flow.copy(formSelected = index)
        }
        activate(index)
    }

    BackHandler { dispatch(NavKey.Back) }

    // 입력칸이 포커스를 가져도 ←/→/Enter는 여기서 먼저 가로채 항목 이동에 쓴다(비밀번호 칸에서 커서를 옮길 일이 없다).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event -> navKeyOf(event)?.let { dispatch(it); true } ?: false }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        when (flow.page) {
            WifiPage.Home -> WifiHomePage(
                active, state, flow.homeSelected, wifiOn,
                autoNote = if (wifiOn && hasSaved && autoAttempts in 1 until AUTO_RETRY_MAX) "자동 재시도 중 ($autoAttempts/$AUTO_RETRY_MAX)" else null,
                onClick = ::tap, onFocused = ::focused,
            )
            WifiPage.Picker -> WifiPickerPage(pickerItems, flow.pickerSelected, scan, state, notice, onClick = ::tap, onFocused = ::focused)
            WifiPage.Form -> WifiFormPage(
                title = if (manual) "직접 입력" else ssid,
                items = formItems,
                selected = flow.formSelected,
                manual = manual,
                ssid = ssid,
                onSsid = { ssid = it; formError = null },
                security = security,
                password = password,
                onPassword = { password = it; formError = null },
                reveal = reveal,
                ssidFocus = ssidFocus,
                passwordFocus = passwordFocus,
                state = state,
                error = formError,
                onClick = ::tap,
                onFocused = ::focused,
                onSubmit = ::submit,
                onNextField = { activate(flow.formSelected) },
            )
        }
    }
}
