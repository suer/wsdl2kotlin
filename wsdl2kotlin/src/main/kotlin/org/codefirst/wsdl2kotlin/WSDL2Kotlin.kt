package org.codefirst.wsdl2kotlin

fun main() {
    // TODO: from args
    val path = WSDLDefinitions::class.java.getResource("/tempconvert.wsdl.xml").file

    val wsdl = WSDL.parse(path)
    var kotlin = """
class ${wsdl.service.name}(val endpoint : String) : WSDLService {
"""
    wsdl.portTypes.forEach { portType ->
        portType.operations.forEach { operation ->
            val inputType = "${wsdl.service.name}_${operation.input.message.removePrefix("tns:")}"
            val outputType = "${wsdl.service.name}_${operation.output.message.removePrefix("tns:")}"
            kotlin += """
    fun request(parameters : $inputType) : $outputType {
        return requestGeneric<$outputType>(parameters)
    }
"""
        }
    }

    kotlin += """
}
"""
    // TODO: write to file
    println(kotlin)
}
