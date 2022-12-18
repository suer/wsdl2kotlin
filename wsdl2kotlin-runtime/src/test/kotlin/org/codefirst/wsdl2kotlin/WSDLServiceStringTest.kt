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

class SampleService : WSDLService() {
    override val targetNamespace = "http://service.sample.codefirst.org/"
    override var endpoint = "http://localhost:18080"
    override var path = "service"

    fun request(parameters: SampleService_echoString): SampleService_echoStringResponse {
        return requestGeneric<SampleService_echoString, SampleService_echoStringResponse>(parameters)
    }
}

class SampleService_echoString : XSDType() {
    var arg0: String? = null

    override fun xmlParams(): Array<XMLParam> {
        return arrayOf(
            XMLParam("", "arg0", arg0, String::class),
        )
    }

    override fun readSOAPEnvelope(bodyElement: Element) {
        arg0 = readSOAPEnvelopeFieldNullable(bodyElement, "arg0", String::class)
    }
}

class SampleService_echoStringResponse : XSDType() {
    var `return`: String? = null

    override fun xmlParams(): Array<XMLParam> {
        return arrayOf(
            XMLParam("", "return", `return`, String::class),
        )
    }

    override fun readSOAPEnvelope(bodyElement: Element) {
        `return` = readSOAPEnvelopeFieldNullable(bodyElement, "return", String::class)
    }
}

class WSDLServiceStringTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(18080)

    @Before
    fun setup() {
        val responseBody = """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoStringResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>test</return></ns2:echoStringResponse></S:Body>
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
    fun testEchoString() {
        val params = SampleService_echoString()
        params.arg0 = "test"
        val response = SampleService().request(params)
        assertEquals("test", response.`return`)
    }
}
