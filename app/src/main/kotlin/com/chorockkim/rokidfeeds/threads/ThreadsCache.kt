package com.chorockkim.rokidfeeds.threads

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Threads 목록·글 상세의 기기 저장(TanStack Query의 캐시처럼 stale-while-revalidate). 켜자마자 저장본을 보여 주고
 * 뒤에서 새로 받아 바꾼다. 앱 전용 저장소(filesDir)에만 두고 [MAX_AGE_MS]가 지난 것·[MAX_DETAILS]를 넘는 것은 지운다.
 * 글 내용은 사용자가 이미 본(볼) 것만 저장한다.
 */
class ThreadsCache(private val dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    data class Entry<T>(val value: T, val savedAt: Long)

    private val feedFile get() = File(dir, "feed.json")
    private fun detailFile(code: String) = File(dir, "post-" + code.filter { it.isLetterOrDigit() || it == '_' || it == '-' } + ".json")

    fun saveFeed(items: List<ThreadItem>) = write(feedFile, JSONObject().put("savedAt", clock()).put("items", JSONArray(items.take(MAX_FEED).map(::itemJson))))

    fun loadFeed(): Entry<List<ThreadItem>>? = read(feedFile)?.let { o ->
        val list = o.optJSONArray("items") ?: return null
        Entry((0 until list.length()).mapNotNull { list.optJSONObject(it)?.let(::itemOf) }, o.optLong("savedAt"))
    }?.takeIf { clock() - it.savedAt < MAX_AGE_MS && it.value.isNotEmpty() }

    fun saveDetail(code: String, detail: ThreadDetail) = write(detailFile(code), JSONObject().put("savedAt", clock()).put("detail", detailJson(detail)))

    fun loadDetail(code: String): Entry<ThreadDetail>? = read(detailFile(code))?.let { o ->
        val d = o.optJSONObject("detail")?.let(::detailOf) ?: return null
        Entry(d, o.optLong("savedAt"))
    }?.takeIf { clock() - it.savedAt < MAX_AGE_MS }

    /** 오래된 것과 넘치는 글 상세를 지운다(앱을 켤 때 한 번). */
    fun prune() = runCatching {
        val files = dir.listFiles()?.filter { it.name.startsWith("post-") }.orEmpty()
        val now = clock()
        files.filter { now - it.lastModified() > MAX_AGE_MS }.forEach { it.delete() }
        files.filter { it.exists() }.sortedByDescending { it.lastModified() }.drop(MAX_DETAILS).forEach { it.delete() }
    }

    fun clear() = runCatching { dir.listFiles()?.forEach { it.delete() } }

    private fun write(file: File, o: JSONObject) = runCatching {
        dir.mkdirs()
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    private fun read(file: File): JSONObject? = runCatching { JSONObject(file.readText()) }.getOrNull()

    companion object {
        const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
        const val MAX_DETAILS = 100
        const val MAX_FEED = 60

        fun postJson(p: ThreadPost): JSONObject = JSONObject()
            .put("pk", p.pk).put("code", p.code).put("author", p.author).put("text", p.text).put("takenAt", p.takenAt)
            .put("likes", p.likeCount).put("replies", p.replyCount).put("images", JSONArray(p.images)).put("thumbs", JSONArray(p.thumbs))
            .put("video", p.hasVideo).put("avatar", p.avatar ?: "").put("tall", JSONArray(p.tall))

        fun postOf(o: JSONObject): ThreadPost? {
            val pk = o.optString("pk").takeIf { it.isNotEmpty() } ?: return null
            fun strings(name: String) = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
            return ThreadPost(
                pk = pk, code = o.optString("code"), author = o.optString("author"), text = o.optString("text"),
                takenAt = o.optLong("takenAt"), likeCount = o.optInt("likes"), replyCount = o.optInt("replies"),
                images = strings("images"), thumbs = strings("thumbs"), hasVideo = o.optBoolean("video"),
                tall = o.optJSONArray("tall")?.let { a -> (0 until a.length()).map { a.optBoolean(it) } }.orEmpty(),
                avatar = o.optString("avatar").takeIf { it.isNotEmpty() },
            )
        }

        private fun posts(a: JSONArray?): List<ThreadPost> = a?.let { (0 until it.length()).mapNotNull { i -> it.optJSONObject(i)?.let(::postOf) } }.orEmpty()

        fun itemJson(i: ThreadItem): JSONObject = JSONObject().put("head", postJson(i.head)).put("cont", JSONArray(i.continuation.map(::postJson)))
        fun itemOf(o: JSONObject): ThreadItem? = o.optJSONObject("head")?.let(::postOf)?.let { ThreadItem(it, posts(o.optJSONArray("cont"))) }

        fun detailJson(d: ThreadDetail): JSONObject = JSONObject().put("main", postJson(d.main))
            .put("cont", JSONArray(d.continuation.map(::postJson))).put("replies", JSONArray(d.replies.map(::postJson)))
        fun detailOf(o: JSONObject): ThreadDetail? = o.optJSONObject("main")?.let(::postOf)?.let {
            ThreadDetail(it, posts(o.optJSONArray("cont")), posts(o.optJSONArray("replies")))
        }
    }
}
