package org.codefirst.wsdl2kotlin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class Main : CliktCommand() {
    val dir: String by option("-d", "--dir", help = "output directory").default("./")
    val paths: Set<Path> by argument().path(mustExist = true).multiple().unique()

    override fun run() {
        val outputs = WSDL2Kotlin().run(*paths.map { it.toString() }.toTypedArray())

        outputs.forEach {
            it.save(dir)
        }
    }
}

fun main(args: Array<String>) {
    Main().main(args)
}
