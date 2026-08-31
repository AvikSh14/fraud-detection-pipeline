package com.pipeline.generator.generator

import com.pipeline.generator.model.Transaction
import java.util.UUID
import kotlin.random.Random

object TransactionGenerator {

    private val merchants = listOf(
        "Coffee Shop",
        "Grocery Store",
        "Gas Station",
        "Online Retailer",
        "Restaurant",
        "Pharmacy",
    )

    private val cardIds: List<String> = (1..20).map { "card-%04d".format(it) }

    fun random(now: Long = System.currentTimeMillis()): Transaction =
        Transaction(
            transactionId = UUID.randomUUID().toString(),
            cardId = cardIds.random(),
            merchant = merchants.random(),
            amountCents = Random.nextLong(from = 100, until = 20_000),
            timestamp = now,
        )
}