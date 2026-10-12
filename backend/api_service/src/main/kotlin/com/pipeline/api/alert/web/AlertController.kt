package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.AlertStatus
import com.pipeline.api.alert.service.AlertService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
}
