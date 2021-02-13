package org.codefirst.wsdl2kotlin

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
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

const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX:00"

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

    abstract fun readSOAPEnvelope(bodyElement: Element)

    protected inline fun <reified O> readSOAPEnvelopeField(bodyElement: Element, name: String, field: Any?): O {
        val item = bodyElement.getElementsByTagName(name).item(0)
        return when (field) {
            is String -> item.textContent
            is Boolean -> item.textContent.equals("true", ignoreCase = true)
            is Integer -> item.textContent.toInt()
            is Float -> item.textContent.toFloat()
            is Long -> item.textContent.toLong()
            is java.util.Date -> SimpleDateFormat(DATETIME_FORMAT).parse(item.textContent)
            // TODO: process by Type
            else -> null
        } as O
    }
}

fun Any?.xmlElements(name: String, document: Document): Array<Element> {
    val element = document.createElement(name)
    when (this) {
        is Date -> element.textContent = SimpleDateFormat(DATETIME_FORMAT).format(this)
        else -> element.textContent = this.toString() // TODO: process by Type
    }

    return arrayOf(element)
}

abstract class WSDLService(
    // TODO: Intercepter
) {
    abstract val targetNamespace: String
    abstract var endpoint: String
    abstract var path: String

    protected inline fun <I : XSDType, reified O : XSDType> requestGeneric(i: I): O {

        val soapRequest = i.soapRequest(targetNamespace)
        println(soapRequest.dump()) // TODO: remove this line

        val request = Request.Builder()
            .url(endpoint + "/" + path)
            .post(soapRequest.dump().toRequestBody("text/xml".toMediaTypeOrNull()))
            .build()
        val client = OkHttpClient.Builder().build()
        val response = client.newCall(request).execute()

        val responseBody = response.body?.string()
        println(responseBody) // TODO: remove this line

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(responseBody?.byteInputStream())
        var bodyElement = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0) as Element

        val o = O::class.java.newInstance()
        o.readSOAPEnvelope(bodyElement)
        return o
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
//    val req = TempConvert_FahrenheitToCelsius()
//    req.Fahrenheit = "100"
//    val res = TempConvert().also { it.endpoint = "https://www.w3schools.com/xml" }.request(req)
//    println(res.FahrenheitToCelsiusResult)
// }
