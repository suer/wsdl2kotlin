package org.codefirst.wsdl2kotlin

import java.io.ByteArrayOutputStream
import java.io.OutputStream

// to fix https://bugs.openjdk.java.net/browse/JDK-8203810
class FixSurrogatePairOutputStream(private val outputStream: OutputStream) : OutputStream() {

    private val buffer = ByteArrayOutputStream()

    private var inNumericalCharacterReferences = false

    private var highChar: Int = 0

    override fun write(b: Int) {
        if ('&'.code == b) {
            inNumericalCharacterReferences = true
        }

        if (inNumericalCharacterReferences) {
            buffer.write(b)

            if (';'.code == b) {
                inNumericalCharacterReferences = false

                // &#55360;
                val numericalCharacterReferences = buffer.toString()

                // 55360
                val charCode = numericalCharacterReferences
                    .removePrefix("&#")
                    .removeSuffix(";")
                    .toIntOrNull()

                if (charCode == null) {
                    highChar = 0
                    outputStream.write(numericalCharacterReferences.toByteArray())
                } else {
                    if ('\uD800'.code <= charCode && charCode <= '\uDBFF'.code) {
                        highChar = charCode
                    } else {
                        if (highChar > 0) {
                            // 𠀋
                            val unicode = String(intArrayOf(highChar, charCode), 0, 2)

                            // 131083
                            val codePoint = unicode.codePointAt(0)

                            // &#131083;
                            outputStream.write("&#$codePoint;".toByteArray())

                            highChar = 0
                        } else {
                            // &#60;
                            outputStream.write("&#$charCode;".toByteArray())
                        }
                    }
                }

                buffer.reset()
            }
        } else {
            outputStream.write(b)
        }
    }

    override fun close() {
        outputStream.write(buffer.toByteArray())
        outputStream.close()
    }
}
