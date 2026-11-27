package com.pipeline.api.alert.metrics

import com.pipeline.api.TestcontainersConfiguration
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.domain.AlertVersionMismatchException
import com.pipeline.api.alert.newAlert
import com.pipeline.api.alert.repository.AlertRepository
import com.pipeline.api.alert.service.AlertService
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.Test

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class AlertStatusChangeMetricsIntegrationTest {

    @Autowired
    lateinit var alertService: AlertService
    @Autowired
    lateinit var alertRepository: AlertRepository
    @Autowired
    lateinit var meterRegistry: MeterRegistry
    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `a committed status change is counted once`() {
        val alert = alertRepository.save(newAlert(status = AlertStatus.NEW))
        val countBefore = newToInReviewCount()

        alertService.changeStatus(alert.id, AlertStatus.IN_REVIEW, expectedVersion = 0L, changedBy = REVIEWER)

        assertThat(newToInReviewCount()).isEqualTo(countBefore + 1)
    }

    @Test
    fun `a rejected status change is not counted`() {
        val alert = alertRepository.save(newAlert(status = AlertStatus.NEW))
        val countBefore = newToInReviewCount()

        assertThatThrownBy {
            alertService.changeStatus(alert.id, AlertStatus.IN_REVIEW, expectedVersion = 5L, changedBy = REVIEWER)
        }.isInstanceOf(AlertVersionMismatchException::class.java)

        assertThat(newToInReviewCount()).isEqualTo(countBefore)
    }

    @Test
    fun `a status change rolled back after the event was published is not counted`() {
        val alert = alertRepository.save(newAlert(status = AlertStatus.NEW))
        val countBefore = newToInReviewCount()

        transactionTemplate.executeWithoutResult { transactionStatus ->
            alertService.changeStatus(alert.id, AlertStatus.IN_REVIEW, expectedVersion = 0L, changedBy = REVIEWER)
            transactionStatus.setRollbackOnly()
        }

        assertThat(newToInReviewCount()).isEqualTo(countBefore)
        assertThat(alertRepository.findById(alert.id).orElseThrow().status).isEqualTo(AlertStatus.NEW)
    }

    private fun newToInReviewCount(): Double =
        meterRegistry.find(AlertStatusChangeMetrics.STATUS_CHANGES_COUNTER)
            .tags("from", AlertStatus.NEW.name, "to", AlertStatus.IN_REVIEW.name)
            .counter()?.count() ?: 0.0

    companion object {
        private const val REVIEWER = "analyst@example.com"
    }
}
