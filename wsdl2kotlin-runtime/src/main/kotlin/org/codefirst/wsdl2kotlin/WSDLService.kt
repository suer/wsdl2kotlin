package org.codefirst.wsdl2kotlin

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.w3c.dom.Element
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SOAPFaultException(
    faultString: String,
) : RuntimeException(faultString)

abstract class WSDLService {
    abstract val targetNamespace: String
    abstract var endpoint: String
    abstract var path: String

    protected val interceptors = mutableListOf<Interceptor>()

    protected val requestUrl: String
        get() = endpoint.removeSuffix("/") + "/" + path.removePrefix("/")

    protected inline fun <I : XSDType, reified O : XSDType> requestGeneric(i: I): O {
        val soapRequest = i.soapRequest(targetNamespace)

        val requestBody =
            object : RequestBody() {
                override fun contentType() = "text/xml".toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                    DocumentHelper.newTransformer().transform(
                        DOMSource(soapRequest),
                        StreamResult(FixSurrogatePairOutputStream(sink.outputStream())),
                    )
                }
            }

        val request =
            Request
                .Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()
        val client =
            OkHttpClient
                .Builder()
                .also { builder ->
                    interceptors.forEach {
                        builder.addInterceptor(it)
                    }
                }.build()
        val response = client.newCall(request).execute()

        val document = DocumentHelper.newDocumentBuilder().parse(response.body?.byteStream())
        val bodyElement = DocumentHelper.getChildElementsByTagName(document.documentElement, "Body").first()

        val fault = DocumentHelper.getChildElementsByTagName(bodyElement, "Fault").firstOrNull()
        if (fault != null) {
            val faultString = fault.getElementsByTagName("faultstring").item(0).textContent
            throw SOAPFaultException(faultString)
        }

        val o = O::class.java.getDeclaredConstructor().newInstance()
        o.readSOAPEnvelope(bodyElement.firstChild as Element)
        return o
    }

    fun addInterceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }
}
