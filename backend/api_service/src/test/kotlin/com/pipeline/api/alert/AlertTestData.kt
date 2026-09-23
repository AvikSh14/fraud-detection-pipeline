package com.pipeline.api.alert

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

// Postgres TIMESTAMPTZ keeps microseconds; truncating keeps values equal after a round trip
fun nowInMicros(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

fun newAlert(status: AlertStatus = AlertStatus.NEW): Alert {
    val now = nowInMicros()
    return Alert(
        id = UUID.randomUUID(),
        transactionReference = "txn-ref",
        riskScore = 0.56,
        status = status,
        createdAt = now,
        updatedAt = now
    )
}

fun newStatusChange(
    alert: Alert,
    previousStatus: AlertStatus,
    newStatus: AlertStatus,
    changedAt: Instant = nowInMicros()
) = AlertStatusHistory(
    id = UUID.randomUUID(),
    alert = alert,
    previousStatus = previousStatus,
    newStatus = newStatus,
    changedBy = "analyst@example.com",
    changedAt = changedAt
)
