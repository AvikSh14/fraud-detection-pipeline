package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.Alert
import com.pipeline.api.alert.domain.AlertNotFoundException
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.newAlert
import com.pipeline.api.alert.newStatusChange
import com.pipeline.api.alert.service.AlertService
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(AlertController::class)
class AlertControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var alertService: AlertService

    @Test
    fun `lists alerts with the default paging, newest first`() {
        val alert = savedAlert()
        val defaultPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        given(alertService.listAlerts(null, defaultPage)).willReturn(PageImpl(listOf(alert), defaultPage, 1))

        mockMvc.get("/alerts").andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(alert.id.toString()) }
            jsonPath("$.totalElements") { value(1) }
        }
    }

    @Test
    fun `returns an alert with its version as the ETag`() {
        val alert = savedAlert(version = 3)
        given(alertService.getAlert(alert.id)).willReturn(alert)

        mockMvc.get("/alerts/{alertId}", alert.id).andExpect {
            status { isOk() }
            header { string(HttpHeaders.ETAG, "\"3\"") }
            jsonPath("$.version") { value(3) }
        }
    }

    @Test
    fun `returns 304 when the client's ETag is still current`() {
        val alert = savedAlert(version = 3)
        given(alertService.getAlert(alert.id)).willReturn(alert)

        mockMvc.get("/alerts/{alertId}", alert.id) { header(HttpHeaders.IF_NONE_MATCH, "\"3\"") }
            .andExpect { status { isNotModified() } }
    }

    @Test
    fun `returns a 404 problem detail for an unknown alert`() {
        val alertId = UUID.randomUUID()
        given(alertService.getAlert(alertId)).willThrow(AlertNotFoundException(alertId))

        mockMvc.get("/alerts/{alertId}", alertId).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.alertId") { value(alertId.toString()) }
        }
    }

    @Test
    fun `rejects an alert id that is not a UUID`() {
        mockMvc.get("/alerts/abc").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `returns the status history oldest first`() {
        val alert = savedAlert()
        given(alertService.getStatusHistory(alert.id))
            .willReturn(listOf(newStatusChange(alert, AlertStatus.NEW, AlertStatus.IN_REVIEW)))

        mockMvc.get("/alerts/{alertId}/history", alert.id).andExpect {
            status { isOk() }
            jsonPath("$[0].previousStatus") { value("NEW") }
            jsonPath("$[0].newStatus") { value("IN_REVIEW") }
        }
    }

    // A loaded alert always has a version, but only Hibernate can set it (protected setter)
    private fun savedAlert(version: Long = 0): Alert =
        newAlert().also { ReflectionTestUtils.setField(it, "version", version) }
}
