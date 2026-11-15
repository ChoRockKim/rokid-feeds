package com.chorockkim.rokidfeeds.nav

/** 앱의 큰 화면. 메인 메뉴에서 고른다. */
enum class Screen { Menu, News, Threads, Settings, ThreadsLogin, Insta, InstaLogin }

/** 메인 메뉴 줄. 로그인·로그아웃은 실수로 누르지 않게 설정 안에 둔다. */
enum class MenuItem(val screen: Screen) {
    News(Screen.News),
    Threads(Screen.Threads),
    Instagram(Screen.Insta),
    Settings(Screen.Settings),
}

/** 설정 줄 = Threads 계정 칸([com.chorockkim.rokidfeeds.threads.ThreadsAccounts], 줄 번호 = 계정 번호). */
data class AccountRow(
    val loggedIn: Boolean,
    val active: Boolean,
    /** 안경 WebView가 프로필을 지원하지 않으면 계정 2는 쓸 수 없다. */
    val available: Boolean = true,
    /** 로그인할 때 넣은 아이디(표시용). */
    val name: String? = null,
)

/**
 * 앱 전체 상태: 지금 화면, 메뉴·설정에서 고른 줄, 로그아웃 확인 중인지. 뉴스·Threads 안의 상태는 각자 갖는다.
 */
data class AppState(
    val screen: Screen = Screen.Menu,
    val menuSelected: Int = 0,
    val settingsSelected: Int = 0,
    val confirmLogout: Boolean = false,
)

/** [AppNav]의 결과. [exit]면 앱을 끝내고, [logout]이면 지금 계정에서 로그아웃, [switchTo]면 그 계정으로 바꾼다. */
data class Reduction2(val state: AppState, val exit: Boolean = false, val logout: Boolean = false, val switchTo: Int? = null)

/**
 * 메인 메뉴·설정의 순수 상태머신.
 * - 메뉴: 이전/다음 = 줄 이동(양끝 순환), 선택 = 그 화면, 뒤로 = 종료.
 * - 설정: 계정 줄마다 — 로그인 안 한 계정 = 그 계정으로 바꾸고 로그인 화면, 로그인한 다른 계정 = 그 계정으로 바꾸기,
 *   지금 계정 = 선택 한 번 "한 번 더 탭하면 로그아웃", 한 번 더 = 로그아웃(쓸기·뒤로는 확인 취소). 뒤로 = 메뉴.
 * - 뉴스·Threads는 자기 목록에서 뒤로 하면 앱을 끝낸다(AppRoot, 두 번 뒤로 하지 않게), 로그인 화면에서 뒤로 하면 설정으로([backFromLogin]).
 */
object AppNav {
    fun reduceMenu(state: AppState, key: NavKey): Reduction2 {
        val count = MenuItem.entries.size
        return when (key) {
            NavKey.Previous -> Reduction2(state.copy(menuSelected = wrapIndex(state.menuSelected, -1, count)))
            NavKey.Next -> Reduction2(state.copy(menuSelected = wrapIndex(state.menuSelected, 1, count)))
            NavKey.Select -> Reduction2(state.copy(screen = MenuItem.entries[state.menuSelected.coerceIn(0, count - 1)].screen))
            NavKey.Back -> Reduction2(state, exit = true)
        }
    }

    fun reduceSettings(state: AppState, key: NavKey, accounts: List<AccountRow>): Reduction2 {
        val count = accounts.size.coerceAtLeast(1)
        return when (key) {
            NavKey.Previous -> Reduction2(state.copy(settingsSelected = wrapIndex(state.settingsSelected, -1, count), confirmLogout = false))
            NavKey.Next -> Reduction2(state.copy(settingsSelected = wrapIndex(state.settingsSelected, 1, count), confirmLogout = false))
            NavKey.Select -> {
                val i = state.settingsSelected.coerceIn(0, count - 1)
                val row = accounts.getOrNull(i) ?: return Reduction2(state)
                when {
                    !row.available -> Reduction2(state)
                    !row.loggedIn -> Reduction2(state.copy(screen = Screen.ThreadsLogin, confirmLogout = false), switchTo = i.takeIf { !row.active })
                    !row.active -> Reduction2(state.copy(confirmLogout = false), switchTo = i)
                    state.confirmLogout -> Reduction2(state.copy(confirmLogout = false), logout = true)
                    else -> Reduction2(state.copy(confirmLogout = true))
                }
            }
            NavKey.Back -> Reduction2(state.copy(screen = Screen.Menu, confirmLogout = false))
        }
    }

    fun backToMenu(state: AppState): AppState = state.copy(screen = Screen.Menu)

    /** 로그인 화면에서 뒤로: Threads 로그인은 설정에서, 인스타 로그인은 스토리 화면에서 왔다. */
    fun backFromLogin(state: AppState): AppState =
        state.copy(screen = if (state.screen == Screen.InstaLogin) Screen.Insta else Screen.Settings)
}
