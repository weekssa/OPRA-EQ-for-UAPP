package com.weekssa.opraeqforuapp.domain.kt02h20

/** Logical outcome of one JA11 HID report write, including the restart race boundary. */
enum class FiioJa11ReportWriteOutcome {
    /** The full report completed and the authorized USB session remained current. */
    COMPLETED,

    /** The full report completed, then the USB session changed during its settle interval. */
    COMPLETED_WITH_SESSION_CHANGE,

    /** No report was issued because the expected session was already stale. */
    STALE_BEFORE_SEND,

    /** Android did not confirm the full report length; device state may be uncertain. */
    INCOMPLETE_OR_UNKNOWN,
}

internal fun ja11RestartWriteWasAccepted(outcome: FiioJa11ReportWriteOutcome): Boolean =
    outcome == FiioJa11ReportWriteOutcome.COMPLETED ||
        outcome == FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE

internal fun classifyJa11ReportWriteOutcome(
    reportWasComplete: Boolean,
    sessionCurrentBeforeSend: Boolean,
    sessionCurrentAfterSettle: Boolean,
): FiioJa11ReportWriteOutcome = when {
    !sessionCurrentBeforeSend -> FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
    !reportWasComplete -> FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
    sessionCurrentAfterSettle -> FiioJa11ReportWriteOutcome.COMPLETED
    else -> FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE
}
