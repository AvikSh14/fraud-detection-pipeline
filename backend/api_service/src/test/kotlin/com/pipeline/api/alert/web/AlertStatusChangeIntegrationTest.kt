package com.pipeline.api.alert.web

import com.pipeline.api.TestcontainersConfiguration
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.newAlert
import com.pipeline.api.alert.repository.AlertRepository
import com.pipeline.api.alert.repository.AlertStatusHistoryRepository
import kotlin.test.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AlertStatusChangeIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var alertRepository: AlertRepository
    @Autowired
    lateinit var alertStatusHistoryRepository: AlertStatusHistoryRepository

    @Test
    fun `a second verdict based on the same version is rejected instead of overwriting the first`() {
        val alert = alertRepository.save(newAlert(status = AlertStatus.IN_REVIEW))

        patchStatus(alert.id, "CONFIRMED_FRAUD", ifMatch = "\"0\"").andExpect {
            status { isOk() }
            header { string(HttpHeaders.ETAG, "\"1\"") }
        }
        patchStatus(alert.id, "FALSE_POSITIVE", ifMatch = "\"0\"").andExpect {
            status { isPreconditionFailed() }
            jsonPath("$.currentVersion") { value(1) }
        }

        val storedAlert = alertRepository.findById(alert.id).orElseThrow()
        assertThat(storedAlert.status).isEqualTo(AlertStatus.CONFIRMED_FRAUD)
        assertThat(storedAlert.version).isEqualTo(1L)
        assertThat(alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alert.id)).hasSize(1)
    }

    private fun patchStatus(alertId: UUID, status: String, ifMatch: String) =
        mockMvc.patch("/alerts/{alertId}/status", alertId) {
            header(HttpHeaders.IF_MATCH, ifMatch)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "$status"}"""
        }
}
