package com.pipeline.api.alert

import com.pipeline.api.PostgresRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// AlertStatus and the SQL CHECK constraints each list the allowed statuses. This fails the build
// if they ever drift apart, instead of the mismatch surfacing as a failed insert in production.
@PostgresRepositoryTest
class AlertStatusSchemaConformanceTest {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    // Postgres's auto-generated names, "<table>_<column>_check", for the three status columns
    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "alerts_status_check",
            "alert_status_history_previous_status_check",
            "alert_status_history_new_status_check"
        ]
    )
    fun `check constraint allows exactly the AlertStatus values`(constraintName: String) {
        // pg_get_constraintdef returns the constraint's SQL text; a renamed constraint finds no row and throws
        val constraintDefinition = checkNotNull(
            jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?",
                String::class.java,
                constraintName
            )
        )

        val allowedValues = quotedLiteral.findAll(constraintDefinition).map { it.groupValues[1] }.toList()

        assertThat(allowedValues).containsExactlyInAnyOrderElementsOf(AlertStatus.entries.map { it.name })
    }

    companion object {
        // Each allowed value appears in the constraint as a quoted SQL literal, e.g. 'IN_REVIEW'
        private val quotedLiteral = Regex("'([^']*)'")
    }
}
