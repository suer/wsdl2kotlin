package org.codefirst.wsdl2kotlin

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.w3c.dom.Element
import java.text.SimpleDateFormat
import kotlin.test.assertEquals

class SampleServiceDate : WSDLService() {
    override val targetNamespace = "http://service.sample.codefirst.org/"
    override var endpoint = "http://localhost:18080"
    override var path = "service"

    fun request(parameters: SampleService_echoDate): SampleService_echoDateResponse =
        requestGeneric<SampleService_echoDate, SampleService_echoDateResponse>(parameters)
}

@Suppress("ktlint:standard:class-naming")
class SampleService_echoDate : XSDType() {
    var arg0: java.util.Date? = null

    override fun xmlParams(): Array<XMLParam> =
        arrayOf(
            XMLParam("", "arg0", arg0, java.util.Date::class),
        )

    override fun readSOAPEnvelope(bodyElement: Element) {
        arg0 = readSOAPEnvelopeFieldNullable(bodyElement, "arg0", java.util.Date::class)
    }
}

@Suppress("ktlint:standard:class-naming")
class SampleService_echoDateResponse : XSDType() {
    @Suppress("ktlint:standard:property-naming")
    var `return`: java.util.Date? = null

    override fun xmlParams(): Array<XMLParam> =
        arrayOf(
            XMLParam("", "return", `return`, java.util.Date::class),
        )

    override fun readSOAPEnvelope(bodyElement: Element) {
        `return` = readSOAPEnvelopeFieldNullable(bodyElement, "return", java.util.Date::class)
    }
}

class WSDLServiceDateTest {
    @Rule
    @JvmField
    var wireMockRule = WireMockRule(18080)

    private val dateFormat = SimpleDateFormat(XSDType.DATETIME_FORMAT)

    private val requestDate = dateFormat.parse("2024-01-02T03:04:05+09:00")
    private val responseDate = dateFormat.parse("2024-05-06T07:08:09+09:00")

    @Before
    fun setup() {
        val responseBody =
            """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoDateResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>${dateFormat.format(responseDate)}</return></ns2:echoDateResponse></S:Body>
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
    fun testEchoDate() {
        val params = SampleService_echoDate()
        params.arg0 = requestDate
        val response = SampleServiceDate().request(params)

        assertEquals(responseDate, response.`return`)

        verify(
            postRequestedFor(urlEqualTo("/service"))
                .withRequestBody(containing(dateFormat.format(requestDate))),
        )
    }
}
