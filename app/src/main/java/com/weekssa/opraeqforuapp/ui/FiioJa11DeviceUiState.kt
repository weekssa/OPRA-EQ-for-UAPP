package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.dac.FiioJa11PendingRestartWrite
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsOperationReport
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsEqReadback
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsMechanism
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsRequest
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsStage
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot

/**
 * Whether the device's *startup* program/volume is known to match what the screen shows.
 *
 * The JA11 exposes no read path for its stored startup defaults, so this is never inferred from a
 * runtime readback or a cached value. Session loss returns it to [UNKNOWN] rather than replaying an
 * old claim.
 */
enum class FiioJa11StartupDefaultStatus {
    UNKNOWN,
    /** Applied at runtime only; these values will not survive unplugging. */
    RUNTIME_ONLY,
    /** Committed and verified by readback during this session. */
    SAVED,
}

data class FiioJa11DeviceUiState(
    val isReading: Boolean = false,
    val isWriting: Boolean = false,
    val isSavingDefaults: Boolean = false,
    val activeWriteControlId: DacControlId? = null,
    val snapshot: FiioJa11DeviceSnapshot? = null,
    val isCurrentSession: Boolean = false,
    val pendingRestartWrite: FiioJa11PendingRestartWrite? = null,
    val lastVerifiedWriteControlId: DacControlId? = null,
    val error: String? = null,
    val defaultsReport: FiioJa11DefaultsOperationReport? = null,
    val startupDefaultStatus: FiioJa11StartupDefaultStatus = FiioJa11StartupDefaultStatus.UNKNOWN,
) {
    init {
        require(isWriting == (activeWriteControlId != null))
        require(!isCurrentSession || snapshot != null)
    }

    /**
     * A reconnect-verification transaction is intentionally busy even when no USB transfer is
     * currently in flight. Treating that gap as idle would let a manual refresh overwrite the
     * pending intent before the replacement JA11 session can be verified.
     */
    val isBusy: Boolean
        get() = isReading || isWriting || isSavingDefaults || pendingRestartWrite != null

    fun beginDefaultsSave(): FiioJa11DeviceUiState = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = true, lastVerifiedWriteControlId = null, error = null,
        defaultsReport = null,
    )

    fun defaultsVerified(snapshot: FiioJa11DeviceSnapshot, report: FiioJa11DefaultsOperationReport) = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = false, snapshot = snapshot, isCurrentSession = true,
        pendingRestartWrite = null, lastVerifiedWriteControlId = null, error = null,
        defaultsReport = report,
        startupDefaultStatus = FiioJa11StartupDefaultStatus.SAVED,
    )

    fun defaultsFailure(
        message: String,
        report: FiioJa11DefaultsOperationReport? = null,
        lastVerifiedSnapshot: FiioJa11DeviceSnapshot? = snapshot,
    ) = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = false, snapshot = lastVerifiedSnapshot, isCurrentSession = false, pendingRestartWrite = null,
        lastVerifiedWriteControlId = null, error = message, defaultsReport = report,
        startupDefaultStatus = FiioJa11StartupDefaultStatus.UNKNOWN,
    )

    fun beginRead(): FiioJa11DeviceUiState = copy(
        isReading = true,
        isWriting = false,
        activeWriteControlId = null,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun readSuccess(snapshot: FiioJa11DeviceSnapshot): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        snapshot = snapshot,
        isCurrentSession = true,
        lastVerifiedWriteControlId = null,
        error = null,
        startupDefaultStatus = if (this.snapshot?.sessionGeneration == snapshot.sessionGeneration) {
            startupDefaultStatus
        } else {
            FiioJa11StartupDefaultStatus.UNKNOWN
        },
    )

    fun beginWrite(controlId: DacControlId): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = true,
        activeWriteControlId = controlId,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun verified(
        controlId: DacControlId,
        snapshot: FiioJa11DeviceSnapshot,
    ): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        snapshot = snapshot,
        isCurrentSession = true,
        pendingRestartWrite = null,
        lastVerifiedWriteControlId = controlId,
        error = null,
        startupDefaultStatus = when (controlId) {
            FiioJa11DeviceControls.OUTPUT_VOLUME,
            FiioJa11DeviceControls.EQ_PROGRAM,
            -> FiioJa11StartupDefaultStatus.RUNTIME_ONLY
            else -> startupDefaultStatus
        },
    )

    fun reconnectRequired(pending: FiioJa11PendingRestartWrite): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
        pendingRestartWrite = pending,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun failure(message: String): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
        pendingRestartWrite = null,
        lastVerifiedWriteControlId = null,
        error = message,
        startupDefaultStatus = FiioJa11StartupDefaultStatus.UNKNOWN,
    )

    fun markStale(): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
        startupDefaultStatus = FiioJa11StartupDefaultStatus.UNKNOWN,
    )

    fun withSessionCurrent(current: Boolean): FiioJa11DeviceUiState = copy(
        isCurrentSession = snapshot != null && current,
    )
}

