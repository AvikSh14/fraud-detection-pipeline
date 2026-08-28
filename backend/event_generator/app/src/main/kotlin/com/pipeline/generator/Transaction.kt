package com.pipeline.generator

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val transactionId: String,
    val cardId: String,
    val merchant: String,
    val amountCents: Long,
    val currency: String = "EUR",
    val timestamp: Long,
)