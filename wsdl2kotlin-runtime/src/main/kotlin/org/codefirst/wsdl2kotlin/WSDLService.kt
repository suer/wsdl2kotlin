package org.codefirst.wsdl2kotlin

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class XMLParam(
    val namespace: String,
    val name: String,
    val value: Any?
)

abstract class XSDType {
    abstract fun xmlParams(): Array<XMLParam>

    fun soapRequest(tns: String): Document {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val envelopeElement = document.createElement("S:Envelope")
        envelopeElement.setAttribute("xmlns:S", "http://schemas.xmlsoap.org/soap/envelope/")
        envelopeElement.setAttribute("xmlns:tns", tns)
        document.appendChild(envelopeElement)

        val headerElement = document.createElement("S:Header")
        envelopeElement.appendChild(headerElement)

        val bodyElement = document.createElement(("S:Body"))
        envelopeElement.appendChild(bodyElement)

        xmlElements("tns:${this.javaClass.simpleName.split('_').last()}", document)
            .forEach { bodyElement.appendChild(it) }

        return document
    }

    fun xmlElements(name: String, document: Document): Array<Element> {
        val typeElement = document.createElement(name)

        xmlParams().forEach() { param ->
            val name = if (param.namespace.isBlank()) { param.name } else { "${param.namespace}:${param.name}" }
            param.value?.xmlElements(name, document)?.forEach {
                typeElement.appendChild(it)
            }
        }

        return arrayOf(typeElement)
    }
}

fun Any?.xmlElements(name: String, document: Document): Array<Element> {
    val element = document.createElement(name)
    element.textContent = this.toString() // TODO: process by Type
    return arrayOf(element)
}

abstract class WSDLService(
    // TODO: Intercepter
) {
    abstract val targetNamespace: String
    abstract var endpoint: String
    abstract var path: String

    protected fun <I : XSDType, O : XSDType> requestGeneric(i: I): O {

        val soapRequest = i.soapRequest(targetNamespace)
        println(soapRequest.dump()) // TODO: remove this line

        val request = Request.Builder()
            .url(endpoint + "/" + path)
            .post(soapRequest.dump().toRequestBody("text/xml".toMediaTypeOrNull()))
            .build()
        val client = OkHttpClient.Builder().build()
        val response = client.newCall(request).execute()

        println(response.body?.string()) // TODO: remove this line

        // TODO

        return "" as O
    }
}

fun Document.dump(): String {
    val writer = StringWriter()
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.transform(DOMSource(this), StreamResult(writer))
    return writer.toString()
}

// fun main() {
//    val req = TempConvert_FahrenheitToCelsius("100")
//    val res = TempConvert().also { it.endpoint = "https://www.w3schools.com/xml" }.request(req)
// }
