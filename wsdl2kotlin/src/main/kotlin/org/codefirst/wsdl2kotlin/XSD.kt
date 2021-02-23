package org.codefirst.wsdl2kotlin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.io.File

class XSDSequence {
    @JacksonXmlElementWrapper(localName = "element", useWrapping = false)
    @JacksonXmlProperty(localName = "element")
    var elements: MutableList<XSDElement> = mutableListOf()
}

class XSDComplexType {
    @JacksonXmlProperty(isAttribute = true)
    var name: String? = null

    var sequence: XSDSequence? = null
}

class XSDElement {
    var complexType: XSDComplexType? = null

    @JacksonXmlProperty(isAttribute = true)
    var name: String = ""

    @JacksonXmlProperty(isAttribute = true)
    var type: String? = null

    @JacksonXmlProperty(isAttribute = true)
    var minOccurs: Int? = null

    @JacksonXmlProperty(isAttribute = true)
    var maxOccurs: String? = null

    val safeName: String?
        get() {
            // https://kotlinlang.org/docs/keyword-reference.html#soft-keywords
            val keywords = arrayListOf("return", "operator", "var", "val", "out")
            if (keywords.contains(name)) {
                return "`$name`"
            }
            return name
        }

    fun typeInKotlin(service: WSDLService): String? {
        val kotlinTypeName = baseTypeInKotlin(service)

        if (maxOccurs == "unbounded") {
            return "Array<$kotlinTypeName>"
        }
        if (minOccurs == 0) {
            return "$kotlinTypeName?"
        }

        return kotlinTypeName
    }

    fun kclassInKotlin(service: WSDLService): String? {
        val kotlinTypeName = baseTypeInKotlin(service)

        if (maxOccurs == "unbounded") {
            return "Array<$kotlinTypeName>::class"
        }

        return "$kotlinTypeName::class"
    }

    fun initialValue(service: WSDLService): String {
        val kotlinTypeName = baseTypeInKotlin(service)

        if (maxOccurs == "unbounded") {
            return "emptyArray<$kotlinTypeName>()"
        }

        if (minOccurs == 0) {
            return "null"
        }

        return when (kotlinTypeName) {
            "String" -> "\"\""
            "Boolean" -> "false"
            "Byte" -> "0"
            "Int" -> "0"
            "Float" -> "0F"
            "Long" -> "0L"
            "Date" -> "java.util.Date()"
            "ByteArray" -> "ByteArray(0)"
            else -> "$kotlinTypeName()"
        }
    }

    private fun baseTypeInKotlin(service: WSDLService): String? {
        if (type == null) {
            return null
        }

        if (type?.startsWith("tns:") == true) {
            return service.name + "_" + type?.removePrefix("tns:") ?: ""
        }

        return when (type?.substringAfterLast(":")) {
            "string" -> "String"
            "boolean" -> "Boolean"
            "byte" -> "Byte"
            "int" -> "Int"
            "float" -> "Float"
            "long" -> "Long"
            "dateTime" -> "java.util.Date"
            "base64Binary" -> "ByteArray"
            else -> ""
        }
    }
}

@JacksonXmlRootElement(localName = "schema")
class XSDSchema {
    @JacksonXmlElementWrapper(localName = "element", useWrapping = false)
    @JacksonXmlProperty(localName = "element")
    var elements: MutableList<XSDElement> = mutableListOf()
        set(value) {
            // workaround for https://github.com/FasterXML/jackson-dataformat-xml/issues/275
            elements.addAll(value)
        }

    @JacksonXmlElementWrapper(localName = "complexType", useWrapping = false)
    @JacksonXmlProperty(localName = "complexType")
    var complexTypes: MutableList<XSDComplexType> = mutableListOf()
        set(value) {
            // workaround for https://github.com/FasterXML/jackson-dataformat-xml/issues/275
            complexTypes.addAll(value)
        }
}

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
