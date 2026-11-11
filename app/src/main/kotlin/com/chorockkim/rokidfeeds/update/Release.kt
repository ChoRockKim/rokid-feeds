package com.chorockkim.rokidfeeds.update

import org.json.JSONObject

/**
 * 업데이트 서버의 최신 APK(`GET <UPDATE_URL>/latest`):
 * `{"versionCode": 200, "versionName": "0.2.0", "size": 7481342, "sha256": "…", "uploadedAt": "…"}`.
 * rokid-lens의 Release와 같은 형식이다(JSON 파서만 nanojson 대신 안드로이드 기본 org.json).
 */
data class Release(
    val versionName: String,
    val versionCode: Int,
    val size: Long,
    /** APK의 SHA-256(소문자 16진수). */
    val sha256: String,
) {
    companion object {
        /** 필드가 빠졌거나 이상하면 null. */
        fun parse(json: String): Release? = runCatching {
            val o = JSONObject(json)
            val sha = o.optString("sha256", "").lowercase()
            if (!Regex("[0-9a-f]{64}").matches(sha)) return null
            val code = o.optInt("versionCode", 0)
            val name = o.optString("versionName", "")
            if (code <= 0 || name.isEmpty()) return null
            Release(name, code, o.optLong("size", -1), sha)
        }.getOrNull()
    }
}
