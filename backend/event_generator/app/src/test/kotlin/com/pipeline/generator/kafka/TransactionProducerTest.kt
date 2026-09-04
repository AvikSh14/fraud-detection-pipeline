package com.pipeline.generator.kafka

import com.pipeline.generator.model.Transaction
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionProducerTest {
    private val topic = "transactions"
    private fun transaction(cardId: String = "card-007") = Transaction(
        transactionId = "txn-1",
        cardId = cardId,
        merchant = "Coffee shop",
        amountCents = 500,
        timestamp = 0L
    )

    private fun mockProducer(autoComplete: Boolean = true) = MockProducer(
        autoComplete,
        StringSerializer(),
        StringSerializer(),
    )
    private fun transactionProducer(kafkaProducer: Producer<String, String>) =
        TransactionProducer(kafkaProducer, topic)

    @Test
    fun `sends a record keyed by card id with the transaction as json`() {
        val kafkaProducer = mockProducer()
        val transactionProducer = transactionProducer(kafkaProducer)
        val cardId = "LM-10"
        transactionProducer.send(transaction(cardId))
        val producerRecordHistory = kafkaProducer.history()
        assertEquals(1, producerRecordHistory.size)
        assertEquals(topic, producerRecordHistory[0].topic())
        assertEquals(cardId, producerRecordHistory[0].key())
        assertEquals(transaction(cardId), Json.decodeFromString(producerRecordHistory[0].value()))
    }

    @Test
    fun `increments the sent counter when delivery succeeds`() {
        val kafkaProducer = mockProducer()
        val transactionProducer = transactionProducer(kafkaProducer)
        transactionProducer.send(transaction())
        assertEquals(Snapshot(sent = 1L, failed = 0L), transactionProducer.stats.snapshot())
    }

    @Test
    fun `increments the failed counter when delivery fails`() {
        val kafkaProducer = mockProducer(autoComplete = false)
        val transactionProducer = transactionProducer(kafkaProducer)
        transactionProducer.send(transaction())
        kafkaProducer.errorNext(RuntimeException("Broker Down"))
        assertEquals(Snapshot(sent = 0L, failed = 1L), transactionProducer.stats.snapshot())

    }

    @Test
    fun `closes the underlying kafka producer`() {
        val kafkaProducer = mockProducer()
        val transactionProducer = transactionProducer(kafkaProducer)
        transactionProducer.send(transaction())
        transactionProducer.close()
        assertTrue(kafkaProducer.closed())
    }
}
