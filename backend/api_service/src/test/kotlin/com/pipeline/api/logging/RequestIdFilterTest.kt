package com.pipeline.api.logging

import com.pipeline.api.logging.RequestIdFilter.Companion.REQUEST_ID_HEADER
import com.pipeline.api.logging.RequestIdFilter.Companion.REQUEST_ID_MDC_KEY
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID
import kotlin.test.Test

class RequestIdFilterTest {

    private val requestIdFilter = RequestIdFilter()
    private val response = MockHttpServletResponse()
    private var requestIdSeenByChain: String? = null
    private val recordingChain = FilterChain { _, _ -> requestIdSeenByChain = MDC.get(REQUEST_ID_MDC_KEY) }

    @Test
    fun `generates a request id when the client sends none`() {
        requestIdFilter.doFilter(MockHttpServletRequest(), response, recordingChain)

        val requestId = response.getHeader(REQUEST_ID_HEADER)
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId)
        assertThat(requestIdSeenByChain).isEqualTo(requestId)
    }

    @Test
    fun `reuses a safe request id sent by the client`() {
        requestIdFilter.doFilter(requestWithId("checkout-42"), response, recordingChain)

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("checkout-42")
        assertThat(requestIdSeenByChain).isEqualTo("checkout-42")
    }

    @Test
    fun `replaces a request id that could forge log lines`() {
        val forgedId = "abc\n{\"log.level\":\"ERROR\"}"

        requestIdFilter.doFilter(requestWithId(forgedId), response, recordingChain)

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isNotEqualTo(forgedId)
        assertThat(requestIdSeenByChain).isEqualTo(response.getHeader(REQUEST_ID_HEADER))
    }

    @Test
    fun `replaces a request id longer than 64 characters`() {
        requestIdFilter.doFilter(requestWithId("a".repeat(65)), response, recordingChain)

        assertThat(response.getHeader(REQUEST_ID_HEADER)).hasSizeLessThanOrEqualTo(64)
    }

    @Test
    fun `clears the request id after the request, even when the chain fails`() {
        val failingChain = FilterChain { _, _ -> throw IllegalStateException("boom") }

        assertThatThrownBy { requestIdFilter.doFilter(requestWithId("checkout-42"), response, failingChain) }
            .isInstanceOf(IllegalStateException::class.java)

        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull()
    }

    private fun requestWithId(requestId: String) =
        MockHttpServletRequest().apply { addHeader(REQUEST_ID_HEADER, requestId) }
}
