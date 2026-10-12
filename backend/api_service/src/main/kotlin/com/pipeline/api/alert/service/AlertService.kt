package com.pipeline.api.alert.service

import com.pipeline.api.alert.domain.Alert
import com.pipeline.api.alert.domain.AlertNotFoundException
import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.domain.AlertStatusHistory
import com.pipeline.api.alert.domain.AlertVersionMismatchException
import com.pipeline.api.alert.domain.InvalidStatusTransitionException
import com.pipeline.api.alert.repository.AlertRepository
import com.pipeline.api.alert.repository.AlertStatusHistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val alertStatusHistoryRepository: AlertStatusHistoryRepository,
    private val clock: Clock
) {

    @Transactional
    fun changeStatus(
        alertId: UUID,
        targetStatus: AlertStatus,
        expectedVersion: Long,
        changedBy: String,
        ) : Alert {

        val alert = findAlert(alertId)

        if (alert.version != expectedVersion) {
            throw AlertVersionMismatchException(
                alertId = alertId,
                expectedVersion = expectedVersion,
                currentVersion = alert.version
            )
        }
        if (!alert.status.canTransitionTo(targetStatus)) {
            throw InvalidStatusTransitionException(
                currentStatus = alert.status,
                targetStatus = targetStatus,
            )
        }

        val previousStatus = alert.status
        val now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)
        alert.status = targetStatus
        alert.updatedAt = now

        alertStatusHistoryRepository.save(
            AlertStatusHistory(
                id = UUID.randomUUID(),
                alert = alert,
                previousStatus = previousStatus,
                newStatus = targetStatus,
                changedBy = changedBy,
                changedAt = now
            )
        )

        return alert
    }

    @Transactional(readOnly = true)
    fun getAlert(alertId: UUID): Alert = findAlert(alertId)

    @Transactional(readOnly = true)
    fun listAlerts(status: AlertStatus?, pageable: Pageable): Page<Alert> {
        return if (status == null) alertRepository.findAll(pageable)
        else alertRepository.findByStatus(status, pageable)
    }

    @Transactional(readOnly = true)
    fun getStatusHistory(alertId: UUID): List<AlertStatusHistory> {
        if (!alertRepository.existsById(alertId)) throw AlertNotFoundException(alertId)
        return alertStatusHistoryRepository.findByAlertIdOrderByChangedAtAsc(alertId)
    }

    private fun findAlert(alertId: UUID): Alert =
        alertRepository.findById(alertId).orElseThrow { AlertNotFoundException(alertId) }
}
