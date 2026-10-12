package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.Alert
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.domain.AlertStatusHistory
import java.time.Instant
import java.util.UUID

data class AlertResponse(
    val id: UUID,
    val transactionReference: String,
    val riskScore: Double,
    val status: AlertStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)

data class StatusChangeResponse(
    val previousStatus: AlertStatus,
    val newStatus: AlertStatus,
    val changedBy: String,
    val changedAt: Instant,
)

fun Alert.toResponse() = AlertResponse(
    id = id,
    transactionReference = transactionReference,
    riskScore = riskScore,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = checkNotNull(version) { "Alert $id was never saved" },
)

fun AlertStatusHistory.toResponse() = StatusChangeResponse(previousStatus, newStatus, changedBy, changedAt)
