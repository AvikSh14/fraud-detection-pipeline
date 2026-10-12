package com.pipeline.api.alert.repository

import com.pipeline.api.alert.domain.Alert
import com.pipeline.api.alert.domain.AlertStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AlertRepository : JpaRepository<Alert, UUID> {
    fun findByStatus(status: AlertStatus, pageable: Pageable): Page<Alert>
}
