package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.data.RssParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssParserTest {

    private fun rss(vararg items: String) =
        """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/"><channel>
           <title>채널</title><link>https://example.com</link>${items.joinToString("")}</channel></rss>"""

    @Test
    fun `CDATA와 pubDate가 있는 연합뉴스 형식`() {
        val articles = RssParser.parse(
            rss(
                """<item>
                     <title><![CDATA[[인사] 보건복지부]]></title>
                     <link>https://www.yna.co.kr/view/AKR1</link>
                     <pubDate>Mon, 19 Oct 2026 15:54:01 +0900</pubDate>
                     <dc:creator>김잔디</dc:creator>
                     <description><![CDATA[▲ 국립부곡병원장 김유석 (서울=연합뉴스)
(끝)]]></description>
                   </item>""",
            ),
        )
        assertEquals(1, articles.size)
        val a = articles.first()
        assertEquals("[인사] 보건복지부", a.title)
        assertEquals("https://www.yna.co.kr/view/AKR1", a.link)
        assertEquals("김잔디", a.author)
        assertEquals("▲ 국립부곡병원장 김유석 (서울=연합뉴스)\n(끝)", a.summary)
        // 2026-10-19T06:54:01Z
        assertEquals(1_792_392_841L, a.publishedAt)
    }

    @Test
    fun `dc date와 엔티티가 있는 경향신문 형식`() {
        val a = RssParser.parse(
            rss(
                """<item>
                     <dc:creator><![CDATA[김종목 기자]]></dc:creator>
                     <title><![CDATA[“단절된 사회”···[만나듣다]&#9322;소장 강신준]]></title>
                     <link>https://www.khan.co.kr/article/1/?utm_source=khan_rss&amp;utm_medium=rss</link>
                     <description><![CDATA[강신준이 &lt;자본&gt;을 보여줬다.]]></description>
                     <dc:date>2026-10-19T15:56:01+09:00</dc:date>
                   </item>""",
            ),
        ).single()
        assertEquals("“단절된 사회”···[만나듣다]⑪소장 강신준", a.title)
        assertEquals("https://www.khan.co.kr/article/1/?utm_source=khan_rss&utm_medium=rss", a.link)
        assertEquals("강신준이 <자본>을 보여줬다.", a.summary)
        assertEquals("김종목 기자", a.author)
        assertEquals(1_792_392_961L, a.publishedAt)
    }

    @Test
    fun `description의 img 태그와 링크는 평문으로`() {
        val a = RssParser.parse(
            rss(
                """<item><title><![CDATA[제목]]></title><link><![CDATA[https://www.donga.com/a/1]]></link>
                   <pubDate>Mon, 19 Oct 2026 15:59:19 +0900</pubDate>
                   <description><![CDATA[<img src='https://dimg.donga.com/x.jpg' align='left'>본문 <b>요약</b>입니다.]]></description></item>""",
            ),
        ).single()
        assertEquals("https://www.donga.com/a/1", a.link)
        assertEquals("본문 요약입니다.", a.summary)
        assertNull(a.author)
    }

    @Test
    fun `제목이나 링크가 없는 항목은 건너뛴다`() {
        val articles = RssParser.parse(
            rss(
                "<item><title>제목만</title></item>",
                "<item><link>https://x.com/1</link></item>",
                "<item><title>정상</title><link>https://x.com/2</link></item>",
            ),
        )
        assertEquals(listOf("정상"), articles.map { it.title })
    }

    @Test
    fun `description이나 날짜가 없어도 파싱한다`() {
        val a = RssParser.parse(rss("<item><title>t</title><link>https://x.com/1</link></item>")).single()
        assertEquals("", a.summary)
        assertEquals(0L, a.publishedAt)
    }

    @Test
    fun `잘못된 날짜는 0으로 둔다`() {
        val a = RssParser.parse(
            rss("<item><title>t</title><link>https://x.com/1</link><pubDate>어제</pubDate></item>"),
        ).single()
        assertEquals(0L, a.publishedAt)
    }

    @Test
    fun `limit만큼만 읽고 순서를 유지한다`() {
        val items = (1..5).map { "<item><title>t$it</title><link>https://x.com/$it</link></item>" }
        val articles = RssParser.parse(rss(*items.toTypedArray()), limit = 3)
        assertEquals(listOf("t1", "t2", "t3"), articles.map { it.title })
    }

    @Test
    fun `item이 없으면 빈 목록`() {
        assertEquals(emptyList<Any>(), RssParser.parse(rss()))
        assertEquals(emptyList<Any>(), RssParser.parse("not xml at all"))
    }

    @Test
    fun `DOCTYPE 엔티티는 해석하지 않는다`() {
        val xml = """<?xml version="1.0"?><!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
            <rss><channel><item><title>&x;</title><link>https://x.com/1</link></item></channel></rss>"""
        assertEquals("&x;", RssParser.parse(xml).single().title)
    }

    @Test
    fun `Atom entry는 href 링크, content, published를 읽고 목록 항목은 줄을 나눈다`() {
        val xml = """<?xml version='1.0'?><feed xmlns='http://www.w3.org/2005/Atom'><title>GN</title>
            <entry><title><![CDATA[JVM 시작 시간 줄이기]]></title>
              <link rel='alternate' type='text/html' href='https://news.hada.io/topic?id=1&amp;utm=x' />
              <published>2026-11-04T11:30:57+09:00</published><author><name>neo</name></author>
              <content type='html'><![CDATA[<div><ul><li><b>첫째</b>임</li><li>둘째임</li></ul></div>]]></content></entry></feed>"""
        val a = RssParser.parse(xml).single()
        assertEquals("JVM 시작 시간 줄이기", a.title)
        assertEquals("https://news.hada.io/topic?id=1&utm=x", a.link)
        assertEquals("neo", a.author)
        assertEquals("첫째임\n둘째임", a.summary)
        assertEquals(java.time.OffsetDateTime.parse("2026-11-04T11:30:57+09:00").toEpochSecond(), a.publishedAt)
    }
}
