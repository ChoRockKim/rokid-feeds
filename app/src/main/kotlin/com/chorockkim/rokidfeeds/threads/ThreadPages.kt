package com.chorockkim.rokidfeeds.threads

import com.chorockkim.rokidfeeds.util.paginate
import com.chorockkim.rokidfeeds.util.stripEmoji

/**
 * 글 상세의 한 쪽(Threads 피드처럼 본문 아래 사진). [header]면 작성자 줄을, [photos]가 있으면 본문 아래에 작은 사진 줄을 그린다.
 * 사진 줄이 있는 쪽에서 탭하면 그 사진들을 크게 본다. [actions]면 아이콘 줄(그 글의 마지막 쪽).
 */
data class PostPage(
    val post: ThreadPost,
    val body: String,
    val header: Boolean,
    val photos: List<String> = emptyList(),
    val thumbs: List<String> = emptyList(),
    val actions: Boolean = false,
)

/** 작성자 줄이 있는 첫 쪽 / 나머지 쪽의 글자 수. */
const val THREAD_FIRST_PAGE_CHARS = 230
const val THREAD_PAGE_CHARS = 290

/** 사진 줄과 함께 한 쪽에 둘 수 있는 본문 길이. 넘치면 사진은 다음 쪽에 따로. */
const val WITH_PHOTOS_CHARS = 110

/** 한 글에서 보여 줄 사진 수 상한. */
const val MAX_IMAGES_PER_POST = 10

/**
 * 글 쪽 목록: 본문(긴 글은 여러 쪽) → 작성자가 이어 쓴 글. 각 글의 사진은 그 글 마지막 본문 쪽 아래(짧으면) 또는 바로 다음 쪽에.
 * 댓글은 쪽이 아니라 따로 목록으로 보여 준다([ThreadDetail.replies]).
 */
fun buildPostPages(item: ThreadItem, detail: ThreadDetail?): List<PostPage> {
    val posts = listOf(detail?.main ?: item.head) + (detail?.continuation ?: item.continuation)
    return posts.flatMap(::pagesOf)
}

private fun pagesOf(post: ThreadPost): List<PostPage> {
    val text = stripEmoji(post.text)
    val photos = post.images.take(MAX_IMAGES_PER_POST)
    val thumbs = post.thumbs.ifEmpty { post.images }.take(MAX_IMAGES_PER_POST)
    val chunks = if (text.isBlank()) listOf("") else paginate(text, THREAD_FIRST_PAGE_CHARS, THREAD_PAGE_CHARS)
    val pages = chunks.mapIndexed { i, body -> PostPage(post, body, header = i == 0) }.toMutableList()
    if (photos.isNotEmpty()) {
        val last = pages.last()
        if (last.body.length <= WITH_PHOTOS_CHARS) pages[pages.lastIndex] = last.copy(photos = photos, thumbs = thumbs)
        else pages += PostPage(post, "", header = false, photos = photos, thumbs = thumbs)
    } else if (text.isBlank()) {
        pages[0] = pages[0].copy(body = if (post.hasVideo) "(동영상)" else "(내용 없음)")
    }
    pages[pages.lastIndex] = pages.last().copy(actions = true)
    return pages
}
