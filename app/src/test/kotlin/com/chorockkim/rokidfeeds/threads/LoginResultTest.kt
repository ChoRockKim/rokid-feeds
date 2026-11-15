package com.chorockkim.rokidfeeds.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginResultTest {
    @Test
    fun `성공·2단계면 이유 없음, 캡차·본인 확인·오류는 그 이유`() {
        assertNull(LoginResult.parse("""{"auth":true}""")!!.reason())
        assertNull(LoginResult.parse("""{"auth":false,"twofa":true}""")!!.reason())
        assertTrue(LoginResult.parse("""{"auth":false,"captcha":true}""")!!.reason()!!.contains("캡차"))
        assertTrue(LoginResult.parse("""{"auth":false,"redirect":"/challenge/abc/"}""")!!.reason()!!.contains("본인 확인"))
        assertEquals("인스타 응답: 400 · bad", LoginResult.parse("""{"auth":false,"code":"400","msg":"bad"}""")!!.reason())
        assertTrue(LoginResult.parse("""{"auth":false,"code":null,"msg":null}""")!!.reason()!!.contains("이유 없음"))
        assertNull(LoginResult.parse("nope"))
        assertTrue(LoginResult.parse("""{"auth":false,"code":"1348009","msg":"{\"title\":\"x\"}"}""")!!.reason()!!.contains("캡차"))
    }
}
