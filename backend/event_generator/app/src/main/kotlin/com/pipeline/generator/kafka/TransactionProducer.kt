package com.pipeline.generator.kafka

import com.pipeline.generator.model.Transaction
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException

fun interface TransactionSender {
    fun send(transaction: Transaction)
}

class TransactionPublishException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal class TransactionProducer(
    private val kafkaProducer: Producer<String, String>,
    private val topic: String,
    private val json: Json = Json,
) : TransactionSender, AutoCloseable {

    private val logger = KotlinLogging.logger { }
    val stats = ProducerStats()

    override fun send(transaction: Transaction) {
        val payload = json.encodeToString(transaction)
        val record = ProducerRecord(topic, transaction.cardId, payload)

        try {
            kafkaProducer.send(record) { _, exception ->
                if (exception == null) {
                    stats.recordSent()
                } else {
                    stats.recordFailed()
                    logger.warn(exception) { "Failed to send transaction ${transaction.transactionId}" }
                }
            }
        } catch (exception: KafkaException) {
            stats.recordFailed()
            throw TransactionPublishException(
                "Failed to enqueue transaction ${transaction.transactionId} for delivery",
                exception,
            )
        }
    }

    override fun close() {
        kafkaProducer.close()
    }
}
