package org.codefirst.wsdl2kotlin

import kotlin.test.Test
import kotlin.test.assertEquals

class XSDTest {
    @Test
    fun testParse() {
        val path = this::class.java.getResource("/weatherforecast.xsd.xml").file
        val xsd = XSD.parse(path)
        assertEquals(5, xsd.elements.size)
    }

    @Test
    fun testIsXSDTrue() {
        val path = this::class.java.getResource("/weatherforecast.xsd.xml").file
        assertEquals(true, XSD.isXSD(path))
    }

    @Test
    fun testIsXSDFalse() {
        val path = this::class.java.getResource("/weatherforecast.wsdl.xml").file
        assertEquals(false, XSD.isXSD(path))
    }
}
