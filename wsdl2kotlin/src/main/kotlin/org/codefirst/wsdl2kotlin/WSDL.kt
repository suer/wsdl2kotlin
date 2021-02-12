package org.codefirst.wsdl2kotlin

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml
import okio.Okio
import java.io.File

@Xml(name = "soap:address")
class SOAPAddress {
    @Attribute
    var location: String = ""
}

@Xml(name = "wsdl:port")
class WSDLPort {
    @Element
    var address: SOAPAddress? = null
}

@Xml(name = "wsdl:service")
class WSDLService {
    @Attribute
    var name: String = ""

    @Element
    var ports = mutableListOf<WSDLPort>()
}

@Xml(name = "wsdl:input")
class WSDLInput {
    @Attribute
    var message: String = ""
}

@Xml(name = "wsdl:output")
class WSDLOutput {
    @Attribute
    var message: String = ""
}

@Xml(name = "wsdl:operation")
class WSDLOperation {
    @Attribute
    var name: String = ""

    @Element
    lateinit var input: WSDLInput

    @Element
    lateinit var output: WSDLOutput
}

@Xml(name = "wsdl:portType")
class WSDLPortType {
    @Element
    var operations: MutableList<WSDLOperation> = mutableListOf()
}

@Xml(name = "s:sequence")
class XSDSequence {
    @Element
    var elements: MutableList<XSDElement> = mutableListOf()
}

@Xml(name = "s:complexType")
class XSDComplexType {
    @Attribute
    var name: String? = null

    @Element
    var sequence: XSDSequence? = null
}

@Xml(name = "s:element")
class XSDElement {
    @Element
    var complexType: XSDComplexType? = null

    @Attribute
    var name: String = ""

    @Attribute
    var type: String? = null

    @Attribute
    var minOccurs: Int? = null

    @Attribute
    var maxOccurs: String? = null

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

        var kotlinTypeName = ""
        if (type?.startsWith("s:") == true) {
            kotlinTypeName = when (type?.removePrefix("s:")) {
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
        if (type?.startsWith("tns:") == true) {
            kotlinTypeName = service.name + "_" + type?.removePrefix("tns:") ?: ""
        }
        return kotlinTypeName
    }
}

@Xml(name = "s:schema")
class XSDSchema {
    @Element
    var elements: MutableList<XSDElement> = mutableListOf()

    @Element
    var complexTypes: MutableList<XSDComplexType> = mutableListOf()
}

@Xml(name = "wsdl:types")
class WSDLTypes {
    @Element
    lateinit var schema: XSDSchema
}

@Xml(name = "wsdl:part")
class WSDLPart {
    @Attribute
    var name: String = ""

    @Attribute
    var element: String? = null

    @Attribute
    var type: String? = null
}

@Xml(name = "wsdl:message")
class WSDLMessage {
    @Attribute
    var name: String = ""

    @Element
    lateinit var part: WSDLPart
}

@Xml(name = "wsdl:definitions")
class WSDLDefinitions {
    @Element
    lateinit var service: WSDLService

    @Element
    var messages: MutableList<WSDLMessage> = mutableListOf()

    @Element
    var portTypes: MutableList<WSDLPortType> = mutableListOf()

    @Attribute(name = "xmlns:tns")
    var tns: String = ""

    @Element
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
            val parser: TikXml = TikXml.Builder().exceptionOnUnreadXml(false).build()
            val buffer = Okio.buffer(Okio.source(File(path)))
            return parser.read<WSDLDefinitions>(buffer, WSDLDefinitions::class.java)
        }
    }
}
