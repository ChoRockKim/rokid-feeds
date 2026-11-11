package com.chorockkim.rokidfeeds.nav

/** 앱의 큰 화면. 메인 메뉴에서 고른다. */
enum class Screen { Menu, News, Threads, ThreadsLogin }

/** 메인 메뉴 줄. [Screen]과 같은 순서로 보여 준다. */
enum class MenuItem(val screen: Screen) {
    News(Screen.News),
    Threads(Screen.Threads),
    ThreadsLogin(Screen.ThreadsLogin),
}

/** 앱 전체 상태: 지금 화면과 메뉴에서 고른 줄. 뉴스·Threads 안의 상태는 각자 [Navigator]가 갖는다. */
data class AppState(val screen: Screen = Screen.Menu, val menuSelected: Int = 0)

/**
 * 메인 메뉴의 순수 상태머신. 메뉴: 이전/다음 = 줄 이동(양끝 순환), 선택 = 그 화면, 뒤로 = 종료.
 * 뉴스·Threads 화면은 자기 [Navigator]가 "종료"를 돌려주면(목록에서 뒤로) 메뉴로 돌아온다([backToMenu]).
 * 로그인 화면의 뒤로는 메뉴로. 어떤 상태에서도 뒤로를 반복하면 메뉴를 거쳐 종료에 닿는다.
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

    fun backToMenu(state: AppState): AppState = state.copy(screen = Screen.Menu)
}

/** [AppNav]의 결과. [exit]면 앱을 끝낸다. */
data class Reduction2(val state: AppState, val exit: Boolean = false)
