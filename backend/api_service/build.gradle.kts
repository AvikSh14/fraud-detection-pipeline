plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.plugin.spring)
	alias(libs.plugins.kotlin.plugin.jpa)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.pipeline"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.kotlin.reflect)
	implementation(libs.jackson.module.kotlin)
	runtimeOnly(libs.postgresql)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.flyway.database.postgresql)

	testImplementation(libs.spring.boot.starter.actuator.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.postgresql)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}
