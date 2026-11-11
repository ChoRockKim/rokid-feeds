package com.chorockkim.rokidfeeds.threads

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * threads.com 웹이 받는 JSON(GraphQL 응답, 페이지에 박힌 `data-sjs` 스크립트)에서 글을 꺼낸다. Android 없는 순수 코드.
 *
 * 화면 HTML(난독화된 class 이름)이 아니라 데이터 필드 이름을 본다. 2026-11-11에 로그인하지 않은 브라우저로 확인한 모양:
 * - 목록(홈·프로필): `…edges[].node.thread_items[].post` — 묶음 하나가 목록의 한 줄.
 * - 글 페이지: `data.media`(본문), `media.text_post_app_info.self_thread.posts.edges[].node`(작성자가 이어 쓴 글),
 *   `media.text_post_app_info.direct_replies.edges[].node.posts.edges[].node`(댓글). `relatedPosts`는 관련 글이라 뺀다.
 * - 글: `pk`, `code`, `taken_at`, `user.username`, `user.profile_pic_url`, `caption.text`, `like_count`, `text_post_app_info.direct_reply_count`,
 *   사진 `image_versions2.candidates[].url/width`, 여러 장 `carousel_media[]`, 동영상 `video_versions`.
 * 구조가 바뀌면 빈 결과가 나올 뿐 앱이 죽지는 않는다.
 */
object ThreadsParser {
    /** 사진은 안경 띠 폭(480)에 맞춰 이 폭 이상인 것 중 가장 작은 것을 고른다. */
    const val IMAGE_TARGET_WIDTH = 480

    /** 응답 하나를 JSON들로 읽는다. GraphQL은 줄마다 JSON이 따로 오기도 하고 앞에 `for (;;);`가 붙기도 한다. */
    fun jsonValues(raw: String): List<Any> {
        val text = raw.trim().removePrefix("for (;;);")
        runCatching { return listOf(JSONTokener(text).nextValue()) }
        return text.lineSequence().mapNotNull { line ->
            line.trim().removePrefix("for (;;);").takeIf { it.startsWith("{") || it.startsWith("[") }
                ?.let { runCatching { JSONTokener(it).nextValue() }.getOrNull() }
        }.toList()
    }

    /** 목록 묶음(`thread_items`)을 나오는 순서대로. */
    fun items(raw: String): List<ThreadItem> {
        val out = mutableListOf<ThreadItem>()
        jsonValues(raw).forEach { walk(it, skip = setOf("relatedPosts")) { key, value ->
            if (key == "thread_items" && value is JSONArray) {
                val posts = (0 until value.length()).mapNotNull { value.optJSONObject(it)?.optJSONObject("post")?.let(::post) }
                if (posts.isNotEmpty()) out += ThreadItem(posts.first(), posts.drop(1))
                true
            } else false
        } }
        return out
    }

    /**
     * 글 페이지 응답 하나에서 꺼낸 조각. 한 번에 글 페이지 하나만 열므로 그 페이지의 `direct_replies` 아래 글은 모두 그 글의 댓글이다.
     * 댓글을 더 받는 응답에는 본문 글(code)이 빠져 있어 [main]이 null일 수 있다(2026-11-11 확인). [merge]로 모은다.
     */
    data class DetailPart(val main: ThreadPost?, val continuation: List<ThreadPost>, val replies: List<ThreadPost>)

    fun detailPart(raw: String, code: String): DetailPart {
        var main: ThreadPost? = null
        val continuation = linkedMapOf<String, ThreadPost>()
        val replies = linkedMapOf<String, ThreadPost>()
        jsonValues(raw).forEach { root ->
            walk(root, skip = setOf("relatedPosts")) { key, value ->
                when {
                    value !is JSONObject -> false
                    key == "self_thread" -> { postsUnder(value).forEach { continuation.putIfAbsent(it.pk, it) }; true }
                    key == "direct_replies" -> { postsUnder(value).forEach { replies.putIfAbsent(it.pk, it) }; true }
                    value.optString("code") == code && isPost(value) -> { if (main == null) main = post(value); false }
                    else -> false
                }
            }
        }
        return DetailPart(main, continuation.values.toList(), replies.values.toList())
    }

