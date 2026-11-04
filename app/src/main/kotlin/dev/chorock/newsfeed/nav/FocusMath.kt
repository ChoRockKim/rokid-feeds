package dev.chorock.newsfeed.nav

/**
 * 보이는 항목([visible]) 중 [selected]에 가장 가까운 것. 거리가 같으면 앞쪽(작은 번호)이다. 비어 있으면 null.
 * 링이 목록을 스크롤해 포커스된 행이 화면에서 사라졌을 때, 맨 위로 튀지 않고 가장 가까운 행으로 포커스를 되돌리는 데 쓴다.
 */
fun nearestVisible(selected: Int, visible: List<Int>): Int? =
    visible.minWithOrNull(compareBy<Int> { kotlin.math.abs(it - selected) }.thenBy { it })

/**
 * 접근성의 스크롤 동작(`ACTION_SCROLL_FORWARD/BACKWARD`)을 상세의 쪽 이동으로 바꾼다.
 * 앞으로 스크롤(양수)은 다음 쪽, 뒤로(음수)는 이전 쪽이다. 움직임이 없으면 null.
 */
fun pageKeyForScroll(x: Float, y: Float): NavKey? {
    val delta = if (y != 0f) y else x
    return when {
        delta > 0f -> NavKey.Next
        delta < 0f -> NavKey.Previous
        else -> null
    }
}
