package org.codefirst.wsdl2kotlin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.io.File

class SOAPAddress {
    @JacksonXmlProperty(isAttribute = true)
    var location: String = ""
}

class WSDLPort {
    var address: SOAPAddress? = null
}

class WSDLService {
    @JacksonXmlProperty(isAttribute = true)
    var name: String = ""

    @JacksonXmlElementWrapper(localName = "port", useWrapping = false)
    @JacksonXmlProperty(localName = "port")
    var ports = mutableListOf<WSDLPort>()
}

class WSDLInput {
    @JacksonXmlProperty(isAttribute = true)
    var message: String = ""
}

class WSDLOutput {
    @JacksonXmlProperty(isAttribute = true)
    var message: String = ""
}

class WSDLOperation {
    @JacksonXmlProperty(isAttribute = true)
    var name: String = ""

    lateinit var input: WSDLInput

    lateinit var output: WSDLOutput
}

class WSDLPortType {
    @JacksonXmlElementWrapper(localName = "operation", useWrapping = false)
    @JacksonXmlProperty(localName = "operation")
    var operations: MutableList<WSDLOperation> = mutableListOf()
}

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

class WSDLTypes {
    lateinit var schema: XSDSchema
}

class WSDLPart {
    @JacksonXmlProperty(isAttribute = true)
    var name: String = ""

    @JacksonXmlProperty(isAttribute = true)
    var element: String? = null

    @JacksonXmlProperty(isAttribute = true)
    var type: String? = null
}

class WSDLMessage {
    @JacksonXmlProperty(isAttribute = true)
    var name: String = ""

    lateinit var part: WSDLPart
}

@JacksonXmlRootElement(localName = "definitions")
class WSDLDefinitions {
    lateinit var service: WSDLService

    @JacksonXmlElementWrapper(localName = "message", useWrapping = false)
    @JacksonXmlProperty(localName = "message")
    var messages: MutableList<WSDLMessage> = mutableListOf()

    @JacksonXmlElementWrapper(localName = "portType", useWrapping = false)
    @JacksonXmlProperty(localName = "portType")
    var portTypes: MutableList<WSDLPortType> = mutableListOf()

    @JacksonXmlProperty(isAttribute = true)
    var targetNamespace: String = ""

    lateinit var types: WSDLTypes

    fun findType(message: String): String? {
        val name = message.removePrefix("tns:")
        val part = messages.first { it.name == name }.part

        if (part.element == null) {
            return null
        }

        val elementName = part.element?.removePrefix("tns:")
        if (!types.schema.elements.any { it.name == elementName }) {
            return null
        }

        return "${service.name}_$elementName"
    }
}

class WSDL {
    companion object {
        fun parse(path: String): WSDLDefinitions {
            val xmlMapper = XmlMapper()
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            return xmlMapper.readValue(File(path), WSDLDefinitions::class.java)
        }
    }
}
