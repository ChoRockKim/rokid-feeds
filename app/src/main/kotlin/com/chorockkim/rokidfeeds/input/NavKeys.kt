package com.chorockkim.rokidfeeds.input

import com.chorockkim.rokidfeeds.nav.NavKey

/**
 * 안경 입력 → 이 앱의 한 축 내비게이션. 다른 Rokid 앱과 같다: 앞/뒤 쓸기·링 앞/뒤 = 다음/이전,
 * 세로 쓸기(DPAD 위/아래)도 이전/다음으로 받는다(rokid-lens의 "처리하지 않는 화면은 PREVIOUS/NEXT로").
 * 탭·두 번 탭(202)·링 탭 = 선택, 뒤로 = 뒤로. 매핑은 이 함수 한 곳에만 둔다.
 */
fun GlassesInput.Action.toNavKey(): NavKey? = when (this) {
    GlassesInput.Action.PREVIOUS, GlassesInput.Action.UP -> NavKey.Previous
    GlassesInput.Action.NEXT, GlassesInput.Action.DOWN -> NavKey.Next
    GlassesInput.Action.SELECT -> NavKey.Select
    GlassesInput.Action.BACK -> NavKey.Back
    GlassesInput.Action.NONE, GlassesInput.Action.DUPLICATE,
    GlassesInput.Action.VOLUME_UP, GlassesInput.Action.VOLUME_DOWN -> null
}
