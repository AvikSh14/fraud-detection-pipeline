package com.pipeline.generator.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ConfigValidatorTest {

    private fun validConfig() = GeneratorConfig(
        kafka = KafkaConfig(
            bootstrapServers = "localhost:9092",
            topic = "transactions",
            clientIdPrefix = "event-generator",
            acks = "all",
            lingerMs = 20,
        ),
        simulation = SimulationConfig(
            targetRatePerSecond = 10,
            metricsInterval = 10.seconds,
        ),
    )

    @Test
    fun `accepts a valid configuration`() {
        assertDoesNotThrow { ConfigValidator.validate(validConfig()) }
    }

    @Test
    fun `reports every invalid field in one pass`() {
        val base = validConfig()
        val broken = base.copy(
            kafka = base.kafka.copy(
                bootstrapServers = " ",
                topic = "",
                lingerMs = -1,
            ),
            simulation = base.simulation.copy(
                targetRatePerSecond = 0
            )
        )

        val error = assertFailsWith<ConfigValidationException> {
            ConfigValidator.validate(broken)
        }

        assertEquals(4, error.errors.size)
        assertTrue(error.errors.any { "kafka.bootstrap-servers" in it })
        assertTrue(error.errors.any { "kafka.topic" in it })
        assertTrue(error.errors.any { "kafka.linger-ms" in it })
        assertTrue(error.errors.any { "simulation.target-rate-per-second" in it })
    }
}