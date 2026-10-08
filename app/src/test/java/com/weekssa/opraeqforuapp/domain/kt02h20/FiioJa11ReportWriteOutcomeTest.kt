package com.weekssa.opraeqforuapp.domain.kt02h20

import org.junit.Assert.assertEquals
import org.junit.Test

class FiioJa11ReportWriteOutcomeTest {
    @Test
    fun completeReportWithStableSessionIsAccepted() {
        assertEquals(
            FiioJa11ReportWriteOutcome.COMPLETED,
            classifyJa11ReportWriteOutcome(
                reportWasComplete = true,
                sessionCurrentBeforeSend = true,
                sessionCurrentAfterSettle = true,
            ),
        )
    }

    @Test
    fun completeReportThenDetachDuringSettleIsAcceptedForFreshSessionVerification() {
        val outcome = classifyJa11ReportWriteOutcome(
            reportWasComplete = true,
            sessionCurrentBeforeSend = true,
            sessionCurrentAfterSettle = false,
        )

        assertEquals(FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE, outcome)
        assertEquals(true, ja11RestartWriteWasAccepted(outcome))
    }

    @Test
    fun incompleteReportIsUncertainEvenIfSessionAlsoChanged() {
        val outcome = classifyJa11ReportWriteOutcome(
            reportWasComplete = false,
            sessionCurrentBeforeSend = true,
            sessionCurrentAfterSettle = false,
        )

        assertEquals(FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN, outcome)
        assertEquals(false, ja11RestartWriteWasAccepted(outcome))
    }

    @Test
    fun staleSessionIsRejectedBeforeTransfer() {
        assertEquals(
            FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND,
            classifyJa11ReportWriteOutcome(
                reportWasComplete = false,
                sessionCurrentBeforeSend = false,
                sessionCurrentAfterSettle = false,
            ),
        )
    }
}
