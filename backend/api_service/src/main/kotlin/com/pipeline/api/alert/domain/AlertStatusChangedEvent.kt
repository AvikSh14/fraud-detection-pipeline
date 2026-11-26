package com.pipeline.api.alert.domain

import java.util.UUID

data class AlertStatusChangedEvent(
    val alertId: UUID,
    val previousStatus: AlertStatus,
    val newStatus: AlertStatus,
)
