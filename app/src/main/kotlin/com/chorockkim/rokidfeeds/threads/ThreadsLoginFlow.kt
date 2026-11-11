package com.chorockkim.rokidfeeds.threads

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
    private val onLoggedIn: () -> Unit,
) {
    sealed interface Ui {
        data object Starting : Ui
        /** 폰을 기다리는 중. [error]는 직전 시도가 실패한 이유(폰에서 다시 보내면 된다). */
        data class Waiting(val url: String, val code: String, val error: String? = null) : Ui
        data class LoggingIn(val url: String, val code: String) : Ui
        data class NeedCode(val url: String, val code: String, val label: String) : Ui
        data object Done : Ui
        data class Failed(val message: String) : Ui
    }

    var ui by mutableStateOf<Ui>(Ui.Starting)
        private set

    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun run() {
        ui = Ui.Starting
        web.openLogin()
        val session = runCatching { relay.newSession() }.getOrElse {
            ui = Ui.Failed("로그인 서버에 연결하지 못했습니다 (${it.message ?: it.javaClass.simpleName})")
            return
        }
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
                        ui = Ui.LoggingIn(url, session.code)
                        val ok = web.submitLogin(m.username, m.password)
                        if (!ok) {
                            report(session, "error", "로그인 폼을 찾지 못했습니다")
                            web.openLogin()
                            ui = Ui.Waiting(url, session.code, "로그인 폼을 찾지 못했습니다")
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
            if (ThreadsWeb.isLoggedIn()) {
                report(session, "done")
                ui = Ui.Done
                onLoggedIn()
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
        const val WATCH_MS = 1_500L
        const val WATCH_STEPS = 16
    }
}
