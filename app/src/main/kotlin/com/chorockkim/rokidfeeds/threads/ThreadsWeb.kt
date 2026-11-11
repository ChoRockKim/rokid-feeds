package com.chorockkim.rokidfeeds.threads

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * threads.com을 여는 WebView 두 개(목록·로그인용 [feed], 글 상세용 [post]). 보통은 화면에 보이지 않고, 페이지에 넣은
 * `assets/threads_bridge.js`가 페이지가 받은 글 JSON을 [deliver]로 넘긴다. 로그인할 때만 [feed]를 띠 안에 보여 준다.
 *
 * 로그인은 사용자가 이 WebView에서 직접 한다(쿠키는 WebView가 보관). 앱은 비밀번호를 보거나 저장하지 않는다.
 * 사용자가 보는 만큼만 연다: 목록 끝에 가까워지면 한 번 더 스크롤([loadMore]), 글을 열면 그 글 페이지 하나.
 */
@SuppressLint("SetJavaScriptEnabled")
class ThreadsWeb(context: Context, private val deliver: (Source, String, String) -> Unit) {
    enum class Source { Feed, Post }

    private val script = context.assets.open("threads_bridge.js").bufferedReader().use { it.readText() }
    private val main = Handler(Looper.getMainLooper())

    val feed: WebView = create(context, Source.Feed)
    val post: WebView = create(context, Source.Post)

