package org.codefirst.wsdl2kotlin

class WSDL2Kotlin() {
    fun run(vararg paths: String): Array<Output> {

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

        return wsdls.map {
            generateOutput(it)
        }.toTypedArray()
    }

    private fun generateOutput(wsdl: WSDLDefinitions): Output {
        var kotlin = """
package ${wsdl.packageName}
"""
        kotlin += """
import org.codefirst.wsdl2kotlin.WSDLService
import org.codefirst.wsdl2kotlin.XMLParam
import org.codefirst.wsdl2kotlin.XSDType
import org.w3c.dom.Element
"""

        val location = wsdl.service.ports.first { it.address != null }.address?.location
        val endpoint = location?.substringBeforeLast("/")
        val path = location?.substringAfterLast("/")

        kotlin += """
class ${wsdl.service.name} : WSDLService() {
    override val targetNamespace = "${wsdl.targetNamespace}"
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
            kotlin += generateType(complexType.name ?: "", wsdl, complexType, "")
        }

        wsdl.types.schema.elements.filter { it.complexType != null }.forEach { element ->
            kotlin += generateType(element.name, wsdl, element.complexType, "tns")
        }

        return Output(wsdl.service.name, wsdl.packageName, kotlin)
    }

    private fun generateType(name: String, wsdl: WSDLDefinitions, complexType: XSDComplexType?, namespace: String): String {
        var kotlin = ""

        val subClassSequence = complexType?.complexContent?.extension?.sequence
        if (subClassSequence != null) {
            complexType.sequence = subClassSequence
        }

        complexType?.sequence?.elements?.filter { it.type == null }?.forEach {
            it.complexType?.name = "${complexType.name}_${it.name}"
            kotlin += generateType("${name}_${it.name}", wsdl, it.complexType, namespace)
        }

        kotlin += """
${if (complexType?.isFinal == true) { "" } else { "open "}}class ${wsdl.service.name}_$name : ${complexType?.baseTypeInKotlin(wsdl.service) ?: "XSDType"}() {"""
        complexType?.sequence?.elements?.forEach {
            kotlin += """
    var ${it.safeName}: ${it.typeInKotlin(wsdl.service, complexType)} = ${it.initialValue(wsdl.service, complexType)}"""
        }

        kotlin += """

    override fun xmlParams(): Array<XMLParam> {
        return ${if (complexType?.isExtended == true) { "super.xmlParams() + " } else { "" }}arrayOf("""
        complexType?.sequence?.elements?.forEach {
            kotlin += """
                XMLParam("$namespace", "${it.name}", ${it.safeName}, ${it.kclassInKotlin(wsdl.service, complexType)}),"""
        }
        kotlin += """
        )
    }

    override fun readSOAPEnvelope(bodyElement: Element) {"""
        if (complexType?.isExtended == true) {
            kotlin += """
        super.readSOAPEnvelope(bodyElement)"""
        }
        complexType?.sequence?.elements?.forEach {
            kotlin += """
        ${it.safeName} = ${it.readMethod()}(bodyElement, "${it.name}", ${it.kclassInKotlin(wsdl.service, complexType)})"""
        }
        kotlin += """
    }
}
"""
        return kotlin
    }
}
