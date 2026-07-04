package org.codefirst.wsdl2kotlin

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.w3c.dom.Element
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import kotlin.test.assertEquals

private fun parseDate(text: String): Date = Date.from(OffsetDateTime.parse(text).toInstant())

private fun formatDate(date: Date): String =
    date
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .format(XSDType.DATETIME_FORMATTER)

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

    private val requestDate = parseDate("2024-01-02T03:04:05+09:00")
    private val responseDate = parseDate("2024-05-06T07:08:09+09:00")

    @Before
    fun setup() {
        val responseBody =
            """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoDateResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>${formatDate(responseDate)}</return></ns2:echoDateResponse></S:Body>
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
                .withRequestBody(containing(formatDate(requestDate))),
        )
    }
}

/**
 * Verifies that half-hour timezone offsets such as +09:30 are preserved end-to-end,
 * now that XSDType uses OffsetDateTime + DateTimeFormatter.ISO_OFFSET_DATE_TIME instead
 * of the old SimpleDateFormat-based DATETIME_FORMAT (which hardcoded the minute offset to "00").
 */
class WSDLServiceDateHalfHourOffsetTest {
    @Rule
    @JvmField
    var wireMockRule = WireMockRule(18080)

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setup() {
        originalTimeZone = TimeZone.getDefault()
        // Australia/Darwin is UTC+09:30 year-round with no daylight saving time
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Darwin"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun testEchoDateSerializationPreservesHalfHourOffset() {
        val requestDate = parseDate("2024-01-02T03:04:05+09:30")

        val responseBody =
            """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoDateResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>${formatDate(requestDate)}</return></ns2:echoDateResponse></S:Body>
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

        val params = SampleService_echoDate()
        params.arg0 = requestDate
        SampleServiceDate().request(params)

        verify(
            postRequestedFor(urlEqualTo("/service"))
                .withRequestBody(containing("2024-01-02T03:04:05+09:30")),
        )
    }

    @Test
    fun testEchoDateDeserializationPreservesHalfHourOffset() {
        val expectedDate = parseDate("2024-01-02T03:04:05+09:30")

        val responseBody =
            """<?xml version='1.0' encoding='UTF-8'?>
            |<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
            |<S:Body><ns2:echoDateResponse xmlns:ns2="http://service.sample.codefirst.org/">
            |<return>2024-01-02T03:04:05+09:30</return></ns2:echoDateResponse></S:Body>
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

        val response = SampleServiceDate().request(SampleService_echoDate())

        assertEquals(expectedDate, response.`return`)
    }
}
