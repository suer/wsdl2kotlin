package org.codefirst.wsdl2kotlin

import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory

object DocumentHelper {
    fun newDocumentBuilder(): DocumentBuilder =
        DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = true
                isXIncludeAware = false
                isExpandEntityReferences = false
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }.newDocumentBuilder()

    fun newTransformer(): Transformer =
        TransformerFactory
            .newInstance()
            .apply {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }.newTransformer()
            .apply {
                this.setOutputProperty(OutputKeys.INDENT, "yes")
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
}
