package org.codefirst.wsdl2kotlin

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml
import okio.Okio
import java.io.File

@Xml(name = "wsdl:service")
class WSDLService {
    @Attribute
    var name: String = ""
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
    @Element
    var sequences: MutableList<XSDSequence> = mutableListOf()
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
    var maxOccurs: Int? = null

    fun typeInKotlin(): String? {
        return when (type) {
            null -> null
            else -> {
                return when (type?.removePrefix("s:")) {
                    "string" -> "String"
                    "boolean" -> "Boolean"
                    "int" -> "Int"
                    "long" -> "Long"
                    "dateTime" -> "Date"
                    "base64Binary" -> "byte[]" // TODO
                    else -> ""
                } + when (minOccurs) {
                    // TODO: array
                    0 -> "?"
                    else -> ""
                }
            }
        }
    }
}

@Xml(name = "s:schema")
class XSDSchema {
    @Element
    var elements: MutableList<XSDElement> = mutableListOf()
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
