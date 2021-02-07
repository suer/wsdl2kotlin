package org.codefirst.wsdl2kotlin

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Path
import com.tickaroo.tikxml.annotation.Xml
import okio.Okio
import java.io.File

@Xml(name = "wsdl:service")
class WSDLService {
    @Attribute
    var name: String = ""
}

@Xml(name = "wsdl:definitions")
class WSDLDefinitions {
    @Element(name = "wsdl:service")
    lateinit var service: WSDLService
}

fun main() {
    // TODO; from args
    val path = WSDLDefinitions::class.java.getResource("/tempconvert.wsdl.xml").file
    println(path)

    val parser: TikXml = TikXml.Builder().exceptionOnUnreadXml(false).build()
    val buffer = Okio.buffer(Okio.source(File(path)))
    val wsdl = parser.read<WSDLDefinitions>(buffer, WSDLDefinitions::class.java)
    println(wsdl.service.name)
}
