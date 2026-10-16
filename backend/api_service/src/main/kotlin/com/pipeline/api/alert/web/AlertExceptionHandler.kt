package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.AlertNotFoundException
import com.pipeline.api.alert.domain.AlertVersionMismatchException
import com.pipeline.api.alert.domain.InvalidStatusTransitionException
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

    @ExceptionHandler(AlertVersionMismatchException::class)
    fun handleVersionMismatch(exception: AlertVersionMismatchException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, exception.message ?: "Alert version mismatch").apply {
            title = "Alert was changed by someone else"
            setProperty("alertId", exception.alertId)
            setProperty("currentVersion", exception.currentVersion)
        }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidTransition(exception: InvalidStatusTransitionException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.message ?: "Status change not allowed").apply {
            title = "Status change not allowed"
            setProperty("currentStatus", exception.currentStatus)
            setProperty("targetStatus", exception.targetStatus)
        }

    @ExceptionHandler(MissingIfMatchException::class)
    fun handleMissingIfMatch(exception: MissingIfMatchException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, exception.message ?: "If-Match header required").apply {
            title = "If-Match header required"
        }

    @ExceptionHandler(InvalidIfMatchException::class)
    fun handleInvalidIfMatch(exception: InvalidIfMatchException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid If-Match header").apply {
            title = "Invalid If-Match header"
            setProperty("ifMatch", exception.headerValue)
        }
}
