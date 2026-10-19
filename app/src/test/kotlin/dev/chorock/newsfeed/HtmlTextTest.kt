package dev.chorock.newsfeed

import dev.chorock.newsfeed.util.htmlToText
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
}
