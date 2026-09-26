package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStage
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace

internal const val FIIO_JA11_READABLE_REPORT_LABEL = "Share operation report"
internal const val FIIO_JA11_TECHNICAL_REPORT_LABEL = "Share technical report (JSON)"

internal data class FiioJa11OperationStatusPresentation(
    val message: String,
    val verified: Boolean,
)

internal fun fiioJa11OperationControlsEnabled(
    operationStatus: FiioJa11OperationStatus,
): Boolean = operationStatus !is FiioJa11OperationStatus.Running

internal fun fiioJa11OperationStatusPresentation(
    trace: FiioJa11OperationTrace,
): FiioJa11OperationStatusPresentation {
    val operation = trace.operation
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
    val reconnectMessage = if (trace.usbSessionChangeObserved()) {
        " A USB session change was observed before the final readback."
    } else {
        " Reconnect persistence was not tested in this operation."
    }
    val verified = trace.isSaveAndFinalReadbackVerified()

    if (verified) {
        val successMessage = when (trace.operation) {
            "RESET" -> "FiiO JA11 EQ was reset to flat, saved, and verified."
            else -> "FiiO JA11 EQ was saved and verified."
        }
        return FiioJa11OperationStatusPresentation(
            message = "✓ $successMessage Final hardware readback matched.$reconnectMessage",
            verified = true,
        )
    }

    val stateMessage = when {
        trace.saveCommandCount == 0L ->
            "The operation stopped before persistent Save; the requested state was not verified."
        trace.comparisonPhase == "FINAL_READBACK" ->
            "Save was sent, but final hardware readback did not establish the requested state."
        trace.stateKnown ->
            "The requested state was not verified."
        else ->
            "The JA11 state could not be confirmed after this operation."
    }
    val reason = trace.failureReason?.takeIf { it.isNotBlank() }?.let { " Reason: $it" } ?: ""
    return FiioJa11OperationStatusPresentation(
        message = "Last FiiO JA11 $operation was not verified. $stateMessage$reconnectMessage$reason Do not retry the hardware action; share the operation report before any later write.",
        verified = false,
    )
}

internal fun fiioJa11OperationReportDescription(trace: FiioJa11OperationTrace): String =
    if (trace.isSaveAndFinalReadbackVerified()) {
        "Keep this readable or technical report if support asks for exact transaction evidence."
    } else {
        "Share this report before any later write so the exact values, phases, USB bytes, and session evidence remain attached to the failed operation."
    }

internal fun FiioJa11OperationTrace.isSaveAndFinalReadbackVerified(): Boolean =
    stateKnown &&
        outcome == "Success" &&
        comparisonPhase == "FINAL_READBACK" &&
        saveCommandCount == 1L &&
        FiioJa11OperationStage.FINAL_READBACK in stages &&
        FiioJa11OperationStage.VERIFIED in stages

internal fun FiioJa11OperationTrace.usbSessionChangeObserved(): Boolean =
    events.any { event ->
        event.sessionGeneration > sessionGeneration || event.detachGeneration > detachGeneration
    }
