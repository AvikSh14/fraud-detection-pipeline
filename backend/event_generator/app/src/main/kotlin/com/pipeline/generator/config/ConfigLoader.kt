package com.pipeline.generator.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

internal data class AppConfig(
    val generator: GeneratorConfig
)

object ConfigLoader {

    fun load(): GeneratorConfig {
        val config = ConfigLoaderBuilder.default()
            .addResourceSource("/application.conf")
            .build()
            .loadConfigOrThrow<AppConfig>()
            .generator

        ConfigValidator.validate(config)
        return config
    }
}
