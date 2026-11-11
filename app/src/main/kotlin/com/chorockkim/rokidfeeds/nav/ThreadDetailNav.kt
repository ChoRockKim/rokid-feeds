package com.chorockkim.rokidfeeds.nav

/**
 * Threads 글 상세 안의 이동(순수, Android 없음). 쪽은 [pageSizes]로 주어지고, 댓글 목록 쪽은 그 쪽의 댓글 수(1 이상),
 * 그 밖의 쪽은 0이다.
 *
 * - 다음/이전: 댓글 쪽에서는 댓글을 하나씩 고르고(밝게), 쪽 끝에서 다음/이전 쪽으로 넘어간다. 맨 처음·맨 끝에서는 멈춘다.
 * - 선택: 댓글을 고른 상태면 그 댓글을 연다([DetailReduction.openReply] = 그 쪽에서 몇 번째 댓글). 아니면 다음 쪽.
 * - 뒤로: [DetailReduction.back] (부르는 쪽이 이전 글이나 목록으로 돌아간다).
 */
data class DetailPos(val page: Int = 0, val reply: Int = 0)

data class DetailReduction(val pos: DetailPos, val openReply: Int? = null, val back: Boolean = false)

object ThreadDetailNav {
    fun reduce(pos: DetailPos, key: NavKey, pageSizes: List<Int>): DetailReduction {
        if (pageSizes.isEmpty()) return DetailReduction(DetailPos(), back = key == NavKey.Back)
        val p = normalize(pos, pageSizes)
        val replies = pageSizes[p.page]
        val last = pageSizes.lastIndex
        return when (key) {
            NavKey.Next -> DetailReduction(
                when {
                    replies > 0 && p.reply < replies - 1 -> p.copy(reply = p.reply + 1)
                    p.page < last -> DetailPos(p.page + 1, 0)
                    else -> p
                },
            )
            NavKey.Previous -> DetailReduction(
                when {
                    replies > 0 && p.reply > 0 -> p.copy(reply = p.reply - 1)
                    p.page > 0 -> DetailPos(p.page - 1, (pageSizes[p.page - 1] - 1).coerceAtLeast(0))
                    else -> p
                },
            )
            NavKey.Select ->
                if (replies > 0) DetailReduction(p, openReply = p.reply)
                else DetailReduction(if (p.page < last) DetailPos(p.page + 1, 0) else p)
            NavKey.Back -> DetailReduction(p, back = true)
        }
    }

    /** 쪽 수·댓글 수가 바뀌었을 때(댓글이 늦게 옴) 범위 안으로. */
    fun normalize(pos: DetailPos, pageSizes: List<Int>): DetailPos {
        if (pageSizes.isEmpty()) return DetailPos()
        val page = pos.page.coerceIn(0, pageSizes.lastIndex)
        val replies = pageSizes[page]
        return DetailPos(page, if (replies > 0) pos.reply.coerceIn(0, replies - 1) else 0)
    }
}
