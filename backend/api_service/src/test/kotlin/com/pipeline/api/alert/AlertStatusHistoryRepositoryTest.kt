package com.pipeline.api.alert

import com.pipeline.api.PostgresRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.test.Test

@PostgresRepositoryTest
class AlertStatusHistoryRepositoryTest {

    @Autowired
    lateinit var alertRepository: AlertRepository

    @Autowired
    lateinit var alertStatusHistoryRepository: AlertStatusHistoryRepository

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `saves and finds a status change round trip`() {
        val alert = alertRepository.saveAndFlush(newAlert())
        val statusChange = newStatusChange(alert, AlertStatus.NEW, AlertStatus.IN_REVIEW)

        alertStatusHistoryRepository.saveAndFlush(statusChange)
        testEntityManager.clear()

        val foundChange = alertStatusHistoryRepository.findById(statusChange.id).orElseThrow()
        // foundChange.alert is a lazy Hibernate proxy, so compare the link by id instead of field by field
        assertThat(foundChange).usingRecursiveComparison().ignoringFields("alert").isEqualTo(statusChange)
        assertThat(foundChange.alert.id).isEqualTo(alert.id)
    }

    @Test
    fun `finds an alert's history oldest first`() {
        val alert = alertRepository.saveAndFlush(newAlert())
        val firstChangeAt = nowInMicros()
        val firstChange = newStatusChange(alert, AlertStatus.NEW, AlertStatus.IN_REVIEW, firstChangeAt)
        val secondChange = newStatusChange(
            alert, AlertStatus.IN_REVIEW, AlertStatus.CONFIRMED_FRAUD, firstChangeAt.plusSeconds(60)
        )

        // Saved newest first on purpose: proves the query sorts, instead of relying on insert order
        alertStatusHistoryRepository.saveAllAndFlush(listOf(secondChange, firstChange))
        testEntityManager.clear()

        val history = alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alert.id)

        assertThat(history.map { it.newStatus })
            .containsExactly(AlertStatus.IN_REVIEW, AlertStatus.CONFIRMED_FRAUD)
    }

    @Test
    fun `returns no history for an alert that never changed status`() {
        val alert = alertRepository.saveAndFlush(newAlert())
        testEntityManager.clear()

        assertThat(alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alert.id)).isEmpty()
    }

    @Test
    fun `database rejects history for an alert that does not exist`() {
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO alert_status_history (id, alert_id, previous_status, new_status, changed_by)
                VALUES (?, ?, 'NEW', 'IN_REVIEW', 'analyst@example.com')
                """.trimIndent(),
                UUID.randomUUID(),
                UUID.randomUUID()
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
            // Postgres names a foreign key "<table>_<column>_fkey"
            .hasMessageContaining("alert_status_history_alert_id_fkey")
    }

    @Test
    fun `saving a history row with a reused row id fails instead of overwriting the original`() {
        val originalChange = savedStatusChange()
        testEntityManager.clear()

        val conflictingChange = newStatusChange(
            alert = originalChange.alert,
            previousStatus = AlertStatus.IN_REVIEW,
            newStatus = AlertStatus.FALSE_POSITIVE,
            id = originalChange.id
        )

        assertThatThrownBy { alertStatusHistoryRepository.saveAndFlush(conflictingChange) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("alert_status_history_pkey")
    }

    @Test
    fun `database rejects updating a status change`() {
        val statusChange = savedStatusChange()

        assertThatThrownBy {
            jdbcTemplate.update(
                "UPDATE alert_status_history SET changed_by = ? WHERE id = ?",
                "attacker@example.com",
                statusChange.id
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("alert_status_history rows are immutable")
    }

    @Test
    fun `database still allows deleting a status change for retention`() {
        val statusChange = savedStatusChange()

        val deletedRowCount = jdbcTemplate.update("DELETE FROM alert_status_history WHERE id = ?", statusChange.id)

        assertThat(deletedRowCount).isEqualTo(1)
    }

    private fun savedStatusChange(): AlertStatusHistory {
        val alert = alertRepository.saveAndFlush(newAlert())
        return alertStatusHistoryRepository.saveAndFlush(
            newStatusChange(alert = alert, previousStatus = AlertStatus.NEW, newStatus = AlertStatus.IN_REVIEW)
        )
    }
}
