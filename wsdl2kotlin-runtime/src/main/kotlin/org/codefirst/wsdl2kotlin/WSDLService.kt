package org.codefirst.wsdl2kotlin

import okhttp3.Interceptor
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

class SOAPFaultException(private val faultString: String) : RuntimeException(faultString)

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

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: String?): String {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return item.textContent
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: Boolean?): Boolean {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return item.textContent.equals("true", ignoreCase = true)
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: Int?): Int {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return item.textContent.toInt()
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: Float?): Float {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return item.textContent.toFloat()
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: Long?): Long {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return item.textContent.toLong()
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: java.util.Date?): java.util.Date {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return SimpleDateFormat(DATETIME_FORMAT).parse(item.textContent)
    }

    protected fun readSOAPEnvelopeField(parentElement: Element, tagName: String, field: ByteArray?): ByteArray {
        val item = parentElement.getElementsByTagName(tagName).item(0)
        return java.util.Base64.getDecoder().decode(item.textContent)
    }

    protected inline fun <reified T> readSOAPEnvelopeField(parentElement: Element, tagName: String, field: Array<T>): Array<T> {
        val items = parentElement.getElementsByTagName(tagName)
        val list = mutableListOf<T>()
        for (i in 0 until items.length) {
            val item = items.item(i)
            val value = when (val value = field.getOrNull(0)) {
                is String? -> item.textContent
                is Boolean? -> item.textContent.equals("true", ignoreCase = true)
                is Int? -> item.textContent.toInt()
                is Float? -> item.textContent.toFloat()
                is Long? -> item.textContent.toLong()
                is java.util.Date? -> SimpleDateFormat(DATETIME_FORMAT).parse(item.textContent)
                is ByteArray? -> java.util.Base64.getDecoder().decode(item.textContent)
                is XSDType? -> NotImplementedError() // TODO
                else -> null
            } as T
            list.add(value)
        }
        return list.toTypedArray()
    }

    protected fun <T : XSDType?> readSOAPEnvelopeField(bodyElement: Element, name: String, field: T): T {
        throw NotImplementedError() // TODO
    }
}

fun Any?.xmlElements(name: String, document: Document): Array<Element> {
    val element = document.createElement(name)
    when (this) {
        is Date -> element.textContent = SimpleDateFormat(DATETIME_FORMAT).format(this)
        is ByteArray -> java.util.Base64.getEncoder().encodeToString(this)
        is Array<*> -> return this.map { it.xmlElements(name, document).first() }.toTypedArray()
        is XSDType -> {
            this.xmlParams().forEach { param ->
                param.value.xmlElements(param.name, document).forEach { childElement ->
                    element.appendChild(childElement)
                }
            }
        }
        else -> element.textContent = this.toString() // TODO: process by Type
    }

    return arrayOf(element)
}

abstract class WSDLService() {
    abstract val targetNamespace: String
    abstract var endpoint: String
    abstract var path: String

    protected val interceptors = mutableListOf<Interceptor>()

    protected inline fun <I : XSDType, reified O : XSDType> requestGeneric(i: I): O {

        val soapRequest = i.soapRequest(targetNamespace)

        val request = Request.Builder()
            .url(endpoint + "/" + path)
            .post(soapRequest.dump().toRequestBody("text/xml".toMediaTypeOrNull()))
            .build()
        val client = OkHttpClient.Builder()
            .also { builder ->
                interceptors.forEach {
                    builder.addInterceptor(it)
                }
            }
            .build()
        val response = client.newCall(request).execute()

        val responseBody = response.body?.string()

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(responseBody?.byteInputStream())
        var bodyElement = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0) as Element

        val fault = bodyElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Fault").item(0) as? Element
        if (fault != null) {
            val faultString = fault.getElementsByTagName("faultstring").item(0).textContent
            throw SOAPFaultException(faultString)
        }

        val o = O::class.java.newInstance()
        o.readSOAPEnvelope(bodyElement)
        return o
    }

    fun addInterceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
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
