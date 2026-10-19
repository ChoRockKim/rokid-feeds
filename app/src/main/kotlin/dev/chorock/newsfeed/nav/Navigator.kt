package dev.chorock.newsfeed.nav

/** 글래스 입력 한 축: 이전·다음·선택·뒤로. (Rokid Nexus의 NEXT/PREV/SELECT/BACK과 같은 모델) */
enum class NavKey { Previous, Next, Select, Back }

/** [count]개 중 [index]에서 [delta]만큼 옮긴 위치. 양끝에서 순환한다. [count]가 0 이하면 [index]를 그대로 돌려준다. */
fun wrapIndex(index: Int, delta: Int, count: Int): Int = if (count <= 0) index else Math.floorMod(index + delta, count)

/**
 * 화면 상태. [openIndex]가 null이면 목록, 아니면 그 기사의 상세이며 [page]는 상세의 쪽 번호(0부터)다.
 */
data class NavState(val selected: Int = 0, val openIndex: Int? = null, val page: Int = 0)

/** [exit]가 true면 앱을 끝낸다(루트에서의 Back). */
data class Reduction(val state: NavState, val exit: Boolean = false)

/**
 * 이 앱의 내비게이션 전체를 담은 순수 상태머신(Android·Compose 의존 없음).
 *
 * - 목록: 이전/다음은 양방향 wrap, 선택은 상세 열기, Back은 종료.
 * - 상세: 이전/다음은 쪽 이동이며 wrap하지 않고 양 끝에서 멈춘다. 선택은 다음 쪽(탭으로도 넘김), Back은 목록으로.
 * - 막다른 길이 없다: 어떤 상태에서도 Back을 최대 두 번 누르면 종료에 닿는다.
 */
class Navigator(
    private val articleCount: Int,
    private val pageCountOf: (Int) -> Int,
) {
    fun reduce(state: NavState, key: NavKey): Reduction {
        val current = normalize(state)
        val open = current.openIndex
        return if (open == null) reduceFeed(current, key) else reduceDetail(current, open, key)
    }

    /** 기사 수가 줄어 범위를 벗어난 상태를 되돌린다. 열린 기사가 사라졌으면 목록으로 돌아간다. */
    fun normalize(state: NavState): NavState {
        if (articleCount <= 0) return NavState()
        val selected = state.selected.coerceIn(0, articleCount - 1)
        val open = state.openIndex
        if (open == null || open !in 0 until articleCount) return NavState(selected = selected)
        val pages = pageCountOf(open).coerceAtLeast(1)
        return NavState(selected = selected, openIndex = open, page = state.page.coerceIn(0, pages - 1))
    }

    private fun reduceFeed(state: NavState, key: NavKey): Reduction = when (key) {
        NavKey.Previous -> Reduction(wrap(state, -1))
        NavKey.Next -> Reduction(wrap(state, 1))
        NavKey.Select ->
            if (articleCount == 0) Reduction(state) else Reduction(state.copy(openIndex = state.selected, page = 0))
        NavKey.Back -> Reduction(state, exit = true)
    }

    private fun reduceDetail(state: NavState, open: Int, key: NavKey): Reduction {
        val last = pageCountOf(open).coerceAtLeast(1) - 1
        return when (key) {
            NavKey.Previous -> Reduction(state.copy(page = (state.page - 1).coerceAtLeast(0)))
            NavKey.Next, NavKey.Select -> Reduction(state.copy(page = (state.page + 1).coerceAtMost(last)))
            NavKey.Back -> Reduction(state.copy(openIndex = null, page = 0))
        }
    }

    private fun wrap(state: NavState, delta: Int): NavState =
        if (articleCount <= 0) state else state.copy(selected = wrapIndex(state.selected, delta, articleCount))
}
