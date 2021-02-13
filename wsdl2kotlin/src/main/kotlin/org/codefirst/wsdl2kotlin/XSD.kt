package org.codefirst.wsdl2kotlin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.File

class XSD {
    companion object {
        fun parse(path: String): XSDSchema {
            val xmlMapper = XmlMapper()
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            return xmlMapper.readValue(File(path), XSDSchema::class.java)
        }

        fun isXSD(path: String): Boolean {
            val xsd = parse(path)
            return xsd.elements.any()
        }
    }
}
