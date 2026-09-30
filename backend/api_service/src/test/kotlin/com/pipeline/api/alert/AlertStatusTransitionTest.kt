package com.pipeline.api.alert

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class AlertStatusTransitionTest {

    private val acceptedWorkflowTransitions = setOf(
        AlertStatus.NEW to AlertStatus.IN_REVIEW,
        AlertStatus.IN_REVIEW to AlertStatus.CONFIRMED_FRAUD,
        AlertStatus.IN_REVIEW to AlertStatus.FALSE_POSITIVE,
    )

    @Test
    fun `allows exactly accepted workflow transitions`() {
        val everyStatusPair = AlertStatus.entries.flatMap { currentStatus ->
            AlertStatus.entries.map { transitionStatus ->
                currentStatus to transitionStatus
            }
        }

        val allowedPairs = everyStatusPair.filter { (current, target) ->
            current.canTransitionTo(target)
        }.toSet()

        assertThat(allowedPairs).isEqualTo(acceptedWorkflowTransitions)
    }
}
