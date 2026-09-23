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
class AlertRepositoryTest {

    @Autowired
    lateinit var alertRepository: AlertRepository

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `saves and finds an alert round trip`() {
        val alert = newAlert()

        saveAndClearCache(alert)
        val foundAlert = alertRepository.findById(alert.id).orElseThrow()

        assertThat(foundAlert).usingRecursiveComparison().isEqualTo(alert)
    }

    @Test
    fun `findById returns empty for an unknown id`() {
        val foundAlert = alertRepository.findById(UUID.randomUUID())

        assertThat(foundAlert).isEmpty()
    }

    @Test
    fun `persists a status change`() {
        val alert = newAlert(status = AlertStatus.NEW)
        saveAndClearCache(alert)

        val loadedAlert = alertRepository.findById(alert.id).orElseThrow()
        loadedAlert.status = AlertStatus.IN_REVIEW
        loadedAlert.updatedAt = loadedAlert.updatedAt.plusSeconds(60)
        saveAndClearCache(loadedAlert)

        val reloadedAlert = alertRepository.findById(alert.id).orElseThrow()
        assertThat(reloadedAlert.status).isEqualTo(AlertStatus.IN_REVIEW)
        assertThat(reloadedAlert.updatedAt).isEqualTo(loadedAlert.updatedAt)
    }

    @Test
    fun `stores status as its enum name`() {
        val alert = newAlert(status = AlertStatus.CONFIRMED_FRAUD)
        saveAndClearCache(alert)

        // Raw SQL bypasses JPA's enum conversion, so this sees the literal column value
        val storedStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM alerts WHERE id = ?", String::class.java, alert.id
        )

        assertThat(storedStatus).isEqualTo("CONFIRMED_FRAUD")
    }

    @Test
    fun `database rejects an unknown status`() {
        // Raw SQL because AlertStatus can't express an invalid value; this tests the V1 migration's CHECK
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO alerts (id, transaction_reference, risk_score, status)
                VALUES (?, 'txn-ref', 0.5, 'ESCALATED')
                """.trimIndent(),
                UUID.randomUUID()
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
            // Postgres names a column CHECK "<table>_<column>_check"; pins the failure to this constraint
            .hasMessageContaining("alerts_status_check")
    }

    // Writes to Postgres now, then empties Hibernate's cache so the next read really hits the database
    private fun saveAndClearCache(alert: Alert) {
        alertRepository.saveAndFlush(alert)
        testEntityManager.clear()
    }
}
