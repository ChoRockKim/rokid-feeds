package com.chorockkim.rokidfeeds.threads

import android.content.Context
import android.webkit.CookieManager
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature
import java.io.File

/**
 * Threads 계정 [COUNT]개(사용자 요청 2026-11-15: 두 계정을 미리 로그인해 두고 설정에서 빠르게 바꾸기).
 *
 * 계정마다 WebView **프로필**을 따로 쓴다(androidx.webkit `ProfileStore`, `WebViewCompat.setProfile`). 프로필마다 쿠키·
 * 저장소·캐시가 완전히 따로라 두 계정이 동시에 로그인돼 있을 수 있다. 계정 1은 기본 프로필이라 예전 로그인이 그대로 남는다.
 * 안경의 WebView가 프로필을 지원하지 않으면([supported] false, 사용자의 안경이 그랬다 2026-11-15) **쿠키 바꿔 끼우기**:
 * 기본 프로필 하나를 쓰고, 계정마다 threads·instagram 쿠키를 앱 저장소에 보관했다가 바꿀 때 지우고 되살린다
 * ([CookieSnapshot], [switchTo]). 쿠키 속성 일부(만료 등)는 잃어서 가끔 다시 로그인해야 할 수 있다.
 * 쿠키 저장본은 로그인 세션이라 앱 내부 저장소(다른 앱이 못 읽음)에만 둔다. WebView 자체 쿠키 저장소와 같은 수준이다.
 *
 * 앱이 기억하는 것: 지금 쓰는 계정, 계정마다 로그인할 때 폰에서 넣은 아이디(표시용). 비밀번호는 저장하지 않는다.
 * 글 저장본([ThreadsCache])도 계정마다 다른 폴더에 둔다(바꾼 뒤 다른 계정 피드가 잠깐 보이지 않게).
 */
class ThreadsAccounts(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("threads-accounts", Context.MODE_PRIVATE)
    private val filesDir = context.applicationContext.filesDir

    val supported: Boolean = runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) }.getOrDefault(false)

    /** 지금 쓰는 계정(0부터). 바꿀 때는 [switchTo]. */
    var active: Int
        get() = prefs.getInt(KEY_ACTIVE, 0).coerceIn(0, COUNT - 1)
        private set(value) { prefs.edit().putInt(KEY_ACTIVE, value.coerceIn(0, COUNT - 1)).apply() }

    /**
     * [i] 계정으로 바꾼다. 프로필을 쓰면 번호만 바꾼다(WebView를 그 프로필로 다시 만드는 것은 부르는 쪽).
     * 쿠키 바꿔 끼우기면: 지금 계정 쿠키를 보관 → WebView 쿠키·웹 저장소를 지움 → [i] 계정 저장본을 넣음. CookieManager 작업은
     * 한 줄로 차례대로 처리되므로, 이 뒤에 만드는 WebView는 바뀐 쿠키로 연다. 부르기 전에 WebView를 버려야 한다.
     */
    fun switchTo(i: Int) {
        if (i == active) return
        if (!supported) {
            saveCookies(active)
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            runCatching { android.webkit.WebStorage.getInstance().deleteAllData() }
            CookieSnapshot.decode(prefs.getString("$KEY_COOKIES$i", null)).forEach { (url, header) ->
                val domain = CookieSnapshot.SITES[url] ?: return@forEach
                CookieSnapshot.restoreLines(header, domain).forEach { cm.setCookie(url, it) }
            }
            cm.flush()
        }
        active = i
    }

    /** 쿠키 바꿔 끼우기: 지금 WebView 쿠키를 [i] 계정 저장본으로 보관한다. */
    fun saveCookies(i: Int) {
        if (supported) return
        val cm = CookieManager.getInstance()
        prefs.edit().putString("$KEY_COOKIES$i", CookieSnapshot.encode(CookieSnapshot.SITES.keys.associateWith { cm.getCookie(it) })).apply()
    }

    /** 로그아웃한 계정의 아이디·쿠키 저장본을 지운다. */
    fun forget(i: Int) {
        prefs.edit().remove("$KEY_NAME$i").remove("$KEY_COOKIES$i").apply()
    }

    fun name(i: Int): String? = prefs.getString("$KEY_NAME$i", null)?.takeIf { it.isNotBlank() }

    fun setName(i: Int, name: String?) {
        prefs.edit().apply { if (name.isNullOrBlank()) remove("$KEY_NAME$i") else putString("$KEY_NAME$i", name.trim().removePrefix("@")) }.apply()
    }

    /** WebView 프로필 이름. 계정 1 = 기본 프로필. */
    fun profile(i: Int): String = if (i == 0 || !supported) Profile.DEFAULT_PROFILE_NAME else "threads-account-${i + 1}"

    /** 그 계정의 쿠키 저장소(메인 스레드에서). */
    fun cookies(i: Int): CookieManager =
        if (i == 0 || !supported) CookieManager.getInstance()
        else ProfileStore.getInstance().getOrCreateProfile(profile(i)).cookieManager

    /** 지금 계정은 WebView 쿠키로, 쿠키 바꿔 끼우기에서 다른 계정은 저장본으로 본다. */
    fun isLoggedIn(i: Int): Boolean =
        if (supported || i == active) ThreadsWeb.isLoggedIn(runCatching { cookies(i) }.getOrNull())
        else CookieSnapshot.loggedIn(CookieSnapshot.decode(prefs.getString("$KEY_COOKIES$i", null)))

    fun cacheDir(i: Int): File = File(filesDir, if (i == 0) "threads-cache" else "threads-cache-${i + 1}")

    companion object {
        const val COUNT = 2
        private const val KEY_ACTIVE = "active"
        private const val KEY_NAME = "name"
        private const val KEY_COOKIES = "cookies"
    }
}
