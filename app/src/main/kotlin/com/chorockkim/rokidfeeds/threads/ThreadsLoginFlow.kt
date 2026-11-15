package com.chorockkim.rokidfeeds.threads

import com.chorockkim.rokidfeeds.util.NetErrors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * 폰 링크로 Threads에 로그인한다. 안경은 코드만 보여 주고, 폰에서 보낸 아이디·비밀번호(인증 코드)를 [LoginRelay]로 받아
 * 숨은 WebView의 threads.com 로그인 폼에 넣고 누른 뒤, 결과(로그인 쿠키 / 인증 코드 칸 / 오류 문구)를 폰 페이지에 알려 준다.
 * 받은 값은 폼에 넣을 때만 쓰고 변수에 남기지 않는다.
 */
class ThreadsLoginFlow(
    private val web: ThreadsWeb,
    private val relay: LoginRelay,
    private val scope: CoroutineScope,
    /** 로그인 완료(폰에서 넣은 아이디, 표시용). */
    private val onLoggedIn: (username: String?) -> Unit,
) {
    sealed interface Ui {
        /** 로그인 서버에 코드를 받는 중. [waking]이면 늦어서 서버가 잠에서 깨는 중으로 본다. */
        data class Starting(val waking: Boolean = false) : Ui
        /** 폰을 기다리는 중. [error]는 직전 시도가 실패한 이유(폰에서 다시 보내면 된다). */
        data class Waiting(val url: String, val code: String, val error: String? = null) : Ui
        data class LoggingIn(val url: String, val code: String) : Ui
        data class NeedCode(val url: String, val code: String, val label: String) : Ui
        data object Done : Ui
        data class Failed(val message: String) : Ui
    }

    var ui by mutableStateOf<Ui>(Ui.Starting())
        private set

    private var job: Job? = null
    private var username: String? = null

    fun start() {
        job?.cancel()
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun run() {
        ui = Ui.Starting()
        web.openLogin()
        // 서버(Railway)가 잠들어 있으면 첫 요청이 늦다: 늦으면 "깨우는 중"을 보여 주고, 실패해도 한 번 더 해 본 뒤에만 실패로.
        val hint = scope.launch { delay(NetErrors.WAKE_HINT_MS); if (ui is Ui.Starting) ui = Ui.Starting(waking = true) }
        val session = runCatching { relay.newSession() }
            .recoverCatching { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                ui = Ui.Starting(waking = true); delay(RETRY_AFTER_MS); relay.newSession()
            }
            .getOrElse {
                hint.cancel()
                if (it is kotlinx.coroutines.CancellationException) throw it
                ui = Ui.Failed("로그인 서버에 연결하지 못했습니다 · ${NetErrors.reason(it)}")
                return
            }
        hint.cancel()
        val url = relay.phoneUrl
        ui = Ui.Waiting(url, session.code)
        while (coroutineContext.isActive) {
            val messages = runCatching { relay.poll(session) }.getOrElse { e ->
                if (e is LoginRelay.ExpiredException) { ui = Ui.Failed("코드가 만료됐습니다 · 탭하면 새 코드"); return }
                delay(POLL_MS)
                continue
            }
            for (m in messages) {
                when (m) {
                    is LoginRelay.Message.Credentials -> {
                        // 아이디 앞뒤 공백을 뗀다: 아이폰 자동 완성으로 고르면 끝에 공백이 붙어 인스타가 "로그인 정보가 잘못"이라고 했다.
                        val user = m.username.trim()
                        username = user // 설정에 보여 줄 아이디만 기억한다(비밀번호는 넘기고 버린다)
                        ui = Ui.LoggingIn(url, session.code)
                        val result = web.submitLogin(user, m.password)
                        if (!result.ok) {
                            // 무엇이 떠 있었는지(입력칸·버튼 이름)를 같이 보여 줘서 원인을 알 수 있게 한다.
                            val why = "로그인 폼을 찾지 못했습니다 (${result.page})"
                            report(session, "error", why)
                            web.openLogin()
                            ui = Ui.Waiting(url, session.code, why)
                        } else watch(session, url)
                    }
                    is LoginRelay.Message.Code -> {
                        ui = Ui.LoggingIn(url, session.code)
                        if (web.submitCode(m.text)) watch(session, url)
                        else { report(session, "error", "인증 코드 칸을 찾지 못했습니다"); ui = Ui.Waiting(url, session.code, "인증 코드 칸을 찾지 못했습니다") }
                    }
                }
                if (ui == Ui.Done) return
            }
            delay(POLL_MS)
        }
    }

    /** 누른 뒤 결과를 본다: 로그인 쿠키가 생기면 완료, 인증 코드 칸이 나오면 코드 요청, 오류 문구면 다시 기다림. */
    private suspend fun watch(session: LoginRelay.Session, url: String) {
        repeat(WATCH_STEPS) {
            delay(WATCH_MS)
            if (web.isLoggedIn()) {
                report(session, "done")
                ui = Ui.Done
                onLoggedIn(username)
                return
            }
            // 서버가 실패 이유를 줬으면 그것을 보여 준다(화면 문구는 늘 "정보가 잘못"이라 진짜 이유를 알 수 없었다).
            web.lastLoginResult?.reason()?.let { why ->
                report(session, "error", why)
                web.openLogin()
                ui = Ui.Waiting(url, session.code, why)
                return
            }
            val state = web.loginState() ?: return@repeat
            when {
                state.code != null -> {
                    report(session, "need_code", state.code)
                    ui = Ui.NeedCode(url, session.code, state.code)
                    return
                }
                state.error.isNotEmpty() -> {
                    report(session, "error", state.error)
                    ui = Ui.Waiting(url, session.code, state.error)
                    if (!state.loginForm) web.openLogin()
                    return
                }
            }
        }
        report(session, "error", "응답이 없습니다. 다시 보내 주세요")
        web.openLogin()
        ui = Ui.Waiting(url, session.code, "응답이 없습니다")
    }

    private suspend fun report(session: LoginRelay.Session, state: String, message: String = "") {
        runCatching { relay.status(session, state, message) }
    }

    private companion object {
        const val POLL_MS = 2_000L
        /** 첫 요청이 실패하면(서버가 깨는 중일 수 있다) 이만큼 뒤 한 번 더. */
        const val RETRY_AFTER_MS = 3_000L
        const val WATCH_MS = 1_500L
        const val WATCH_STEPS = 16
    }
}
