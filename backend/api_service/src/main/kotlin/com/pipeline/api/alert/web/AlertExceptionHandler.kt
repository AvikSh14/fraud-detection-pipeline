package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.AlertNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AlertExceptionHandler {

    @ExceptionHandler(AlertNotFoundException::class)
    fun handleAlertNotFound(exception: AlertNotFoundException): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        exception.message ?: "Alert not found"
    ).apply {
        title = "Alert not found"
        setProperty("alertId", exception.alertId)
    }
}
