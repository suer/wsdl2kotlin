package org.codefirst.wsdl2kotlin

data class XMLParam(
        val namespace: String,
        val name: String,
        val value: Any?,
)

abstract class XSDType {
    abstract fun xmlParams(): Array<XMLParam>
}

open class WSDLService {
    fun <I : XSDType, O : XSDType> requestGeneric(i: I): O {
        // TODO
        return "" as O
    }
}