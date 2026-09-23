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
}
