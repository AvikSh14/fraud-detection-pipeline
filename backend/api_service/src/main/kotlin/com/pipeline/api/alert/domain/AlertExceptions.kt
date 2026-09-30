package com.pipeline.api.alert.domain

import java.util.UUID

class AlertNotFoundException(val alertId: UUID) : RuntimeException("Alert $alertId not found")

class AlertVersionMismatchException(
    val alertId: UUID,
    val expectedVersion: Long,
    val currentVersion: Long?,
) : RuntimeException("Alert $alertId is at version $currentVersion, but the request expected $expectedVersion")

class InvalidStatusTransitionException(
    val currentStatus: AlertStatus,
    val targetStatus: AlertStatus,
) : RuntimeException("An alert cannot move from $currentStatus to $targetStatus")
