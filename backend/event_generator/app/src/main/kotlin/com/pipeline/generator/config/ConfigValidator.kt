package com.pipeline.generator.config

class ConfigValidationException(
    val errors: List<String>
) : RuntimeException(
    "Invalid configuration (${errors.size} error(s))" + errors.joinToString("") {
        "\n - $it"
    }
)

object ConfigValidator {

    const val MAX_RATE_PER_SECOND = 100_000

    fun validate(config: GeneratorConfig) {
        val kafka = config.kafka
        val simulation = config.simulation

        val errors = buildList {
            if (kafka.bootstrapServers.isBlank()) add("kafka.bootstrap-servers must not be blank")
            if (kafka.topic.isBlank()) add("kafka.topic must not be blank")
            if (kafka.clientIdPrefix.isBlank()) add("kafka.client-id-prefix must not be blank")
            if (kafka.lingerMs < 0) add("kafka.linger-ms must be >= 0, was ${kafka.lingerMs}")

            if (simulation.targetRatePerSecond !in 1..MAX_RATE_PER_SECOND) {
                add("simulation.target-rate-per-second must be between 1 and $MAX_RATE_PER_SECOND, was ${simulation.targetRatePerSecond}")
            }
            if (!simulation.metricsInterval.isPositive()) {
                add("simulation.metrics-interval must be positive, was ${simulation.metricsInterval}")
            }
        }

        if (errors.isNotEmpty()) {
            throw ConfigValidationException(
                errors = errors
            )
        }
    }
}