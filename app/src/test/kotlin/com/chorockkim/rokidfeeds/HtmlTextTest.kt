package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.util.dropTruncatedTail
import com.chorockkim.rokidfeeds.util.htmlToText
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {
    @Test
    fun `태그를 제거하고 단락을 줄바꿈으로 바꾼다`() {
        assertEquals("a\n\nb", htmlToText("a<p>b"))
        assertEquals("a\n\nb", htmlToText("a<br/>b"))
        assertEquals("link text", htmlToText("""<a href="https://x.com">link text</a>"""))
    }

    @Test
    fun `엔티티를 복원한다`() {
        assertEquals("a & b < c > \"d\"", htmlToText("a &amp; b &lt; c &gt; &quot;d&quot;"))
        assertEquals("'/", htmlToText("&#x27;&#x2F;"))
        assertEquals("A", htmlToText("&#65;"))
    }

    @Test
    fun `apos 엔티티도 복원한다`() {
        assertEquals("'2026 이스라엘 성지'", htmlToText("&apos;2026 이스라엘 성지&apos;"))
    }

    @Test
    fun `이중 이스케이프는 한 번만 푼다`() {
        assertEquals("&lt;", htmlToText("&amp;lt;"))
    }

    @Test
    fun `잘못된 숫자 엔티티는 그대로 둔다`() {
        assertEquals("&#xZZ;", htmlToText("&#xZZ;"))
    }

    @Test
    fun `목록 항목은 가운뎃점으로 시작하는 한 줄`() {
        assertEquals("· 하나\n· 둘", htmlToText("<ul><li>하나</li><li class=\"x\">둘</li></ul>"))
    }

    @Test
    fun `끝이 말줄임이 아니면 그대로 둔다`() {
        assertEquals("완전한 문장이다.", dropTruncatedTail("완전한 문장이다."))
        assertEquals("· 하나\n· 둘", dropTruncatedTail("· 하나\n· 둘"))
    }

    @Test
    fun `한 줄 요약은 잘린 마지막 단어만 버린다`() {
        assertEquals("연합뉴스 요약이 여기서 … 원문에서 계속", dropTruncatedTail("연합뉴스 요약이 여기서 잘..."))
    }

    @Test
    fun `공백도 줄도 없는 말줄임은 내용을 버리지 않는다`() {
        assertEquals("짧은요약 … 원문에서 계속", dropTruncatedTail("짧은요약…"))
        assertEquals("…", dropTruncatedTail("…"))
    }
}
