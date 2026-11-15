package com.chorockkim.rokidfeeds.nav

/**
 * 인스타 스토리 이동(순수, Android 없음).
 * - 목록: 이전/다음 = 사람(양끝 순환), 탭 = 그 사람 스토리 보기, 뒤로 = 앱 종료(피드에서 뒤로 = 종료, 다른 화면과 같음).
 * - 보기: 다음 = 다음 장 → 그 사람 끝이면 다음 사람 → 마지막 사람 끝이면 목록. 이전 = 이전 장(첫 장이면 이전 사람 첫 장).
 *   탭 = 일시정지/계속, 뒤로 = 목록(고른 사람 그대로). 사진은 시간이 지나면, 동영상은 끝나면 [advance]로 저절로 넘어간다.
 * [itemsOf]는 그 사람 장 수, 아직 안 받았으면 null(그 자리에서 기다린다).
 */
data class StoryState(val viewing: Boolean = false, val reel: Int = 0, val item: Int = 0, val paused: Boolean = false)

data class StoryOutcome(val state: StoryState, val exit: Boolean = false)

object StoryNav {
    fun reduce(s: StoryState, key: NavKey, reels: Int, itemsOf: (Int) -> Int?): StoryOutcome {
        if (!s.viewing) return when (key) {
            NavKey.Previous -> StoryOutcome(s.copy(reel = if (reels > 0) wrapIndex(s.reel, -1, reels) else 0))
            NavKey.Next -> StoryOutcome(s.copy(reel = if (reels > 0) wrapIndex(s.reel, 1, reels) else 0))
            NavKey.Select -> StoryOutcome(if (reels > 0) s.copy(viewing = true, item = 0, paused = false) else s)
            NavKey.Back -> StoryOutcome(s, exit = true)
        }
        return when (key) {
            NavKey.Next -> StoryOutcome(advance(s, reels, itemsOf))
            NavKey.Previous -> StoryOutcome(
                when {
                    s.item > 0 -> s.copy(item = s.item - 1, paused = false)
                    s.reel > 0 -> s.copy(reel = s.reel - 1, item = 0, paused = false)
                    else -> s.copy(paused = false)
                },
            )
            NavKey.Select -> StoryOutcome(s.copy(paused = !s.paused))
            NavKey.Back -> StoryOutcome(s.copy(viewing = false, item = 0, paused = false))
        }
    }

    /** 다음 장으로(저절로 넘어갈 때도). 아직 안 받은 사람이면 그대로. */
    fun advance(s: StoryState, reels: Int, itemsOf: (Int) -> Int?): StoryState {
        val count = itemsOf(s.reel) ?: return s
        return when {
            s.item + 1 < count -> s.copy(item = s.item + 1, paused = false)
            s.reel + 1 < reels -> s.copy(reel = s.reel + 1, item = 0, paused = false)
            else -> s.copy(viewing = false, item = 0, paused = false)
        }
    }
}
