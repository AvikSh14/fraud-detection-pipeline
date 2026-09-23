package com.pipeline.api.alert

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AlertStatusHistoryRepository : JpaRepository<AlertStatusHistory, UUID> {
    // Derived query: WHERE alert_id = ? ORDER BY changed_at ASC, an alert's timeline oldest first
    fun findByAlertIdOrderByChangedAtAsc(alertId: UUID): List<AlertStatusHistory>
}
