package org.codefirst.wsdl2kotlin

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.w3c.dom.Element
import kotlin.test.assertEquals

class SampleServiceInt : WSDLService() {
    override val targetNamespace = "http://service.sample.codefirst.org/"
    override var endpoint = "http://localhost:18080"
    override var path = "service"

    fun request(parameters: SampleService_echoInt): SampleService_echoIntResponse =
        requestGeneric<SampleService_echoInt, SampleService_echoIntResponse>(parameters)
}

@Suppress("ktlint:standard:class-naming")
class SampleService_echoInt : XSDType() {
    var arg0: Int? = null

    override fun xmlParams(): Array<XMLParam> =
        arrayOf(
            XMLParam("", "arg0", arg0, Int::class),
        )

    override fun readSOAPEnvelope(bodyElement: Element) {
        arg0 = readSOAPEnvelopeFieldNullable(bodyElement, "arg0", Int::class)
    }
}

@Suppress("ktlint:standard:class-naming")
class SampleService_echoIntResponse : XSDType() {
    @Suppress("ktlint:standard:property-naming")
    var `return`: Int? = null

    override fun xmlParams(): Array<XMLParam> =
        arrayOf(
            XMLParam("", "return", `return`, Int::class),
        )

    override fun readSOAPEnvelope(bodyElement: Element) {
        `return` = readSOAPEnvelopeFieldNullable(bodyElement, "return", Int::class)
    }
}

class WSDLServiceIntTest {
    @Rule
    @JvmField
    var wireMockRule = WireMockRule(18080)

    @Before
    fun setup() {
        val responseBody =
            """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoIntResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>123</return></ns2:echoIntResponse></S:Body>
            |</S:Envelope>
            """.trimMargin()

        stubFor(
            post(urlEqualTo("/service"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "text/xml")
                        .withBody(responseBody),
                ),
        )
    }

    @Test
    fun testEchoInt() {
        val params = SampleService_echoInt()
        params.arg0 = 123
        val response = SampleServiceInt().request(params)
        assertEquals(123, response.`return`)
    }
}
