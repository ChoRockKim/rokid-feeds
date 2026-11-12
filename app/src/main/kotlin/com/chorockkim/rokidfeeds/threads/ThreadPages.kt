package com.chorockkim.rokidfeeds.threads

import com.chorockkim.rokidfeeds.util.paginate
import com.chorockkim.rokidfeeds.util.stripEmoji

/**
 * 글 상세의 한 쪽(Threads 피드처럼 본문 아래 사진). [header]면 작성자 줄을, [thumbs]가 있으면 본문 아래에 작은 사진 줄을
 * 그린다(그 글의 첫 쪽에만). [photos]는 이 쪽이 속한 글의 사진 전부: 어느 쪽에서든 탭하면 이 사진들을 크게 본다.
 * 아래로 넘길 때 사진만 있는 쪽이 끼지 않는다(사용자 요청: 사진은 탭할 때만 크게). [actions]면 아이콘 줄(그 글의 마지막 쪽).
 */
data class PostPage(
    val post: ThreadPost,
    val body: String,
    val header: Boolean,
    val photos: List<String> = emptyList(),
    val thumbs: List<String> = emptyList(),
    /** [photos]마다 길쭉한지(사진 화면에서 탭하면 반씩 확대). */
    val tall: List<Boolean> = emptyList(),
    val actions: Boolean = false,
    /** 작성자가 이어 쓴 글이면 몇 번째(1부터, 본문은 0)와 전체 수. 화면에 "이어서 2/3"으로 보여 다른 글처럼 보이지 않게 한다. */
    val part: Int = 0,
    val parts: Int = 1,
)

/** 작성자 줄이 있는 첫 쪽 / 나머지 쪽의 글자 수. */
const val THREAD_FIRST_PAGE_CHARS = 200
const val THREAD_PAGE_CHARS = 290

/**
 * 사진 줄이 있는 첫 쪽의 본문 길이. 띠 안쪽 246dp에 작성자 줄(30) + 본문 + 사진 줄(56) + 아이콘 줄 + 아래 안내가 들어가야 한다.
 * 110자일 때 넘쳐 본문이 잘렸다(사용자 확인) → 80자(한 줄 약 30자, 3줄).
 */
const val WITH_PHOTOS_CHARS = 80

/** 한 글에서 보여 줄 사진 수 상한. */
const val MAX_IMAGES_PER_POST = 10

/**
 * 글 쪽 목록: 본문(긴 글은 여러 쪽) → 작성자가 이어 쓴 글. 각 글의 사진 줄은 그 글 첫 쪽 본문 아래에만(첫 쪽 본문을 줄인다).
 * 댓글은 쪽이 아니라 따로 목록으로 보여 준다([ThreadDetail.replies]).
 */
/**
 * 본문을 쪽으로 나누는 방법. 앱에서는 실제 글꼴로 줄을 재서 나누고(ui/PostSplitter), 테스트에서는 글자 수로 나눈다.
 * [withPhotos]면 첫 쪽에 사진 줄이 들어가므로 첫 쪽이 짧아야 한다.
 */
typealias TextSplitter = (text: String, withPhotos: Boolean) -> List<String>

/** 글자 수로 나누기(테스트·기본값). 실제 화면은 글꼴에 따라 줄 수가 달라 넘칠 수 있어 앱은 줄을 재서 나눈다. */
val CharSplitter: TextSplitter = { text, withPhotos ->
    paginate(text, if (withPhotos) WITH_PHOTOS_CHARS else THREAD_FIRST_PAGE_CHARS, THREAD_PAGE_CHARS)
}

fun buildPostPages(item: ThreadItem, detail: ThreadDetail?, split: TextSplitter = CharSplitter): List<PostPage> {
    val main = detail?.main ?: item.head
    val posts = listOf(main) + (detail?.continuation ?: item.continuation).filter { it.author == main.author }
    return posts.flatMapIndexed { i, post -> pagesOf(post, split).map { it.copy(part = i, parts = posts.size) } }
}

private fun pagesOf(post: ThreadPost, split: TextSplitter): List<PostPage> {
    val text = stripEmoji(post.text)
    val photos = post.images.take(MAX_IMAGES_PER_POST)
    val thumbs = post.thumbs.ifEmpty { post.images }.take(MAX_IMAGES_PER_POST)
    val tall = photos.indices.map { post.tall.getOrElse(it) { false } }
    val chunks = if (text.isBlank()) listOf("") else split(text, photos.isNotEmpty()).ifEmpty { listOf(text) }
    val pages = chunks.mapIndexed { i, body ->
        PostPage(post, body, header = i == 0, photos = photos, thumbs = if (i == 0) thumbs else emptyList(), tall = tall)
    }.toMutableList()
    if (photos.isEmpty() && text.isBlank()) {
        pages[0] = pages[0].copy(body = if (post.hasVideo) "(동영상)" else "(내용 없음)")
    }
    pages[pages.lastIndex] = pages.last().copy(actions = true)
    return pages
}
