package org.codefirst.wsdl2kotlin

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.text.SimpleDateFormat
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

data class XMLParam(
    val namespace: String,
    val name: String,
    val value: Any?,
    val clazz: KClass<*>,
)

abstract class XSDType {
    companion object {
        const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX:00"
    }

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

    private fun xmlElements(
        name: String,
        document: Document,
    ): Array<Element> {
        val typeElement = document.createElement(name)

        xmlParams().forEach { param ->
            val name =
                if (param.namespace.isBlank()) {
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

    private fun xmlElements(
        value: Any?,
        name: String,
        document: Document,
    ): Array<Element> {
        val element = document.createElement(name)
        when (value) {
            is java.util.Date -> {
                element.textContent = SimpleDateFormat(DATETIME_FORMAT).format(value)
            }

            is ByteArray -> {
                element.textContent =
                    java.util.Base64
                        .getEncoder()
                        .encodeToString(value)
            }

            is Array<*> -> {
                return value.map { xmlElements(it, name, document).first() }.toTypedArray()
            }

            is XSDType -> {
                value.xmlParams().forEach { param ->
                    xmlElements(param.value, param.name, document).forEach { childElement ->
                        element.appendChild(childElement)
                    }
                }
            }

            null -> {
                return arrayOf()
            }

            else -> {
                element.textContent = value.toString()
            }
        }

        return arrayOf(element)
    }

    abstract fun readSOAPEnvelope(bodyElement: Element)

    protected fun <T : Any> readSOAPEnvelopeField(
        parentElement: Element,
        tagName: String,
        clazz: KClass<T>,
    ): T = readSOAPEnvelopeFieldNullable(parentElement, tagName, clazz)!!

    private fun <T : Any> isSingleType(clazz: KClass<T>): Boolean =
        when (clazz) {
            String::class, Boolean::class, Int::class, Float::class, Long::class, java.util.Date::class, ByteArray::class -> true
            else -> false
        }

    protected fun <T : Any> readSOAPEnvelopeFieldNullable(
        parentElement: Element,
        tagName: String,
        clazz: KClass<T>,
    ): T? {
        val items = DocumentHelper.getChildElementsByTagName(parentElement, tagName)
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

            val arr =
                java.lang.reflect.Array
                    .newInstance(k.java, items.size)
            items.forEachIndexed { i, item ->
                java.lang.reflect.Array
                    .set(arr, i, singleNodeToObject(item, k))
            }
            return arr as T
        }

        val t =
            clazz.java.getDeclaredConstructor().newInstance() as? XSDType
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

    private fun <T : Any> singleNodeToObject(
        item: Node,
        clazz: KClass<T>,
    ): T {
        return when (clazz) {
            String::class -> {
                item.textContent
            }

            Boolean::class -> {
                item.textContent.equals("true", ignoreCase = true)
            }

            Int::class -> {
                item.textContent.toInt()
            }

            Float::class -> {
                item.textContent.toFloat()
            }

            Long::class -> {
                item.textContent.toLong()
            }

            java.util.Date::class -> {
                SimpleDateFormat(DATETIME_FORMAT).parse(item.textContent)
            }

            ByteArray::class -> {
                java.util.Base64
                    .getDecoder()
                    .decode(item.textContent)
            }

            else -> {
                val t =
                    clazz.java.getDeclaredConstructor().newInstance() as? XSDType
                        ?: throw NotImplementedError("Unsupported type: ${clazz.simpleName}")
                t.readSOAPEnvelope(item as Element)
                return t as T
            }
        } as T
    }
}
