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

@Xml(name = "wsdl:definitions")
class WSDLDefinitions {
    @Element(name = "wsdl:service")
    lateinit var service: WSDLService

    @Element
    var portTypes: MutableList<WSDLPortType> = mutableListOf()
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
