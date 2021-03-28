package org.codefirst.wsdl2kotlin

fun main(args: Array<String>) {
    val outputs = WSDL2Kotlin().run(*args)

    // TODO: write to file
    outputs.forEach {
        println(it.code)
    }
}
