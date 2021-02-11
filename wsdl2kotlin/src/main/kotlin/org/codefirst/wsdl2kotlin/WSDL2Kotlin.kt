package org.codefirst.wsdl2kotlin

class WSDL2Kotlin() {
    fun run(vararg paths: String): String {
        var kotlin = """
import org.codefirst.wsdl2kotlin.WSDLService
import org.codefirst.wsdl2kotlin.XMLParam
import org.codefirst.wsdl2kotlin.XSDType
"""
        val wsdls = mutableListOf<WSDLDefinitions>()
        paths.forEach {
            if (XSD.isXSD(it)) {
                val xsd = XSD.parse(it)
                wsdls.last().types.schema.elements.addAll(xsd.elements)
                wsdls.last().types.schema.complexTypes.addAll(xsd.complexTypes)
            } else {
                wsdls.add(WSDL.parse(it))
            }
        }

        wsdls.forEach { wsdl ->
            val location = wsdl.service.ports.first { it.address != null }.address?.location
            val endpoint = location?.substringBeforeLast("/")
            val path = location?.substringAfterLast("/")

            kotlin += """
class ${wsdl.service.name} : WSDLService() {
    override val targetNamespace = "${wsdl.tns}"
    override var endpoint = "$endpoint"
    override var path = "$path"
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
            wsdl.types.schema.complexTypes.forEach { complexType ->
                kotlin += generateType(complexType?.name ?: "", wsdl, complexType, "")
            }

            wsdl.types.schema.elements.filter { it.complexType != null }.forEach { element ->
                kotlin += generateType(element.name, wsdl, element.complexType, "tns")
            }
        }
        return kotlin
    }

    private fun generateType(name: String, wsdl: WSDLDefinitions, complexType: XSDComplexType?, namespace: String): String {
        var kotlin = """
class ${wsdl.service.name}_$name ("""
        complexType?.sequence?.elements?.forEach {
            kotlin += """
    var ${it.name}: ${it.typeInKotlin(wsdl.service)},"""
        }

        kotlin += """
) : XSDType() {
    override fun xmlParams(): Array<XMLParam> {
        return arrayOf("""
        complexType?.sequence?.elements?.forEach {
            kotlin += """
                XMLParam("$namespace", "${it.name}", ${it.name}),"""
        }
        kotlin += """
        )
    }
}
"""
        return kotlin
    }
}