    /** 지금까지 모은 상세에 조각 하나를 더한다(나중에 온 것을 뒤에 붙이고 중복은 뺀다). 본문을 아직 못 찾았으면 null. */
    fun merge(detail: ThreadDetail?, part: DetailPart, fallbackMain: ThreadPost? = null): ThreadDetail? {
        val main = detail?.main ?: part.main ?: fallbackMain ?: return null
        val author = main.author
        val continuation = ((detail?.continuation.orEmpty()) + part.continuation.filter { it.author == author })
            .distinctBy { it.pk }.filter { it.pk != main.pk }
        val replies = ((detail?.replies.orEmpty()) + part.replies)
            .distinctBy { it.pk }.filter { it.pk != main.pk && continuation.none { c -> c.pk == it.pk } }
        return ThreadDetail(main, continuation, replies)
    }

    private fun postsUnder(node: JSONObject?): List<ThreadPost> {
        node ?: return emptyList()
        val out = mutableListOf<ThreadPost>()
        walk(node, skip = emptySet()) { _, value ->
            if (value is JSONObject && isPost(value)) { post(value)?.let(out::add); true } else false
        }
        return out
    }

    private fun isPost(o: JSONObject): Boolean =
        o.has("pk") && o.has("code") && o.has("taken_at") && o.optJSONObject("user")?.has("username") == true

    /** 글 객체 → [ThreadPost]. 필수 필드가 없으면 null. */
    fun post(o: JSONObject): ThreadPost? {
        if (!isPost(o)) return null
        val info = o.optJSONObject("text_post_app_info")
        val carousel = o.optJSONArray("carousel_media")
        val images = if (carousel != null && carousel.length() > 0) {
            (0 until carousel.length()).mapNotNull { carousel.optJSONObject(it)?.let(::bestImage) }
        } else listOfNotNull(bestImage(o))
        return ThreadPost(
            pk = o.optString("pk"),
            code = o.optString("code"),
            author = o.getJSONObject("user").optString("username"),
            text = o.optJSONObject("caption")?.optString("text").orEmpty(),
            takenAt = o.optLong("taken_at"),
            likeCount = o.optInt("like_count"),
            replyCount = info?.optInt("direct_reply_count") ?: 0,
            images = images,
            hasVideo = (o.optJSONArray("video_versions")?.length() ?: 0) > 0,
            avatar = o.getJSONObject("user").optString("profile_pic_url").takeIf { it.startsWith("https://") },
        )
    }

    private fun bestImage(o: JSONObject): String? {
        val candidates = o.optJSONObject("image_versions2")?.optJSONArray("candidates") ?: return null
        val list = (0 until candidates.length()).mapNotNull { candidates.optJSONObject(it) }
            .filter { it.optString("url").startsWith("https://") }
        if (list.isEmpty()) return null
        val wide = list.filter { it.optInt("width") >= IMAGE_TARGET_WIDTH }
        return (wide.minByOrNull { it.optInt("width") } ?: list.maxByOrNull { it.optInt("width") })?.optString("url")
    }

    /**
     * 깊이 우선으로 돈다. [visit]가 true를 돌려주면 그 값 안으로는 더 들어가지 않는다(이미 처리함).
     * [skip]에 있는 키 아래는 보지 않는다.
     */
    private fun walk(value: Any?, skip: Set<String>, key: String? = null, visit: (String?, Any?) -> Boolean) {
        if (visit(key, value)) return
        when (value) {
            is JSONObject -> value.keys().forEach { k -> if (k !in skip) walk(value.opt(k), skip, k, visit) }
            is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i), skip, key, visit)
        }
    }
}
