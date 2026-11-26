package com.pipeline.api.alert.metrics

import com.pipeline.api.alert.domain.AlertStatusChangedEvent
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AlertStatusChangeMetrics(private val meterRegistry: MeterRegistry) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onStatusChanged(event: AlertStatusChangedEvent) {
        meterRegistry.counter(
            STATUS_CHANGES_COUNTER,
            "from", event.previousStatus.name,
            "to", event.newStatus.name,
        ).increment()
    }

    companion object {
        const val STATUS_CHANGES_COUNTER = "alerts.status.changes"
    }
}