    private fun create(context: Context, source: Source): WebView = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        // 포커스를 받지 않는다(링 노드에서 포커스를 뺏으면 R08 링 입력이 먹지 않는다).
        setInputEnabled(this, false)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = true
        // 글 데이터만 필요하다. 사진·동영상은 화면에 안 보이는 페이지가 받을 이유가 없고(느려짐), 앱이 필요할 때 따로 받는다.
        settings.blockNetworkImage = true
        if (android.os.Build.VERSION.SDK_INT >= 26) defaultFocusHighlightEnabled = false
        settings.userAgentString = settings.userAgentString.replace("; wv", "")
        CookieManager.getInstance().setAcceptCookie(true)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
        }
        addJavascriptInterface(Bridge(source), "RokidThreads")
        val documentStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (documentStart) WebViewCompat.addDocumentStartJavaScript(this, script, ALLOWED_ORIGINS)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !isAllowed(request.url) // threads·instagram(로그인) 밖으로는 나가지 않는다

            /** 사용자 사진·동영상 CDN(scontent*.cdninstagram.com, *.fbcdn.net)은 빈 응답. 화면 코드(static.*)는 그대로 받는다. */
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? =
                if (isUserMedia(request.url)) android.webkit.WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0))) else null

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                // 문서 시작 스크립트를 못 쓰는 WebView: 가능한 한 일찍 넣는다(처음 몇 응답은 놓칠 수 있다).
                if (!documentStart) view.evaluateJavascript(script, null)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (!documentStart) view.evaluateJavascript(script, null)
                view.evaluateJavascript("window.__rokidScan && window.__rokidScan()", null)
            }
        }
    }

    /** 페이지 스크립트 → 앱. 백그라운드 스레드에서 불리므로 메인 스레드로 넘긴다. threads 주소에서 온 것만 받는다. */
    private inner class Bridge(private val source: Source) {
        @JavascriptInterface
        fun onJson(pageUrl: String, json: String) {
            if (!isThreads(Uri.parse(pageUrl))) return
            main.post { deliver(source, pageUrl, json) }
        }
    }

    fun openHome() = feed.loadUrl(HOME)

    init {
        applyVisibility()
    }

    fun openPost(item: ThreadPost) = post.loadUrl(item.url)

    /** 목록 페이지를 아래로 내려 다음 글을 받게 한다(사람이 스크롤하는 것과 같다). */
    fun loadMore() = feed.evaluateJavascript("window.__rokidLoadMore ? window.__rokidLoadMore() : window.scrollBy(0, document.body.scrollHeight)", null)

    private val mobileUserAgent = feed.settings.userAgentString

    /**
     * 로그인 페이지. 모바일로 열면 "Instagram으로 계속하기"만 있고 인스타그램 앱·웹으로 넘어가야 하지만, 데스크톱으로 열면
     * threads.com에 아이디·비밀번호 칸이 바로 있다(2026-11-11 확인). 그래서 로그인할 때만 데스크톱 UA로 연다.
     */
    fun openLogin() {
        feed.settings.userAgentString = desktopUserAgent(mobileUserAgent)
        feed.loadUrl(LOGIN)
    }

    /** 로그인 화면을 나가면 원래(모바일) UA로. 글 데이터 모양은 모바일 UA에서 확인했다. */
    fun endLogin() {
        feed.settings.userAgentString = mobileUserAgent
    }

    /** 로그인 폼에 넣고 누른다. 폼이 없으면 false. */
    suspend fun submitLogin(username: String, password: String): Boolean =
        eval("window.__rokidLogin ? window.__rokidLogin(${JSONObject.quote(username)}, ${JSONObject.quote(password)}) : ''")
            .let { raw -> runCatching { JSONObject(unquote(raw)).optBoolean("ok") }.getOrDefault(false) }

    /** 인증 코드 칸에 넣고 누른다. */
    suspend fun submitCode(text: String): Boolean =
        eval("window.__rokidFillCode ? window.__rokidFillCode(${JSONObject.quote(text)}) : false") == "true"

    data class LoginState(val loginForm: Boolean, val code: String?, val error: String)

    /** 누른 뒤의 페이지: 로그인 폼이 아직 있는지, 인증 코드 칸(이름표), 오류 문구. 페이지가 바뀌는 중이면 null. */
    suspend fun loginState(): LoginState? = runCatching {
        val o = JSONObject(unquote(eval("window.__rokidLoginState ? window.__rokidLoginState() : ''")))
        LoginState(o.optBoolean("loginForm"), o.optString("code").takeIf { it.isNotEmpty() && it != "null" }, o.optString("error"))
    }.getOrNull()

    private suspend fun eval(js: String): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        feed.evaluateJavascript(js) { if (cont.isActive) cont.resumeWith(Result.success(it ?: "")) }
    }

    fun destroy() {
        feed.destroy()
        post.destroy()
    }

    private var login = false

    /**
     * WebView는 데이터를 받는 동안만 켠다(VISIBLE). 켜 두면 threads.com 앱 전체가 계속 돌고 그려져 우리 화면·링 입력까지
     * 느려졌다(사용자 확인). 끌 때는 GONE이라 그리지 않는다. [ThreadsSession]이 받기 시작·끝에 맞춰 바꾼다.
     */
    var feedActive = false
        set(value) { field = value; applyVisibility() }
    var postActive = false
        set(value) { field = value; applyVisibility() }

    private fun applyVisibility() {
        feed.visibility = if (login || feedActive) android.view.View.VISIBLE else android.view.View.GONE
        post.visibility = if (postActive) android.view.View.VISIBLE else android.view.View.GONE
    }

    /** 로그인 중에는 [feed]를 계속 켜 둔다(폰에서 받은 값을 넣을 로그인 페이지). 화면에는 보이지 않고 포커스도 받지 않는다. */
    fun setLoginMode(on: Boolean) {
        login = on
        applyVisibility()
    }

    /** Threads 화면 밖: 끄고 쉬게 한다. */
    fun sleep() {
        feedActive = false
        postActive = false
        feed.onPause()
        post.onPause()
    }

    fun wake() {
        feed.onResume()
        post.onResume()
    }

    companion object {
        private fun setInputEnabled(view: WebView, on: Boolean) {
            view.isFocusable = on
            view.isFocusableInTouchMode = on
            view.descendantFocusability = if (on) android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS else android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            if (!on) view.clearFocus()
        }

        const val HOME = "https://www.threads.com/"
        const val LOGIN = "https://www.threads.com/login"
        // 인스타그램은 로그인 단계에서 거친다. 거기서는 입력칸 이동 도우미만 쓰고 글 JSON은 받지 않는다(Bridge가 threads 주소만 받음).
        private val ALLOWED_ORIGINS = setOf("https://www.threads.com", "https://threads.com", "https://www.threads.net", "https://www.instagram.com")

        private fun isThreads(uri: Uri): Boolean = uri.scheme == "https" &&
            (uri.host == "threads.com" || uri.host?.endsWith(".threads.com") == true ||
                uri.host == "threads.net" || uri.host?.endsWith(".threads.net") == true)

        /** 사용자가 올린 사진·동영상 주소(페이지가 받지 않게 막는다). */
        fun isUserMedia(uri: Uri): Boolean {
            val host = uri.host ?: return false
            return host.startsWith("scontent") && (host.endsWith(".cdninstagram.com") || host.endsWith(".fbcdn.net")) ||
                host.startsWith("video") && host.endsWith(".fbcdn.net")
        }

        private fun isAllowed(uri: Uri): Boolean = isThreads(uri) ||
            (uri.scheme == "https" && (uri.host == "instagram.com" || uri.host?.endsWith(".instagram.com") == true))

        /** evaluateJavascript가 돌려준 JSON 문자열 값("\"{...}\"")을 벗긴다. */
        fun unquote(raw: String): String =
            if (raw.startsWith("\"")) org.json.JSONTokener(raw).nextValue() as? String ?: "" else raw

        /** "Mozilla/5.0 (Linux; Android 12; …) … Chrome/124 Mobile Safari/537.36" → 같은 Chrome의 데스크톱 리눅스 UA. */
        fun desktopUserAgent(mobile: String): String =
            mobile.replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)").replace(" Mobile", "").replace("; wv", "")

        /** 로그인돼 있는지: threads.com 쿠키에 세션이 있는지로 본다. */
        fun isLoggedIn(): Boolean = runCatching {
            CookieManager.getInstance().getCookie(HOME).orEmpty().let { "sessionid=" in it || "ds_user_id=" in it }
        }.getOrDefault(false)

        fun logout(done: () -> Unit) {
            CookieManager.getInstance().removeAllCookies { done() }
        }
    }
}
