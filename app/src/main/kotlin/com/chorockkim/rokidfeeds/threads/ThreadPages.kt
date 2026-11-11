package com.chorockkim.rokidfeeds.threads

import com.chorockkim.rokidfeeds.util.paginate
import com.chorockkim.rokidfeeds.util.stripEmoji

/** Threads 상세의 한 쪽. 뉴스 상세처럼 스크롤 없이 쪽 단위로 넘긴다. */
sealed interface ThreadPage {
    /** 글자 쪽. [post]가 있으면 위에 작성자·시각을 함께 그린다(그 글의 첫 쪽). [label]은 "댓글 3/10" 같은 구분. */
    data class Text(val body: String, val post: ThreadPost? = null, val label: String? = null) : ThreadPage

    /** 사진 한 장. [index]/[total]은 그 글 안에서의 순서. */
    data class Image(val url: String, val post: ThreadPost, val index: Int, val total: Int) : ThreadPage

    /** 짧은 댓글 여러 개를 Threads 앱처럼 아바타 줄로 한 쪽에. [first]는 이 쪽 첫 댓글의 번호(0부터), [total]은 전체 댓글 수. */
    data class Replies(val replies: List<ThreadPost>, val first: Int, val total: Int) : ThreadPage
}

/** 쪽당 글자 수. 사진·작성자 줄이 없는 쪽은 뉴스 상세와 같은 290, 작성자 줄이 있는 첫 쪽은 230. */
const val THREAD_FIRST_PAGE_CHARS = 230
const val THREAD_PAGE_CHARS = 290

/** 사진 쪽 수 상한(한 글에 사진이 많아도 이만큼만). */
const val MAX_IMAGES_PER_POST = 6

/** 댓글 쪽: 한 쪽에 댓글 최대 3개, 글자 합 이만큼까지(댓글마다 작성자 줄 몫으로 [REPLY_HEADER_COST]를 더한다). */
const val REPLIES_PER_PAGE = 3
const val REPLY_PAGE_CHARS = 240
const val REPLY_HEADER_COST = 40

/**
 * 상세 쪽 목록: 본문 → 본문 사진 → 작성자가 이어 쓴 글(각각 글·사진) → 댓글(각 댓글이 작성자 줄과 함께 시작).
 * 상세를 아직 못 받았으면 목록에서 받은 [item]만으로 만든다(댓글 없이). 댓글이 있다고 했는데 아직 안 왔으면 안내 쪽을 붙인다.
 */
fun buildThreadPages(item: ThreadItem, detail: ThreadDetail?, loading: Boolean): List<ThreadPage> {
    val main = detail?.main ?: item.head
    val continuation = detail?.continuation ?: item.continuation
    val pages = mutableListOf<ThreadPage>()
    pages += postPages(main, label = null)
    continuation.forEach { pages += postPages(it, label = null) }
    val replies = detail?.replies.orEmpty()
    pages += replyPages(replies)
    when {
        loading -> pages += ThreadPage.Text("댓글 불러오는 중…")
        replies.isEmpty() && main.replyCount > 0 && detail != null -> pages += ThreadPage.Text("댓글을 불러오지 못했습니다 (로그인하면 더 보일 수 있습니다)")
    }
    return pages
}

/**
 * 댓글을 쪽으로 묶는다. 짧은 댓글은 한 쪽에 최대 [REPLIES_PER_PAGE]개(아바타 줄 목록), 한 쪽에 안 들어가는 긴 댓글이나
 * 사진이 있는 댓글은 혼자 글자 쪽·사진 쪽으로 나눈다.
 */
private fun replyPages(replies: List<ThreadPost>): List<ThreadPage> {
    val pages = mutableListOf<ThreadPage>()
    var group = mutableListOf<ThreadPost>()
    var groupStart = 0
    var cost = 0
    fun flush() {
        if (group.isNotEmpty()) pages += ThreadPage.Replies(group.toList(), groupStart, replies.size)
        group = mutableListOf()
        cost = 0
    }
    replies.forEachIndexed { i, reply ->
        val text = stripEmoji(reply.text)
        val c = text.length + REPLY_HEADER_COST
        if (c > REPLY_PAGE_CHARS || reply.images.isNotEmpty()) {
            flush()
            pages += postPages(reply, label = "댓글 ${i + 1}/${replies.size}")
            return@forEachIndexed
        }
        if (group.size >= REPLIES_PER_PAGE || cost + c > REPLY_PAGE_CHARS) flush()
        if (group.isEmpty()) groupStart = i
        group += reply.copy(text = text)
        cost += c
    }
    flush()
    return pages
}

private fun postPages(post: ThreadPost, label: String?): List<ThreadPage> {
    val text = stripEmoji(post.text).ifBlank { if (post.images.isEmpty()) if (post.hasVideo) "(동영상)" else "(내용 없음)" else "" }
    val pages = mutableListOf<ThreadPage>()
    if (text.isNotEmpty()) {
        paginate(text, THREAD_FIRST_PAGE_CHARS, THREAD_PAGE_CHARS).forEachIndexed { i, body ->
            pages += ThreadPage.Text(body, post = if (i == 0) post else null, label = if (i == 0) label else null)
        }
    }
    val images = post.images.take(MAX_IMAGES_PER_POST)
    images.forEachIndexed { i, url -> pages += ThreadPage.Image(url, post, i, images.size) }
    if (pages.isEmpty()) pages += ThreadPage.Text("(내용 없음)", post = post, label = label)
    // 글 없이 사진만 있으면 첫 사진 쪽이 작성자 줄 역할을 한다.
    return pages
}
