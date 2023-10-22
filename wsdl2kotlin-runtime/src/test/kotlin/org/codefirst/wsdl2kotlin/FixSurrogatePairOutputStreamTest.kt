package org.codefirst.wsdl2kotlin

import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class FixSurrogatePairOutputStreamTest {
    private fun assertStream(
        input: String,
        output: String,
    ) {
        val byteStream = ByteArrayOutputStream()
        val stream = FixSurrogatePairOutputStream(byteStream)
        stream.bufferedWriter().use {
            it.write(input)
        }
        assertEquals(output, byteStream.toString())
    }

    @Test
    fun testSimpleString() {
        assertStream("Hello World", "Hello World")
    }

    @Test
    fun testSurrogatePairString() {
        assertStream("&#55360;&#56331;", "&#131083;")
    }

    @Test
    fun testSuspiciousString1() {
        assertStream("&amp;", "&amp;")
    }

    @Test
    fun testSuspiciousString2() {
        assertStream("&#38;", "&#38;")
    }

    @Test
    fun testSuspiciousString3() {
        assertStream(";", ";")
    }

    @Test
    fun testSuspiciousString4() {
        assertStream("&", "&")
    }
}
