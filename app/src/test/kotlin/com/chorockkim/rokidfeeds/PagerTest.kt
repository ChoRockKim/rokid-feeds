package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.util.FIRST_PAGE_CHARS
import com.chorockkim.rokidfeeds.util.LINK_COST_CHARS
import com.chorockkim.rokidfeeds.util.NO_SUMMARY
import com.chorockkim.rokidfeeds.util.PAGE_CHARS
import com.chorockkim.rokidfeeds.util.buildDetailPages
import com.chorockkim.rokidfeeds.util.paginate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagerTest {

    private fun words(random: Random, count: Int): String {
        val syllables = "가나다라마바사아자차카타파하경향동아연합뉴스정부국회".toList()
        return (1..count).joinToString(" ") { String(CharArray(random.nextInt(1, 9)) { syllables.random(random) }) }
    }

    @Test
    fun `예산보다 짧으면 한 쪽`() {
        assertEquals(listOf("짧은 글"), paginate("짧은 글", 50, 100))
    }

    @Test
    fun `비어 있거나 공백뿐이면 쪽이 없다`() {
        assertEquals(emptyList<String>(), paginate("", 10, 10))
        assertEquals(emptyList<String>(), paginate("  \n ", 10, 10))
    }

    @Test
    fun `무작위 글에서도 모든 쪽이 예산 이하이고 단어 중간을 자르지 않는다`() {
        val random = Random(20261019)
        repeat(500) {
            val text = words(random, random.nextInt(1, 200))
            val first = random.nextInt(20, 120)
            val rest = random.nextInt(20, 300)
            val pages = paginate(text, first, rest)

            assertTrue(pages.first().length <= first)
            pages.drop(1).forEach { assertTrue("쪽이 예산 초과: ${it.length} > $rest", it.length <= rest) }
            // 쪽을 다시 이으면 원문의 단어 순서가 그대로여야 한다(단어는 최대 8자라 예산보다 짧다).
            assertEquals(text.split(" "), pages.flatMap { it.split(Regex("\\s+")) })
        }
    }

    @Test
    fun `공백이 없는 긴 문자열은 예산 위치에서 강제로 자르고 내용을 잃지 않는다`() {
        val text = "가".repeat(95)
        val pages = paginate(text, 30, 40)
        assertEquals(listOf(30, 40, 25), pages.map { it.length })
        assertEquals(text, pages.joinToString(""))
    }

    @Test
    fun `문장 끝을 공백보다 먼저 자를 곳으로 쓴다`() {
        // 예산 40: 첫 문장은 27자에서 끝나고(절반 20 이후), 그 뒤에도 예산 안에 공백이 있다
        val text = "정부가 어제 새로운 부동산 대책을 전격 발표했다. 야당은 즉각 반발하며 국정조사를 요구했고 여당은 신중한 입장이다"
        val pages = paginate(text, 40, 40)
        assertEquals("정부가 어제 새로운 부동산 대책을 전격 발표했다.", pages.first())
    }

    @Test
    fun `너무 짧은 쪽을 피하려고 앞쪽 절반의 문장 끝은 쓰지 않는다`() {
        // 첫 문장이 13자(예산 40의 절반 20 미만)라 문장 끝이 아니라 공백에서 자른다
        val pages = paginate("정부가 대책을 발표했다. 야당은 즉각 반발하며 국정조사를 요구했고 여당은 신중한 입장이다", 40, 40)
        assertTrue(pages.first().length > 13)
        assertTrue(pages.first().length <= 40)
    }

    @Test
    fun `문단 경계를 문장 끝보다 먼저 쓴다`() {
        val text = "첫 문단은 여기서 끝난다. 이어서 한 문장 더.\n\n둘째 문단이 이어진다 그리고 계속된다 길게 길게"
        val pages = paginate(text, 50, 50)
        assertEquals("첫 문단은 여기서 끝난다. 이어서 한 문장 더.", pages.first())
    }

    @Test
    fun `첫 쪽은 첫 예산을 쓰고 나머지는 나머지 예산을 쓴다`() {
        val pages = paginate("가나다 ".repeat(100), FIRST_PAGE_CHARS, PAGE_CHARS)
        assertTrue(pages.first().length <= FIRST_PAGE_CHARS)
        assertTrue(pages.drop(1).all { it.length <= PAGE_CHARS })
        assertTrue(pages.size > 1)
    }

    @Test
    fun `상수 예산으로 실제 피드급 요약을 나누면 모든 쪽이 상한 안이다`() {
        val sentence = "현대건설이 서울 양천구 목동10단지 재건축사업 시공권을 확보하며 올해 도시정비사업 수주 목표인 12조 원을 돌파했다. " +
            "현대건설은 지난 18일 열린 전체회의에서 시공사로 최종 선정됐다고 19일 밝혔다. "
        for (length in listOf(80, 200, 500, 900)) { // 연합뉴스 ~80, 경향신문 ~200, 동아일보 ~500, 그 이상
            val summary = sentence.repeat(10).take(length)
            assertEquals(length, summary.length)
            val pages = buildDetailPages(summary, "https://www.donga.com/news/Economy/article/all/20261019/134867835/1")
            pages.forEach { page ->
                val budget = if (page.showHeader) FIRST_PAGE_CHARS else PAGE_CHARS
                val cost = page.body.length + if (page.link != null) LINK_COST_CHARS else 0
                assertTrue("길이 $length: 쪽이 상한 초과 $cost > $budget", cost <= budget)
            }
        }
    }

    @Test
    fun `긴 요약은 여러 쪽으로 나뉘고 짧은 요약은 링크까지 한 쪽에 들어간다`() {
        val long = buildDetailPages("가나다 ".repeat(300), "https://x.com/1")
        assertTrue(long.size >= 3)
        // 연합뉴스급 짧은 요약(윗부분 20% 비운 뒤 상한이 줄어 약 75자까지)은 링크 때문에 쪽이 늘어나지 않는다
        assertEquals(1, buildDetailPages("가".repeat(75), "https://www.yna.co.kr/view/AKR20261019142000054").size)
    }

    @Test
    fun `요약이 비어 있으면 안내 문구 한 쪽과 링크`() {
        val pages = buildDetailPages("   ", "https://x.com/1")
        assertEquals(NO_SUMMARY, pages.single().body)
        assertEquals("https://x.com/1", pages.single().link)
    }

    @Test
    fun `링크는 자리가 있으면 마지막 쪽에 붙는다`() {
        val pages = buildDetailPages("짧은 요약", "https://x.com/1")
        assertEquals(1, pages.size)
        assertEquals("https://x.com/1", pages.single().link)
    }

    @Test
    fun `자리가 없으면 링크만 있는 쪽을 덧붙인다`() {
        val full = "가".repeat(FIRST_PAGE_CHARS - 10)
        val pages = buildDetailPages(full, "https://x.com/1")
        assertEquals(2, pages.size)
        assertNull(pages.first().link)
        assertEquals("", pages.last().body)
        assertEquals("https://x.com/1", pages.last().link)
    }

    @Test
    fun `링크가 비어 있으면 링크가 없다`() {
        val pages = buildDetailPages("요약", "")
        assertTrue(pages.all { it.link == null })
    }

    @Test
    fun `헤더는 첫 쪽에만 보인다`() {
        val pages = buildDetailPages("가나다 ".repeat(200), "https://x.com/1")
        assertEquals(listOf(true), pages.take(1).map { it.showHeader })
        assertTrue(pages.drop(1).none { it.showHeader })
    }
}
