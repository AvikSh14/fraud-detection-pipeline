package com.pipeline.generator.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ConfigLoaderTest {

    @Test
    fun `loads the bundled defaults`() {
        val config = ConfigLoader.load()

        assertEquals("localhost:9092", config.kafka.bootstrapServers)
        assertEquals("transactions", config.kafka.topic)
        assertEquals("event-generator", config.kafka.clientIdPrefix)
        assertEquals("all", config.kafka.acks)
        assertEquals(20, config.kafka.lingerMs)
        assertEquals(10, config.simulation.targetRatePerSecond)
        assertEquals(10.seconds, config.simulation.metricsInterval)
    }
}
