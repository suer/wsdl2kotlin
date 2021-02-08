package org.codefirst.wsdl2kotlin

fun main() {
    // TODO: from args
    val path = WSDLDefinitions::class.java.getResource("/tempconvert.wsdl.xml").file

    val wsdl = WSDL.parse(path)
    var kotlin = """
import org.codefirst.wsdl2kotlin.WSDLService

class ${wsdl.service.name}(val endpoint: String) : WSDLService() {
"""
    wsdl.portTypes.forEach { portType ->
        portType.operations.forEach { operation ->
            val inputType = wsdl.findType(operation.input.message)
            val outputType = wsdl.findType(operation.output.message)
            if (inputType != null && outputType != null) {
                kotlin += """
    fun request(parameters: $inputType): $outputType {
        return requestGeneric<$inputType, $outputType>(parameters)
    }
"""
            }
        }
    }

    kotlin += """
}
"""

    wsdl.types.schema.elements.filter { it.complexType != null }.forEach { element ->
        kotlin += """
class ${wsdl.service.name}_${element.name} ("""
        element.complexType?.sequences?.forEach { sequence ->
            sequence.elements.forEach { element2 ->
                kotlin += """
    var ${element2.name}: ${element2.typeInKotlin()},"""
            }
        }

        kotlin += """
)
"""
    }

    // TODO: write to file
    println(kotlin)
}
