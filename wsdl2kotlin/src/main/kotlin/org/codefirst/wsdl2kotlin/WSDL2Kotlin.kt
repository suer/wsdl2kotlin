package org.codefirst.wsdl2kotlin

class WSDL2Kotlin() {
    fun run(vararg paths: String): String {
        var kotlin = """
import org.codefirst.wsdl2kotlin.WSDLService
import org.codefirst.wsdl2kotlin.XMLParam
import org.codefirst.wsdl2kotlin.XSDType
"""
        paths.map { WSDL.parse(it) }.forEach { wsdl ->

            kotlin += """
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
) : XSDType() {
    override fun xmlParams(): Array<XMLParam> {
        return arrayOf("""
                element.complexType?.sequences?.forEach { sequence ->
                    sequence.elements.forEach { element2 ->
                        // TODO: tns or empty
                        kotlin += """
                XMLParam("tns", "${element2.name}", ${element2.name}),
"""
                    }
                }
                kotlin += """
        )
    }
}
"""
            }
        }
        return kotlin
    }
}
