package org.codefirst.wsdl2kotlin

fun interface Logger {
    fun info(message: String)
}

object PrintLogger : Logger {
    override fun info(message: String) {
        println(message)
    }
}
