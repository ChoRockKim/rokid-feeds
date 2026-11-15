package com.chorockkim.rokidfeeds.insta

import com.chorockkim.rokidfeeds.threads.ThreadsParser
import org.json.JSONArray
import org.json.JSONObject

/** 스토리 한 장(사진 또는 동영상). */
data class StoryItem(
    val pk: String,
    val takenAt: Long,
    /** 사진(동영상이면 첫 화면). */
    val image: String?,
    /** 동영상 주소(사진이면 null). */
    val video: String? = null,
)

/** 한 사람의 스토리 묶음. [items]가 비어 있으면 아직 안 받은 것(목록에서는 사람만 온다). */
data class StoryReel(
    val userPk: String,
    val username: String,
    /** 마지막으로 올린 시각(epoch 초). */
    val latest: Long,
    /** 다 봤는지(목록에서 흐리게). */
    val seen: Boolean = false,
    val items: List<StoryItem> = emptyList(),
) {
    val url: String get() = "https://www.instagram.com/stories/$username/"
}

/**
 * 인스타 웹이 받는 JSON에서 스토리를 꺼낸다(순수). 공개 자료로 짐작한 모양(실기 미확인, 2026-11-15):
 * - 목록(홈 위 동그라미 줄): `tray[]` 또는 `reels_tray` — 사람(`user.username`)과 `latest_reel_media`(시각), `seen`.
 * - 한 사람 스토리(스토리 페이지): `reels_media[]` — 같은 사람 객체에 `items[]`(XDTMediaDict: `pk`·`taken_at`·`media_type` 1 사진 /
 *   2 동영상·`image_versions2`·`video_versions`). 웹은 다음 사람 몇 명 것도 미리 넣어 준다.
 * 키 이름 대신 모양(사람 객체 + 시각 또는 items)으로 찾아서, 조금 바뀌어도 읽는다. 못 읽으면 빈 목록.
 */
object InstaStories {
    /** 응답 하나에서 찾은 스토리 묶음들(나온 순서, 같은 사람은 합침). */
    fun reels(raw: String): List<StoryReel> {
        val out = linkedMapOf<String, StoryReel>()
        ThreadsParser.jsonValues(raw).forEach { root ->
            ThreadsParser.walk(root, skip = emptySet()) { _, value ->
                val reel = (value as? JSONObject)?.let(::reel) ?: return@walk false
                out[reel.userPk] = out[reel.userPk]?.let { merge(it, reel) } ?: reel
                true
            }
        }
        return out.values.toList()
    }

    /** 이미 아는 묶음에 새로 온 것을 합친다(사람 정보는 새것, 장은 많은 쪽). */
    fun merge(old: StoryReel, new: StoryReel): StoryReel =
        new.copy(items = if (new.items.size >= old.items.size) new.items else old.items, seen = new.seen || old.seen, latest = maxOf(old.latest, new.latest))

    private fun reel(o: JSONObject): StoryReel? {
        val user = o.optJSONObject("user") ?: return null
        val username = user.optString("username").takeIf { it.isNotEmpty() } ?: return null
        if (o.has("taken_at") && o.has("code")) return null // 글(게시물)이지 스토리 묶음이 아니다
        val items = o.optJSONArray("items")?.let(::items).orEmpty()
        val latest = o.optLong("latest_reel_media").takeIf { it > 0 } ?: items.maxOfOrNull { it.takenAt } ?: return null
        val userPk = user.optString("pk").ifEmpty { user.optString("id") }.ifEmpty { username }
        // seen: 마지막으로 본 시각이 마지막 올린 시각 이상이면 다 봄.
        val seenAt = o.optLong("seen")
        return StoryReel(userPk, username, latest, seen = seenAt > 0 && seenAt >= latest, items = items)
    }

    private fun items(a: JSONArray): List<StoryItem> = (0 until a.length()).mapNotNull { i ->
        val m = a.optJSONObject(i) ?: return@mapNotNull null
        if (!m.has("taken_at")) return@mapNotNull null
        val image = ThreadsParser.bestImage(m, ThreadsParser.IMAGE_TARGET_WIDTH)
        val video = ThreadsParser.videoUrl(m)
        if (image == null && video == null) return@mapNotNull null
        StoryItem(pk = m.optString("pk").ifEmpty { m.optString("id") }, takenAt = m.optLong("taken_at"), image = image, video = video)
    }
}
