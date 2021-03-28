package org.codefirst.wsdl2kotlin

import kotlin.test.Test
import kotlin.test.assertEquals

class WSDLTest {
    @Test
    fun testParse() {
        val path = this::class.java.getResource("/tempconvert.wsdl.xml").file
        val wsdl = WSDL.parse(path)
        assertEquals(wsdl.service.name, "TempConvert")
    }

    @Test
    fun testPackageName() {
        val path = this::class.java.getResource("/tempconvert.wsdl.xml").file
        val wsdl = WSDL.parse(path)
        assertEquals(wsdl.packageName, "com.w3schools.www")
    }
}
