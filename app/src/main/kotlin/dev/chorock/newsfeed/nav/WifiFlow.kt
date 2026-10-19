package dev.chorock.newsfeed.nav

/** Wi-Fi 화면 묶음의 쪽: 홈(연결/선택) → 목록 → 입력 폼. */
enum class WifiPage { Home, Picker, Form }

/** 쪽마다 선택 위치를 따로 기억한다(목록에서 폼으로 갔다 돌아와도 위치가 유지된다). */
data class WifiFlowState(
    val page: WifiPage = WifiPage.Home,
    val homeSelected: Int = 0,
    val pickerSelected: Int = 0,
    val formSelected: Int = 0,
)

/** [selectedActivated]가 null이 아니면 그 쪽의 해당 항목이 눌렸다는 뜻이고, [exit]이면 Wi-Fi 화면 묶음을 떠난다(홈에서의 Back). */
data class WifiFlowResult(
    val state: WifiFlowState,
    val activated: Int? = null,
    val exit: Boolean = false,
)

/**
 * Wi-Fi 화면 묶음의 내비게이션(순수). ←/→는 항목 사이를 순환하고, 선택은 항목을 누르며, Back은 한 쪽씩 되돌아간다.
 * 쪽을 여는 것(목록 열기, 폼 열기)은 항목을 눌렀을 때 UI가 [open]으로 처리한다.
 */
object WifiFlow {
    fun reduce(state: WifiFlowState, key: NavKey, itemCount: Int): WifiFlowResult = when (key) {
        NavKey.Previous -> WifiFlowResult(moved(state, -1, itemCount))
        NavKey.Next -> WifiFlowResult(moved(state, 1, itemCount))
        NavKey.Select -> if (itemCount <= 0) WifiFlowResult(state) else WifiFlowResult(state, activated = selected(state))
        NavKey.Back -> when (state.page) {
            WifiPage.Home -> WifiFlowResult(state, exit = true)
            WifiPage.Picker -> WifiFlowResult(state.copy(page = WifiPage.Home))
            WifiPage.Form -> WifiFlowResult(state.copy(page = WifiPage.Picker))
        }
    }

    fun selected(state: WifiFlowState): Int = when (state.page) {
        WifiPage.Home -> state.homeSelected
        WifiPage.Picker -> state.pickerSelected
        WifiPage.Form -> state.formSelected
    }

    /** [page]를 연다. 폼은 항상 첫 항목에서 시작한다. */
    fun open(state: WifiFlowState, page: WifiPage): WifiFlowState =
        if (page == WifiPage.Form) state.copy(page = page, formSelected = 0) else state.copy(page = page)

    /** 항목 수가 줄어 선택이 범위를 벗어났으면 되돌린다. */
    fun clamp(state: WifiFlowState, itemCount: Int): WifiFlowState {
        if (itemCount <= 0) return state
        val max = itemCount - 1
        return when (state.page) {
            WifiPage.Home -> state.copy(homeSelected = state.homeSelected.coerceIn(0, max))
            WifiPage.Picker -> state.copy(pickerSelected = state.pickerSelected.coerceIn(0, max))
            WifiPage.Form -> state.copy(formSelected = state.formSelected.coerceIn(0, max))
        }
    }

    private fun moved(state: WifiFlowState, delta: Int, itemCount: Int): WifiFlowState {
        if (itemCount <= 0) return state
        val next = wrapIndex(selected(state), delta, itemCount)
        return when (state.page) {
            WifiPage.Home -> state.copy(homeSelected = next)
            WifiPage.Picker -> state.copy(pickerSelected = next)
            WifiPage.Form -> state.copy(formSelected = next)
        }
    }
}
