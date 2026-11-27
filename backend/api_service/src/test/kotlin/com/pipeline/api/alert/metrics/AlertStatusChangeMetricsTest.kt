package com.pipeline.api.alert.metrics

import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.domain.AlertStatusChangedEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

class AlertStatusChangeMetricsTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val alertStatusChangeMetrics = AlertStatusChangeMetrics(meterRegistry)

    @Test
    fun `counts each status change tagged with its from and to status`() {
        alertStatusChangeMetrics.onStatusChanged(statusChanged(AlertStatus.NEW, AlertStatus.IN_REVIEW))
        alertStatusChangeMetrics.onStatusChanged(statusChanged(AlertStatus.NEW, AlertStatus.IN_REVIEW))
        alertStatusChangeMetrics.onStatusChanged(statusChanged(AlertStatus.IN_REVIEW, AlertStatus.CONFIRMED_FRAUD))

        assertThat(statusChangeCount(AlertStatus.NEW, AlertStatus.IN_REVIEW)).isEqualTo(2.0)
        assertThat(statusChangeCount(AlertStatus.IN_REVIEW, AlertStatus.CONFIRMED_FRAUD)).isEqualTo(1.0)
        assertThat(statusChangeCount(AlertStatus.IN_REVIEW, AlertStatus.FALSE_POSITIVE)).isEqualTo(0.0)
    }

    private fun statusChanged(previousStatus: AlertStatus, newStatus: AlertStatus) =
        AlertStatusChangedEvent(UUID.randomUUID(), previousStatus, newStatus)

    private fun statusChangeCount(previousStatus: AlertStatus, newStatus: AlertStatus): Double =
        meterRegistry.find(AlertStatusChangeMetrics.STATUS_CHANGES_COUNTER)
            .tags("from", previousStatus.name, "to", newStatus.name)
            .counter()?.count() ?: 0.0
}