internal fun fiioJa11DefaultsReportIsVerified(report: FiioJa11DefaultsOperationReport): Boolean =
    report.stage == FiioJa11DefaultsStage.COMPLETE &&
        report.eqReadback != FiioJa11DefaultsEqReadback.CHANGED

internal fun fiioJa11DefaultsReportText(report: FiioJa11DefaultsOperationReport): String {
    if (fiioJa11DefaultsReportIsVerified(report)) {
        return when (report.mechanism) {
            FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE ->
                "Saved to JA11. Volume ${report.requestedVolume} and " +
                    "${report.requestedProgram.technicalLabel} are set. The active EQ and microphone " +
                    "setting were preserved and verified by readback."
            FiioJa11DefaultsMechanism.MIC_RECONNECT ->
                buildString {
                    append("Defaults verified. Volume ${report.requestedVolume} and ")
                    append("${report.requestedProgram.technicalLabel} are set. ")
                    append(
                        when (report.eqReadback) {
                            FiioJa11DefaultsEqReadback.MATCHED ->
                                "The existing User 1 EQ was not changed and matched on readback. "
                            FiioJa11DefaultsEqReadback.UNAVAILABLE ->
                                "The existing User 1 EQ was not written, but was not compared. "
                            FiioJa11DefaultsEqReadback.CHANGED -> ""
                        },
                    )
                    append("The microphone setting was restored and verified after the brief disconnects.")
                }
        }
    }

    val recovery = when (report.mechanism) {
        FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE ->
            "Read the current JA11 EQ and settings before retrying."
        FiioJa11DefaultsMechanism.MIC_RECONNECT ->
            "The microphone state needs a fresh read before retrying."
    }
    return "Defaults were not verified. Last confirmed: " +
        "${report.lastVerifiedSnapshot?.outputVolume ?: "unknown"} / " +
        "${report.lastVerifiedSnapshot?.eqProgram?.technicalLabel ?: "unknown"}. $recovery"
}

internal fun fiioJa11DefaultsEqReadbackText(report: FiioJa11DefaultsOperationReport): String? =
    when (report.eqReadback) {
        FiioJa11DefaultsEqReadback.MATCHED ->
            "Final EQ readback matched."
        FiioJa11DefaultsEqReadback.CHANGED ->
            "Final EQ readback did not match. Review the JA11 EQ before use."
        FiioJa11DefaultsEqReadback.UNAVAILABLE ->
            "Final EQ readback was unavailable."
    }

internal fun fiioJa11DefaultsConfirmationText(
    request: FiioJa11DefaultsRequest,
    mechanism: FiioJa11DefaultsMechanism =
        FiioJa11DefaultsMechanism.forRequestedProgram(request.requestedProgram),
): String {
    val target = "volume ${request.requestedVolume} and ${request.requestedProgram.technicalLabel}"
    return when (mechanism) {
        FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE ->
            "Save $target as JA11's startup defaults? EQ Library keeps the active EQ and microphone " +
                "setting as they are. JA11 may disconnect briefly while saving."
        FiioJa11DefaultsMechanism.MIC_RECONNECT ->
            run {
                val reviewedOn = request.reviewedSnapshot.headsetControlEnabled
                val reviewed = if (reviewedOn) "On" else "Off"
                val switched = if (reviewedOn) "Off" else "On"
                "Save $target? EQ Library will not write the User 1 EQ. " +
                    "Reviewed microphone setting: $reviewed. EQ Library will switch it to $switched, " +
                    "then back to $reviewed with two USB audio reconnects before verifying it."
            }
    }
}

/**
 * Short, truthful startup-default line for the DEVICE screen. A runtime-only edit is the state the
 * owner reported as "works until I unplug it", so it names the consequence directly.
 */
internal fun fiioJa11StartupStatusText(status: FiioJa11StartupDefaultStatus): String? = when (status) {
    FiioJa11StartupDefaultStatus.RUNTIME_ONLY ->
        "Set for this session only. Unplugging JA11 restores its saved startup settings."
    FiioJa11StartupDefaultStatus.SAVED ->
        "Saved for startup and verified by readback."
    FiioJa11StartupDefaultStatus.UNKNOWN -> null
}
