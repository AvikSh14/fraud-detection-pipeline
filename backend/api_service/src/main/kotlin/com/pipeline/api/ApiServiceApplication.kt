package com.pipeline.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiServiceApplication

fun main(args: Array<String>) {
	runApplication<ApiServiceApplication>(*args)
}
