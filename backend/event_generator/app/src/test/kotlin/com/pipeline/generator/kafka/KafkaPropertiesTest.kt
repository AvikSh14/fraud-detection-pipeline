package com.pipeline.generator.kafka

import com.pipeline.generator.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class KafkaPropertiesTest {

    private val kafkaConfig = KafkaConfig(
        bootstrapServers = "kafka:9092",
        topic = "transactions",
        clientIdPrefix = "event-generator",
        lingerMs = 20,
    )
    private val producerProperties = buildProducerProperties(kafkaConfig, instanceId = "prod-7")

    @Test
    fun `maps kafkaConfig values onto producer property keys`() {
        assertEquals("kafka:9092", producerProperties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
        assertEquals("event-generator-prod-7", producerProperties.getProperty(ProducerConfig.CLIENT_ID_CONFIG))
        assertEquals("20", producerProperties.getProperty(ProducerConfig.LINGER_MS_CONFIG))
    }

    @Test
    fun `uses string serializers for key and value`() {
        assertEquals(
            StringSerializer::class.java.name,
            producerProperties.getProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
        )

        assertEquals(
            StringSerializer::class.java.name,
            producerProperties.getProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
        )
    }

    @Test
    fun `always enables idempotent delivery with acks all`() {
        assertEquals("true", producerProperties.getProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG))
        assertEquals("all", producerProperties.getProperty(ProducerConfig.ACKS_CONFIG))
    }
}
