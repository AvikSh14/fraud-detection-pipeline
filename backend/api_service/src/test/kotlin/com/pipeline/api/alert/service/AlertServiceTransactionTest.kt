package com.pipeline.api.alert.service

import com.pipeline.api.TestcontainersConfiguration
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.newAlert
import com.pipeline.api.alert.repository.AlertRepository
import com.pipeline.api.alert.repository.AlertStatusHistoryRepository
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest
@Import(TestcontainersConfiguration::class)
// Deliberately not @Transactional: changeStatus must open and commit its own transaction,
// otherwise the failing commit (and the rollback being tested) never happens inside the call
class AlertServiceTransactionTest {

    @Autowired
    lateinit var alertService: AlertService
    @Autowired
    lateinit var alertRepository: AlertRepository
    @Autowired
    lateinit var alertStatusHistoryRepository: AlertStatusHistoryRepository

    @Test
    fun `a failed history insert rolls back the status change`() {
        val alert = alertRepository.save(newAlert(status = AlertStatus.NEW))
        // changed_by is VARCHAR(255), so Postgres itself rejects the history row at commit
        val tooLongAnalystName = "a".repeat(256)

        assertThatThrownBy {
            alertService.changeStatus(
                alertId = alert.id,
                targetStatus = AlertStatus.IN_REVIEW,
                expectedVersion = 0L,
                changedBy = tooLongAnalystName,
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        val storedAlert = alertRepository.findById(alert.id).orElseThrow()
        assertThat(storedAlert.status).isEqualTo(AlertStatus.NEW)
        assertThat(storedAlert.version).isEqualTo(0L)
        assertThat(alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alert.id)).isEmpty()
    }
}
