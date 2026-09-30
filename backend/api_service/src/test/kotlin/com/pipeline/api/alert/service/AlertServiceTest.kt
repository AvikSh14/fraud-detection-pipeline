package com.pipeline.api.alert.service

import com.pipeline.api.PostgresRepositoryTest
import com.pipeline.api.alert.domain.Alert
import com.pipeline.api.alert.domain.AlertNotFoundException
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.domain.AlertVersionMismatchException
import com.pipeline.api.alert.domain.InvalidStatusTransitionException
import com.pipeline.api.alert.newAlert
import com.pipeline.api.alert.repository.AlertRepository
import com.pipeline.api.alert.repository.AlertStatusHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test

@PostgresRepositoryTest
@Import(AlertService::class, AlertServiceTest.FixedClockConfiguration::class)
class AlertServiceTest {

    @TestConfiguration
    class FixedClockConfiguration {
        @Bean
        fun clock(): Clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC)
    }

    @Autowired lateinit var alertService: AlertService
    @Autowired lateinit var alertRepository: AlertRepository
    @Autowired lateinit var alertStatusHistoryRepository: AlertStatusHistoryRepository
    @Autowired lateinit var testEntityManager: TestEntityManager
    @Autowired lateinit var clock: Clock

    private val reviewer = "analyst@example.com"

    @Test
    fun `changes the status and records the change in the history`() {
        val alert = alertRepository.saveAndFlush(newAlert())

        alertService.changeStatus(alert.id, AlertStatus.IN_REVIEW, expectedVersion = 0, changedBy = reviewer)

        val storedAlert = flushAndReload(alert.id)
        assertThat(storedAlert.status).isEqualTo(AlertStatus.IN_REVIEW)
        assertThat(storedAlert.updatedAt).isEqualTo(clock.instant())
        assertThat(storedAlert.createdAt).isEqualTo(alert.createdAt)
        assertThat(storedAlert.version).isEqualTo(1L)
        assertThat(historyOf(alert.id)).containsExactly(Triple(AlertStatus.NEW, AlertStatus.IN_REVIEW, reviewer))
    }

    @Test
    fun `rejects a transition outside the workflow and records nothing`() {
        val alert = alertRepository.saveAndFlush(newAlert())

        assertThatThrownBy {
            alertService.changeStatus(alert.id, AlertStatus.CONFIRMED_FRAUD, expectedVersion = 0, changedBy = reviewer)
        }.isInstanceOf(InvalidStatusTransitionException::class.java)

        assertThat(flushAndReload(alert.id).status).isEqualTo(AlertStatus.NEW)
        assertThat(historyOf(alert.id)).isEmpty()
    }

    @Test
    fun `rejects a change based on a stale version`() {
        val alert = alertRepository.saveAndFlush(newAlert())

        assertThatThrownBy {
            alertService.changeStatus(alert.id, AlertStatus.IN_REVIEW, expectedVersion = 5, changedBy = reviewer)
        }.isInstanceOf(AlertVersionMismatchException::class.java)
    }

    @Test
    fun `rejects a change to an alert that does not exist`() {
        assertThatThrownBy {
            alertService.changeStatus(UUID.randomUUID(), AlertStatus.IN_REVIEW, expectedVersion = 0, changedBy = reviewer)
        }.isInstanceOf(AlertNotFoundException::class.java)
    }

    private fun flushAndReload(alertId: UUID): Alert {
        testEntityManager.flush()
        testEntityManager.clear()
        return alertRepository.findById(alertId).orElseThrow()
    }

    private fun historyOf(alertId: UUID) = alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alertId)
        .map { change -> Triple(change.previousStatus, change.newStatus, change.changedBy) }
}

