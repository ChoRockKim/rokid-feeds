package com.chorockkim.rokidfeeds

import com.chorockkim.rokidfeeds.util.stripEmoji
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiTest {
    @Test
    fun `이모지·변형 선택자·결합 문자를 빼고 겹친 공백을 줄인다`() {
        assertEquals("좋아요 최고", stripEmoji("좋아요 🔥🔥 최고"))
        assertEquals("가족", stripEmoji("가족 👨‍👩‍👧"))
        assertEquals("하트", stripEmoji("하트 ❤️"))
        assertEquals("한글 漢字 · 문장부호!?", stripEmoji("한글 漢字 · 문장부호!?"))
        assertEquals("첫 줄\n둘째 줄", stripEmoji("첫 줄 😀\n둘째 줄"))
    }
}
