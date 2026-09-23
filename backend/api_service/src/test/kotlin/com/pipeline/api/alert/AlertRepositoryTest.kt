package com.pipeline.api.alert

import com.pipeline.api.PostgresRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
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

        assertThat(storedStatus(alert.id)).isEqualTo("CONFIRMED_FRAUD")
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

    @Test
    fun `a new alert starts at version 0 and each update increments it`() {
        val alert = newAlert()
        saveAndClearCache(alert)
        val loadedAlert = alertRepository.findById(alert.id).orElseThrow()
        assertThat(loadedAlert.version).isEqualTo(0L)

        loadedAlert.status = AlertStatus.IN_REVIEW
        saveAndClearCache(loadedAlert)

        assertThat(alertRepository.findById(alert.id).orElseThrow().version).isEqualTo(1L)
    }

    @Test
    fun `rejects saving an alert that changed after it was loaded`() {
        val alert = newAlert(status = AlertStatus.IN_REVIEW)
        saveAndClearCache(alert)
        val analystAView = loadDetached(alert.id)
        val analystBView = loadDetached(alert.id)

        // A saves first, so B's copy is now stale
        analystAView.status = AlertStatus.CONFIRMED_FRAUD
        saveAndClearCache(analystAView)

        analystBView.status = AlertStatus.FALSE_POSITIVE
        assertThatThrownBy { alertRepository.saveAndFlush(analystBView) }
            .isInstanceOf(OptimisticLockingFailureException::class.java)
        // Hibernate spots the old version itself before any UPDATE is sent, so the DB can still be queried
        assertThat(storedStatus(alert.id)).isEqualTo("CONFIRMED_FRAUD")
    }

    @Test
    fun `rejects a redelivered alert instead of overwriting the stored one`() {
        val alert = newAlert()
        saveAndClearCache(alert)

        // A fresh object with an existing id, like a Kafka redelivery: must INSERT and fail, never UPDATE
        val redeliveredAlert = newAlert(id = alert.id)

        // Postgres rejected the INSERT, which aborts the transaction, so no further queries here
        assertThatThrownBy { alertRepository.saveAndFlush(redeliveredAlert) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("alerts_pkey")
    }

    // A copy Hibernate no longer tracks, like an alert held between two HTTP requests
    private fun loadDetached(id: UUID): Alert =
        alertRepository.findById(id).orElseThrow().also { testEntityManager.clear() }

    // Raw SQL bypasses JPA's enum conversion and Hibernate's cache, so this is the literal column value
    private fun storedStatus(id: UUID): String? =
        jdbcTemplate.queryForObject("SELECT status FROM alerts WHERE id = ?", String::class.java, id)

    // Writes to Postgres now, then empties Hibernate's cache so the next read really hits the database
    private fun saveAndClearCache(alert: Alert) {
        alertRepository.saveAndFlush(alert)
        testEntityManager.clear()
    }
}
