package org.codefirst.wsdl2kotlin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.io.File
import java.net.URI

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
    lateinit var input: WSDLInput

    lateinit var output: WSDLOutput
}

class WSDLPortType {
    @JacksonXmlElementWrapper(localName = "operation", useWrapping = false)
    @JacksonXmlProperty(localName = "operation")
    var operations: MutableList<WSDLOperation> = mutableListOf()
}

class WSDLTypes {
    lateinit var schema: XSDSchema
}

class WSDLPart {
    @JacksonXmlProperty(isAttribute = true)
    var element: String? = null
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

    val packageName: String
        get() = URI(targetNamespace).host.split('.').reversed().joinToString(".")
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
