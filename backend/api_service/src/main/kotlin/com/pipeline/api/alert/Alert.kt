package com.pipeline.api.alert

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alerts")
class Alert(
    @Id
    val id: UUID,

    @Column(name = "transaction_reference", nullable = false)
    val transactionReference: String,

    @Column(name = "risk_score", nullable = false)
    val riskScore: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AlertStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)
