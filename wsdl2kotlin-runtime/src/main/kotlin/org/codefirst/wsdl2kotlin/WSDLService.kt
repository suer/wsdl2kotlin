package org.codefirst.wsdl2kotlin

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.text.SimpleDateFormat
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

data class XMLParam(
    val namespace: String,
    val name: String,
    val value: Any?,
    val clazz: KClass<*>
)

class SOAPFaultException(faultString: String) : RuntimeException(faultString)

const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX:00"

class DocumentHelper {
    companion object {
        fun newDocumentBuilder(): DocumentBuilder {
            return DocumentBuilderFactory.newInstance().apply {
                this.isNamespaceAware = true
            }.newDocumentBuilder()
        }

        fun newTransformer(): Transformer {
            return TransformerFactory.newInstance().newTransformer().apply {
                this.setOutputProperty(OutputKeys.INDENT, "yes")
            }
        }
    }
}

abstract class XSDType {
    abstract fun xmlParams(): Array<XMLParam>

    fun soapRequest(tns: String): Document {
        val document = DocumentHelper.newDocumentBuilder().newDocument()

        val envelopeElement = document.createElement("S:Envelope")
        envelopeElement.setAttribute("xmlns:S", "http://schemas.xmlsoap.org/soap/envelope/")
        envelopeElement.setAttribute("xmlns:tns", tns)
        document.appendChild(envelopeElement)

        val headerElement = document.createElement("S:Header")
        envelopeElement.appendChild(headerElement)

        val bodyElement = document.createElement("S:Body")
        envelopeElement.appendChild(bodyElement)

        xmlElements("tns:${this.javaClass.simpleName.split('_').last()}", document)
            .forEach { bodyElement.appendChild(it) }

        return document
    }

    private fun xmlElements(name: String, document: Document): Array<Element> {
        val typeElement = document.createElement(name)

        xmlParams().forEach { param ->
            val name = if (param.namespace.isBlank()) {
                param.name
            } else {
                "${param.namespace}:${param.name}"
            }
            xmlElements(param.value, name, document).forEach {
                typeElement.appendChild(it)
            }
        }

        return arrayOf(typeElement)
    }

    private fun xmlElements(value: Any?, name: String, document: Document): Array<Element> {
        val element = document.createElement(name)
        when (value) {
            is java.util.Date -> element.textContent = SimpleDateFormat(DATETIME_FORMAT).format(value)
            is ByteArray -> element.textContent = java.util.Base64.getEncoder().encodeToString(value)
            is Array<*> -> return value.map { xmlElements(it, name, document).first() }.toTypedArray()
            is XSDType -> {
                value.xmlParams().forEach { param ->
                    xmlElements(param.value, param.name, document).forEach { childElement ->
                        element.appendChild(childElement)
                    }
                }
            }
            null -> return arrayOf()
            else -> element.textContent = value.toString()
        }

        return arrayOf(element)
    }

    abstract fun readSOAPEnvelope(bodyElement: Element)

    protected fun <T : Any> readSOAPEnvelopeField(parentElement: Element, tagName: String, clazz: KClass<T>): T {
        return readSOAPEnvelopeFieldNullable(parentElement, tagName, clazz)!!
    }

    private fun <T : Any> isSingleType(clazz: KClass<T>): Boolean {
        return when (clazz) {
            String::class, Boolean::class, Int::class, Float::class, Long::class, java.util.Date::class, ByteArray::class -> true
            else -> false
        }
    }

