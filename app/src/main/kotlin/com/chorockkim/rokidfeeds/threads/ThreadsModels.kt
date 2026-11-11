package com.chorockkim.rokidfeeds.threads

/** Threads 글 하나(본문·댓글·이어 쓴 글 공통). */
data class ThreadPost(
    val pk: String,
    /** 글 주소의 짧은 코드(`threads.com/@user/post/<code>`). */
    val code: String,
    val author: String,
    val text: String,
    /** 올린 시각(epoch 초). */
    val takenAt: Long,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    /** 사진 주소(여러 장이면 순서대로). 동영상은 미리보기 사진만. */
    val images: List<String> = emptyList(),
    val hasVideo: Boolean = false,
    /** 목록 썸네일용 작은 사진 주소([images]와 같은 순서). */
    val thumbs: List<String> = emptyList(),
    /** 프로필 사진 주소. 지금은 받지 않고 머리글자로 그린다(속도). */
    val avatar: String? = null,
) {
    val url: String get() = "https://www.threads.com/@$author/post/$code"
}

/**
 * 목록의 한 줄: 작성자가 이어 쓴 글 묶음(`thread_items`). [head]가 첫 글이고 [continuation]은 같은 작성자가 이어 쓴 글.
 */
data class ThreadItem(val head: ThreadPost, val continuation: List<ThreadPost> = emptyList())

/** 글 상세 페이지에서 꺼낸 것: 본문, 작성자가 이어 쓴 글, 댓글. */
data class ThreadDetail(
    val main: ThreadPost,
    val continuation: List<ThreadPost>,
    val replies: List<ThreadPost>,
)
