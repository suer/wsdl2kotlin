package org.codefirst.wsdl2kotlin

import java.io.File

class Output(
    private val serviceName: String,
    private val packageName: String,
    private val code: String,
) {
    fun save(
        dir: String = "./",
        logger: Logger = PrintLogger,
    ) {
        val directory = File(dir, packageName.replace('.', '/'))
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "$serviceName.kt")
        logger.info("Generating ${file.canonicalPath} ...")
        file.writeText(code, Charsets.UTF_8)
    }
}
