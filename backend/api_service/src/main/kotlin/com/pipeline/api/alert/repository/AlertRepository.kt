package com.pipeline.api.alert.repository

import com.pipeline.api.alert.domain.Alert
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AlertRepository : JpaRepository<Alert, UUID>
