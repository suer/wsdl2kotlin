package org.codefirst.wsdl2kotlin

import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerFactory

object DocumentHelper {
    fun newDocumentBuilder(): DocumentBuilder =
        DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                trySetXIncludeAware(false)
                trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                trySetFeature("http://xml.org/sax/features/external-general-entities", false)
                trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
                trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }.newDocumentBuilder()

    fun newTransformer(): Transformer =
        TransformerFactory
            .newInstance()
            .apply {
                trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }.newTransformer()
            .apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
            }

    fun getChildElementsByTagName(
        parentElement: Element,
        tagName: String,
    ): List<Element> {
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

    // Android's JAXP implementation (not Xerces) doesn't recognize Xerces-specific
    // features like disallow-doctype-decl or support isXIncludeAware, and throws
    // UnsupportedOperationException. Ignore on unsupported platforms instead of crashing.
    private fun DocumentBuilderFactory.trySetXIncludeAware(value: Boolean) {
        try {
            isXIncludeAware = value
        } catch (e: UnsupportedOperationException) {
        }
    }

    private fun DocumentBuilderFactory.trySetFeature(
        name: String,
        value: Boolean,
    ) {
        try {
            setFeature(name, value)
        } catch (e: ParserConfigurationException) {
        } catch (e: UnsupportedOperationException) {
        }
    }

    private fun TransformerFactory.trySetFeature(
        name: String,
        value: Boolean,
    ) {
        try {
            setFeature(name, value)
        } catch (e: TransformerConfigurationException) {
        } catch (e: UnsupportedOperationException) {
        }
    }
}
