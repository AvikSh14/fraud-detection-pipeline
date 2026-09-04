package com.pipeline.generator.config

import kotlin.time.Duration

data class GeneratorConfig(
    val kafka: KafkaConfig,
    val simulation: SimulationConfig,
)

data class KafkaConfig(
    val bootstrapServers: String,
    val topic: String,
    val clientIdPrefix: String,
    val lingerMs: Int,
)

data class SimulationConfig(
    val targetRatePerSecond: Int,
    val metricsInterval: Duration,
)