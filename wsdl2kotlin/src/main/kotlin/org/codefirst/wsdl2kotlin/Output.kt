package org.codefirst.wsdl2kotlin

import java.io.File
import java.nio.charset.Charset

class Output(private val serviceName: String, private val packageName: String, private val code: String) {
    fun save(dir: String = "./") {
        val directory = File(dir, packageName.replace('.', '/'))
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "$serviceName.kt")
        println("Generating ${file.canonicalPath} ...")
        file.writeText(code, Charset.forName("UTF-8"))
    }
}
