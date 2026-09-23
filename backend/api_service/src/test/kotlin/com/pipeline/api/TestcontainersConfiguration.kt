package com.pipeline.api

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

// A throwaway Postgres as a Spring bean. Spring caches test contexts, so every test class
// with the same configuration shares one container instead of starting its own.
// @ServiceConnection points the datasource at this container, overriding application.yml.
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
}
