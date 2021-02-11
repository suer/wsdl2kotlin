package org.codefirst.wsdl2kotlin

import com.tickaroo.tikxml.TikXml
import okio.Okio
import java.io.File

class XSD {
    companion object {
        fun parse(path: String): XSDSchema {
            val parser: TikXml = TikXml.Builder().exceptionOnUnreadXml(false).build()
            val buffer = Okio.buffer(Okio.source(File(path)))
            return parser.read<XSDSchema>(buffer, XSDSchema::class.java)
        }

        fun isXSD(path: String): Boolean {
            val xsd = parse(path)
            return xsd.elements.any()
        }
    }
}
