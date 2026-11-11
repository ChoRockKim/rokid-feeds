package com.chorockkim.rokidfeeds

import android.app.Application
import com.chorockkim.rokidfeeds.update.Updater
import java.io.File

class RokidFeedsApp : Application() {
    val updater by lazy { Updater(this) }

    private val crashFile get() = File(filesDir, "last_crash.txt")

    override fun onCreate() {
        super.onCreate()
        // 안경에서는 logcat을 볼 수 없다. 앱이 죽은 이유를 남겨 두고 다음에 켤 때 화면에 보여 준다(rokid-lens와 같음).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { crashFile.writeText(crashSummary(e)) }
            previous?.uncaughtException(thread, e)
        }
    }

    /** 지난번 앱이 죽은 이유를 한 번만 돌려준다(파일은 지운다). 죽지 않았으면 null. */
    fun takeLastCrash(): String? = runCatching {
        crashFile.takeIf { it.exists() }?.let { f -> f.readText().also { f.delete() } }
    }.getOrNull()

    companion object {
        /** "OutOfMemoryError: … @ NewsApi.kt:42": 근본 원인과 이 앱 안에서 던져진 위치. */
        fun crashSummary(e: Throwable): String {
            var root = e
            while (root.cause != null && root.cause !== root) root = root.cause!!
            val here = (root.stackTrace + e.stackTrace).firstOrNull { it.className.startsWith("com.chorockkim") }
                ?: root.stackTrace.firstOrNull()
            val where = here?.let { " @ ${it.fileName}:${it.lineNumber}" }.orEmpty()
            return "${root.javaClass.simpleName}: ${root.message?.take(80) ?: ""}$where".trim()
        }
    }
}
