package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.service.AlertService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/alerts")
class AlertController(
    private val alertService: AlertService
) {
    @GetMapping
    fun listAlerts(
        @RequestParam(required = false) status: AlertStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): PageResponse<AlertResponse> =
        PageResponse.from(alertService.listAlerts(status, pageable).map { it.toResponse() })

    @GetMapping("/{alertId}")
    fun getAlert(
        @PathVariable alertId: UUID
    ): ResponseEntity<AlertResponse> {
        val alertResponse = alertService.getAlert(alertId).toResponse()
        return ResponseEntity.ok().eTag(alertResponse.version.toString()).body(alertResponse)
    }

    @GetMapping("/{alertId}/history")
    fun getStatusHistory(@PathVariable alertId: UUID): List<StatusChangeResponse> =
        alertService.getStatusHistory(alertId).map { it.toResponse() }

    // Optimistic locking over HTTP: the client sends back the ETag it read, and the change only
    // applies if the alert is still at that version. The response carries the new ETag.
    @PatchMapping("/{alertId}/status")
    fun changeStatus(
        @PathVariable alertId: UUID,
        // Not required for Spring, so a missing header reaches parseVersion and becomes 428 instead of a generic 400
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
        @RequestBody request: StatusChangeRequest,
    ): ResponseEntity<AlertResponse> {
        val alertResponse = alertService.changeStatus(
            alertId = alertId,
            targetStatus = request.status,
            expectedVersion = parseVersion(ifMatch),
            changedBy = UNKNOWN_ANALYST,
        ).toResponse()
        return ResponseEntity.ok().eTag(alertResponse.version.toString()).body(alertResponse)
    }

    // "\"3\"" -> 3. A weak tag (W/"3"), "*" or any non-number is rejected: none can name a single version
    private fun parseVersion(ifMatch: String?): Long {
        if (ifMatch == null) throw MissingIfMatchException()
        return ifMatch.removeSurrounding("\"").toLongOrNull() ?: throw InvalidIfMatchException(ifMatch)
    }

    companion object {
        // Replaced by the logged-in user's id once authentication lands (FDF-3)
        const val UNKNOWN_ANALYST = "unknown"
    }
}
