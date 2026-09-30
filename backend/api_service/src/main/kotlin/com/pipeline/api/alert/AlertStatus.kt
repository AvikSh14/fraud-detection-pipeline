package com.pipeline.api.alert

enum class AlertStatus {
    NEW,
    IN_REVIEW,
    CONFIRMED_FRAUD,
    FALSE_POSITIVE;

    val allowedNextStatuses: Set<AlertStatus>
        get() = when (this) {
            NEW -> setOf(IN_REVIEW)
            IN_REVIEW -> setOf(CONFIRMED_FRAUD, FALSE_POSITIVE)
            CONFIRMED_FRAUD, FALSE_POSITIVE -> emptySet()
        }

    fun canTransitionTo(targetStatus: AlertStatus) = targetStatus in allowedNextStatuses
}
