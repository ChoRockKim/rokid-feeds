package com.chorockkim.rokidfeeds.threads

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 0.2.10에서 ThreadsWeb의 init이 busy(MutableState) 선언보다 앞에 있어, Threads를 여는 순간 NullPointerException으로
 * 앱이 죽었다(Kotlin은 선언 순서대로 초기화). WebView는 JVM 테스트에서 만들 수 없어 소스 순서로 막는다.
 */
class ThreadsWebOrderTest {
    @Test
    fun initRunsAfterEveryStateItTouches() {
        val src = File("src/main/kotlin/com/chorockkim/rokidfeeds/threads/ThreadsWeb.kt").readText()
        val init = src.indexOf("    init {")
        assertTrue("init 블록이 있어야 한다", init > 0)
        for (name in listOf("private var login", "var feedActive", "var postActive", "var busy")) {
            val at = src.indexOf(name)
            assertTrue("$name 선언이 init보다 앞에 있어야 한다", at in 0 until init)
        }
    }
}
