package org.codefirst.wsdl2kotlin

fun main(args: Array<String>) {
    val outputs = WSDL2Kotlin().run(*args)

    outputs.forEach {
        it.save()
    }
}
