package com.chorockkim.rokidfeeds.nav

/**
 * Threads 글 상세 안의 이동(순수, Android 없음). 화면은 세 가지다.
 * - [Mode.Post] 글: 본문 쪽([page]). 사진은 본문 아래에 작게 붙는다.
 * - [Mode.Photos] 사진 크게 보기: 그 쪽의 [photo]번째 사진.
 * - [Mode.Comments] 댓글 목록: [comment]번째 댓글을 고른 상태.
 *
 * 흐름(사용자 요청 2026-11-11): 글에서 탭 → 사진 크게(사진이 있으면) → 위아래 쓸기로 다른 사진 → 뒤로 → 글.
 * 글에서 아래로(다음) 쓸기 → 다음 쪽 → 마지막 쪽 다음은 댓글 목록. 댓글에서 탭 → 그 댓글을 연다(답글·사진).
 * 댓글에서 뒤로 → 글(첫 쪽), 글에서 뒤로 → 밖([DetailOutcome.back]: 이전 글이나 목록).
 */
enum class Mode { Post, Photos, Comments }

data class DetailState(val mode: Mode = Mode.Post, val page: Int = 0, val photo: Int = 0, val comment: Int = 0)

data class DetailOutcome(val state: DetailState, val openComment: Int? = null, val back: Boolean = false)

object ThreadDetailNav {
    /**
     * [photosPerPage]는 글 쪽마다 붙은 사진 수(쪽 수 = 크기), [comments]는 지금까지 받은 댓글 수.
     */
    fun reduce(state: DetailState, key: NavKey, photosPerPage: List<Int>, comments: Int): DetailOutcome {
        val s = normalize(state, photosPerPage, comments)
        val lastPage = (photosPerPage.size - 1).coerceAtLeast(0)
        return when (s.mode) {
            Mode.Post -> when (key) {
                NavKey.Next -> DetailOutcome(
                    when {
                        s.page < lastPage -> s.copy(page = s.page + 1)
                        comments > 0 -> s.copy(mode = Mode.Comments, comment = 0)
                        else -> s
                    },
                )
                NavKey.Previous -> DetailOutcome(s.copy(page = (s.page - 1).coerceAtLeast(0)))
                NavKey.Select -> DetailOutcome(
                    if (photosPerPage.getOrElse(s.page) { 0 } > 0) s.copy(mode = Mode.Photos, photo = 0)
                    else if (s.page < lastPage) s.copy(page = s.page + 1)
                    else if (comments > 0) s.copy(mode = Mode.Comments, comment = 0)
                    else s,
                )
                NavKey.Back -> DetailOutcome(s, back = true)
            }
            Mode.Photos -> {
                val last = (photosPerPage.getOrElse(s.page) { 0 } - 1).coerceAtLeast(0)
                when (key) {
                    NavKey.Next, NavKey.Select -> DetailOutcome(s.copy(photo = (s.photo + 1).coerceAtMost(last)))
                    NavKey.Previous -> DetailOutcome(s.copy(photo = (s.photo - 1).coerceAtLeast(0)))
                    NavKey.Back -> DetailOutcome(s.copy(mode = Mode.Post))
                }
            }
            Mode.Comments -> when (key) {
                NavKey.Next -> DetailOutcome(s.copy(comment = (s.comment + 1).coerceAtMost((comments - 1).coerceAtLeast(0))))
                NavKey.Previous -> DetailOutcome(
                    if (s.comment > 0) s.copy(comment = s.comment - 1) else s.copy(mode = Mode.Post, page = lastPage),
                )
                NavKey.Select -> DetailOutcome(s, openComment = s.comment.takeIf { comments > 0 })
                NavKey.Back -> DetailOutcome(s.copy(mode = Mode.Post, page = 0))
            }
        }
    }

    /** 쪽·사진·댓글 수가 바뀌었을 때(늦게 온 상세) 범위 안으로. 댓글이 없어졌으면 글로. */
    fun normalize(state: DetailState, photosPerPage: List<Int>, comments: Int): DetailState {
        val page = state.page.coerceIn(0, (photosPerPage.size - 1).coerceAtLeast(0))
        val photos = photosPerPage.getOrElse(page) { 0 }
        return when (state.mode) {
            Mode.Post -> state.copy(page = page)
            Mode.Photos -> if (photos == 0) DetailState(Mode.Post, page) else state.copy(page = page, photo = state.photo.coerceIn(0, photos - 1))
            Mode.Comments -> if (comments == 0) DetailState(Mode.Post, page) else state.copy(page = page, comment = state.comment.coerceIn(0, comments - 1))
        }
    }
}
