package org.codefirst.wsdl2kotlin

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory

class DocumentHelper {
    companion object {
        fun newDocumentBuilder(): DocumentBuilder =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    this.isNamespaceAware = true
                }.newDocumentBuilder()

        fun newTransformer(): Transformer =
            TransformerFactory.newInstance().newTransformer().apply {
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
}
