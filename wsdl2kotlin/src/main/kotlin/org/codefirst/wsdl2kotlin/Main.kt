package org.codefirst.wsdl2kotlin

fun main(args: Array<String>) {
    val kotlin = WSDL2Kotlin().run(*args)

    // TODO: write to file
    println(kotlin)
}
