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
 * 안경의 WebView가 프로필을 지원하지 않으면([supported] false) 계정 1만 쓴다(실기 미확인: 안경 WebView 버전을 모름).
 *
 * 앱이 기억하는 것: 지금 쓰는 계정, 계정마다 로그인할 때 폰에서 넣은 아이디(표시용). 비밀번호는 저장하지 않는다.
 * 글 저장본([ThreadsCache])도 계정마다 다른 폴더에 둔다(바꾼 뒤 다른 계정 피드가 잠깐 보이지 않게).
 */
class ThreadsAccounts(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("threads-accounts", Context.MODE_PRIVATE)
    private val filesDir = context.applicationContext.filesDir

    val supported: Boolean = runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) }.getOrDefault(false)

    /** 지금 쓰는 계정(0부터). 프로필을 못 쓰면 늘 0. */
    var active: Int
        get() = if (supported) prefs.getInt(KEY_ACTIVE, 0).coerceIn(0, COUNT - 1) else 0
        set(value) { prefs.edit().putInt(KEY_ACTIVE, value.coerceIn(0, COUNT - 1)).apply() }

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

    fun isLoggedIn(i: Int): Boolean = ThreadsWeb.isLoggedIn(runCatching { cookies(i) }.getOrNull())

    fun cacheDir(i: Int): File = File(filesDir, if (i == 0) "threads-cache" else "threads-cache-${i + 1}")

    companion object {
        const val COUNT = 2
        private const val KEY_ACTIVE = "active"
        private const val KEY_NAME = "name"
    }
}
