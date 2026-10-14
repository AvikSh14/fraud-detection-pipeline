package com.pipeline.api.alert.web

import com.pipeline.api.alert.domain.AlertStatus

// Body of PATCH /alerts/{alertId}/status. The id comes from the URL and the version from If-Match
data class StatusChangeRequest(val status: AlertStatus)