    protected fun <T : Any> readSOAPEnvelopeFieldNullable(parentElement: Element, tagName: String, clazz: KClass<T>): T? {
        val items = getChildElementsByTagName(parentElement, tagName)
        if (items.isEmpty()) {
            return null
        }

        if (isSingleType(clazz)) {
            return singleNodeToObject(items.first(), clazz)
        }

        if (clazz != ByteArray::class && clazz.java.isArray) {
            val k = clazz.java.componentType.kotlin

            if (isSingleType(k)) {
                val array = items.map { singleNodeToObject(it, k) }.toTypedArray()
                return when (k) {
                    String::class -> array.map { it as String }.toTypedArray()
                    Boolean::class -> array.map { it as Boolean }.toTypedArray()
                    Int::class -> array.map { it as Int }.toTypedArray()
                    Float::class -> array.map { it as Float }.toTypedArray()
                    Long::class -> array.map { it as Long }.toTypedArray()
                    java.util.Date::class -> array.map { it as java.util.Date }.toTypedArray()
                    ByteArray::class -> array.map { it as ByteArray }.toTypedArray()
                    else -> array
                } as T
            }

            val arr = java.lang.reflect.Array.newInstance(k.java, items.size)
            items.forEachIndexed { i, item ->
                java.lang.reflect.Array.set(arr, i, singleNodeToObject(item, k))
            }
            return arr as T
        }

        val t = clazz.java.newInstance() as? XSDType
            ?: throw NotImplementedError("Unsupported type: ${clazz.simpleName}")
        val properties = t.javaClass.kotlin.memberProperties

        val item = items.first()

        properties.filterIsInstance<KMutableProperty<*>>().forEach { p ->
            val param = t.xmlParams().first { p.name == it.name }

            val v = readSOAPEnvelopeFieldNullable(item, param.name, param.clazz)

            p.setter.call(t, v)
        }

        return t as T
    }

    private fun getChildElementsByTagName(parentElement: Element, tagName: String): List<Element> {
        val items = parentElement.childNodes
        val nodes = mutableListOf<Element>()
        for (i in 0 until items.length) {
            val item = items.item(i)
            if (item.localName == tagName) {
                nodes.add(item as Element)
            }
        }
        return nodes
    }

    private fun <T : Any> singleNodeToObject(item: Node, clazz: KClass<T>): T {
        return when (clazz) {
            String::class -> item.textContent
            Boolean::class -> item.textContent.equals("true", ignoreCase = true)
            Int::class -> item.textContent.toInt()
            Float::class -> item.textContent.toFloat()
            Long::class -> item.textContent.toLong()
            java.util.Date::class -> SimpleDateFormat(DATETIME_FORMAT).parse(item.textContent)
            ByteArray::class -> java.util.Base64.getDecoder().decode(item.textContent)
            else -> {
                val t = clazz.java.newInstance() as? XSDType
                    ?: throw NotImplementedError("Unsupported type: ${clazz.simpleName}")
                t.readSOAPEnvelope(item as Element)
                return t as T
            }
        } as T
    }
}

abstract class WSDLService() {
    abstract val targetNamespace: String
    abstract var endpoint: String
    abstract var path: String

    protected val interceptors = mutableListOf<Interceptor>()

    protected inline fun <I : XSDType, reified O : XSDType> requestGeneric(i: I): O {

        val soapRequest = i.soapRequest(targetNamespace)

        val requestBody = object : RequestBody() {
            override fun contentType() = "text/xml".toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                DocumentHelper.newTransformer().transform(
                    DOMSource(soapRequest),
                    StreamResult(FixSurrogatePairOutputStream(sink.outputStream()))
                )
            }
        }

        val request = Request.Builder()
            .url("$endpoint/$path")
            .post(requestBody)
            .build()
        val client = OkHttpClient.Builder()
            .also { builder ->
                interceptors.forEach {
                    builder.addInterceptor(it)
                }
            }
            .build()
        val response = client.newCall(request).execute()

        val document = DocumentHelper.newDocumentBuilder().parse(response.body?.byteStream())
        val bodyElement = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0) as Element

        val fault = bodyElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Fault").item(0) as? Element
        if (fault != null) {
            val faultString = fault.getElementsByTagName("faultstring").item(0).textContent
            throw SOAPFaultException(faultString)
        }

        val o = O::class.java.newInstance()
        o.readSOAPEnvelope(bodyElement.firstChild as Element)
        return o
    }

    fun addInterceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }
}
