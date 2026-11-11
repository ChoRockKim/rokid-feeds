package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.data.ArticleBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleBodyTest {
    private val long1 = "청주시는 매달 만드는 TV청주시정소식을 이달부터 드라마 형식으로 바꿔 선보였다고 4일 밝혔다."
    private val long2 = "딱딱한 정보 전달에서 벗어나 웃음과 감동을 추가해 시민들이 재미있게 접하게 하자는 취지다."

    @Test
    fun `연합뉴스는 속성 없는 p만 뽑고 사진 설명 이메일 저작권 문구는 뺀다`() {
        val html = """<div class="story-news article"><div class="writer-zone01"><p class="x">기자 소개</p></div>
            <p>$long1</p><p class="txt-desc">[청주시 제공. 재판매 및 DB 금지]</p><p>$long2</p><p>vodcast@yna.co.kr</p>
            <p class="txt-copyright adrs">제보는 카카오톡 <저작권자(c) 연합뉴스></p></div><p>다른 영역 문단은 뽑지 않는다 다른 영역 문단은 뽑지 않는다</p>"""
        assertEquals("$long1\n\n$long2", ArticleBody.extract("https://www.yna.co.kr/view/AKR1", html))
    }

    @Test
    fun `경향신문은 articleBody 아래 content_text 문단만 뽑는다`() {
        val html = """<p>밖의 문단</p><div id="articleBody"><p class="content_text text-l">$long1</p>
            <p class="dapartment">정책사회부</p><p class="content_text text-l">$long2</p></div>"""
        assertEquals("$long1\n\n$long2", ArticleBody.extract("https://www.khan.co.kr/article/1/?utm=x", html))
    }

    @Test
    fun `동아일보는 광고 스크립트 사진을 걷어 내고 br로 나뉜 문단을 읽는다`() {
        val html = """<section class="news_view"><figure><img src='a.jpg'><figcaption>ⓒ뉴시스</figcaption></figure>
            <div class='view_adK'><script>googletag.cmd.push(1)</script></div>$long1<br><br>$long2<br><br>
            <div class='ad'><script>x()</script></div>ⓒ 동아일보 저작권자 무단 전재</section><section>다른 섹션</section>"""
        assertEquals("$long1\n\n$long2", ArticleBody.extract("https://www.donga.com/news/Culture/article/all/1/2", html))
    }

    @Test
    fun `지원하지 않는 사이트나 구조가 다르면 null`() {
        assertNull(ArticleBody.extract("https://news.hada.io/topic?id=1", "<p>$long1</p>"))
        assertNull(ArticleBody.extract("https://www.yna.co.kr/view/1", "<html><p>$long1</p></html>"))
        assertNull(ArticleBody.extract("not a url", "<p>$long1</p>"))
    }

    @Test
    fun `너무 짧게 뽑히면 버린다`() {
        assertNull(ArticleBody.extract("https://www.khan.co.kr/a", """<div id="articleBody"><p class="content_text">짧음</p></div>"""))
    }

    @Test
    fun `상한을 넘으면 문장 끝에서 자르고 말줄임표를 붙인다`() {
        val sentence = "이것은 한 문장이다. "
        val paragraphs = (1..300).joinToString("") { """<p class="content_text">$sentence</p>""" }
        val body = ArticleBody.extract("https://www.khan.co.kr/a", """<div id="articleBody">$paragraphs</div>""")!!
        assertTrue(body.length <= ArticleBody.MAX_CHARS + 1)
        assertTrue(body.endsWith("…"))
        assertTrue("문장 중간에서 잘리지 않음", body.dropLast(1).trimEnd().endsWith("."))
    }
}
