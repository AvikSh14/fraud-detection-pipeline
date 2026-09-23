package com.pipeline.api.alert

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AlertRepository : JpaRepository<Alert, UUID>
