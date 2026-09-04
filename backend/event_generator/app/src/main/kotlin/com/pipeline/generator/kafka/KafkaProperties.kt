package com.pipeline.generator.kafka

import com.pipeline.generator.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

/**
 * Builds the producer configuration from our validated [KafkaConfig].
 *
 * Pure function: same input always yields the same [Properties], no side effects,
 * so it can be unit-tested without a broker.
 *
 * @param instanceId identifier for this running process, appended to the client
 *   id prefix. Defaults to [resolveInstanceId]; injectable so tests stay deterministic.
 */
internal fun buildProducerProperties(
    kafkaConfig: KafkaConfig,
    instanceId: String = resolveInstanceId(),
): Properties = Properties().apply {
    setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
    setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    setProperty(ProducerConfig.CLIENT_ID_CONFIG, buildClientId(kafkaConfig.clientIdPrefix, instanceId))
    setProperty(ProducerConfig.LINGER_MS_CONFIG, kafkaConfig.lingerMs.toString())

    // Delivery guarantees for a fraud pipeline: never silently drop or duplicate a
    // transaction. Idempotence also forces acks=all and bounded in-flight retries;
    // we set acks explicitly so the intent is visible at the call site.
    setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
    setProperty(ProducerConfig.ACKS_CONFIG, "all")
}
