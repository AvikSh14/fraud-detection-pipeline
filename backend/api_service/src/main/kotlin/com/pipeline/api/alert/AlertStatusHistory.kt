package com.pipeline.api.alert

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alert_status_history")
class AlertStatusHistory(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    val alert: Alert,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    val previousStatus: AlertStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    val newStatus: AlertStatus,

    @Column(name = "changed_by", nullable = false)
    val changedBy: String,

    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant
)
