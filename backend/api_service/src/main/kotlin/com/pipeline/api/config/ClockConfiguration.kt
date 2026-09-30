package com.pipeline.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
