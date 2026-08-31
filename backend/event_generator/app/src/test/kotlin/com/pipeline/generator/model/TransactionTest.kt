package com.pipeline.generator.model

import com.pipeline.generator.generator.TransactionGenerator
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionTest {

    @Test
    fun `serializes to the expected json shape`() {
        val transaction = Transaction(
            transactionId = "t-1",
            cardId = "card-0001",
            merchant = "Coffee Shop",
            amountCents = 4250,
            currency = "USD",
            timestamp = 1_700_000_000_000,
        )

        val json = Json.encodeToString(transaction)

        assertEquals(
            """{"transactionId":"t-1","cardId":"card-0001","merchant":"Coffee Shop","amountCents":4250,"currency":"USD","timestamp":1700000000000}""",
            json,
        )
    }

    @Test
    fun `generator produces values within the configured ranges`() {
        repeat(1_000) {
            val transaction = TransactionGenerator.random(now = 1_700_000_000_000)

            assertTrue(transaction.amountCents in 100 until 20_000, "amount out of range: ${transaction.amountCents}")
            assertTrue(transaction.cardId.startsWith("card-"), "unexpected cardId: ${transaction.cardId}")
            assertEquals(1_700_000_000_000, transaction.timestamp)
        }
    }
}