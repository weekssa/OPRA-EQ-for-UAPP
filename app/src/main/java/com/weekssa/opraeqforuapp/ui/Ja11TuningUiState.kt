package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningApplyOutcome
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningCapability
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningController
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningLevel
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningPersistence
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningReadOutcome
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningSnapshot
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningValues

enum class Ja11TuningPhase {
    IDLE,
    READING,
    APPLYING,
    VERIFYING,
    VERIFIED,
    FAILED,
    DISCONNECTED,
    PERMISSION_DENIED,
    UNSUPPORTED,
}

/**
 * Screen state for the JA11 tuning prototype.
 *
 * Every value here is simulated. [level] and [noiseGateEnabled] are nullable because "not readable"
 * is a real state for this device and must not be rendered as a default setting.
 */
data class Ja11TuningUiState(
    val phase: Ja11TuningPhase = Ja11TuningPhase.IDLE,
    val isSimulated: Boolean = true,
    val capability: Ja11TuningCapability = Ja11TuningController.JA11_TESTED_FIRMWARE_2_20_CAPABILITY,
    val level: Ja11TuningLevel? = null,
    val noiseGateEnabled: Boolean? = null,
    val persistence: Ja11TuningPersistence = Ja11TuningPersistence.UNKNOWN,
    val sessionGeneration: Long? = null,
    val isCurrent: Boolean = false,
    val error: String? = null,
) {
    init {
        require(!isCurrent || sessionGeneration != null) {
            "a current tuning state needs a verified session generation"
        }
    }

    val isBusy: Boolean
        get() = phase == Ja11TuningPhase.READING ||
            phase == Ja11TuningPhase.APPLYING ||
            phase == Ja11TuningPhase.VERIFYING

    fun beginRead(): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.READING,
        error = null,
    )

    fun beginApply(values: Ja11TuningValues): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.APPLYING,
        level = values.level,
        noiseGateEnabled = values.noiseGateEnabled,
        error = null,
    )

    fun verifying(): Ja11TuningUiState = copy(phase = Ja11TuningPhase.VERIFYING, error = null)

    fun verified(snapshot: Ja11TuningSnapshot): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.VERIFIED,
        level = snapshot.values?.level,
        noiseGateEnabled = snapshot.values?.noiseGateEnabled,
        persistence = snapshot.persistence,
        sessionGeneration = snapshot.sessionGeneration,
        isCurrent = true,
        error = null,
    )

    fun read(snapshot: Ja11TuningSnapshot): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.IDLE,
        level = snapshot.values?.level,
        noiseGateEnabled = snapshot.values?.noiseGateEnabled,
        persistence = snapshot.persistence,
        sessionGeneration = snapshot.sessionGeneration,
        isCurrent = true,
        error = null,
    )

    fun permissionDenied(): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.PERMISSION_DENIED,
        isCurrent = false,
        error = "Android USB permission is required before this prototype can be read.",
    )

    fun disconnected(reason: String): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.DISCONNECTED,
        isCurrent = false,
        error = reason,
        persistence = Ja11TuningPersistence.UNKNOWN,
    )

    fun unsupported(): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.UNSUPPORTED,
        isCurrent = false,
        error = null,
    )

    fun failed(reason: String, uncertain: Boolean): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.FAILED,
        isCurrent = !uncertain && isCurrent,
        error = reason,
        persistence = if (uncertain) Ja11TuningPersistence.UNKNOWN else persistence,
    )

    fun markStale(): Ja11TuningUiState = copy(
        phase = Ja11TuningPhase.DISCONNECTED,
        isCurrent = false,
        persistence = Ja11TuningPersistence.UNKNOWN,
    )
}

/** Applies a controller outcome to the screen state without inventing success. */
internal fun Ja11TuningUiState.applyOutcome(
    outcome: Ja11TuningApplyOutcome,
): Ja11TuningUiState = when (outcome) {
    is Ja11TuningApplyOutcome.Verified -> verified(outcome.snapshot)
    is Ja11TuningApplyOutcome.StaleSession -> copy(
        phase = Ja11TuningPhase.FAILED,
        isCurrent = false,
        error = "JA11 settings were read in a different session. Read again before applying.",
    )
    is Ja11TuningApplyOutcome.VerificationFailed -> copy(
        phase = Ja11TuningPhase.FAILED,
        isCurrent = false,
        persistence = Ja11TuningPersistence.UNKNOWN,
        level = outcome.snapshot?.values?.level ?: level,
        noiseGateEnabled = outcome.snapshot?.values?.noiseGateEnabled ?: noiseGateEnabled,
        error = outcome.reason,
    )
    Ja11TuningApplyOutcome.Unsupported -> unsupported()
    Ja11TuningApplyOutcome.PermissionDenied -> permissionDenied()
    is Ja11TuningApplyOutcome.Disconnected -> if (outcome.uncertain) {
        copy(
            phase = Ja11TuningPhase.DISCONNECTED,
            isCurrent = false,
            persistence = Ja11TuningPersistence.UNKNOWN,
            error = "Completion is unknown: " + outcome.reason +
                " Read the current values before retrying.",
        )
    } else {
        disconnected(outcome.reason)
    }
    is Ja11TuningApplyOutcome.Failed -> failed(outcome.reason, outcome.uncertain)
}

/** Applies a controller read outcome without presenting unknown values as defaults. */
internal fun Ja11TuningUiState.readOutcome(
    outcome: Ja11TuningReadOutcome,
): Ja11TuningUiState = when (outcome) {
    is Ja11TuningReadOutcome.Read -> read(outcome.snapshot)
    Ja11TuningReadOutcome.Unsupported -> unsupported()
    Ja11TuningReadOutcome.PermissionDenied -> permissionDenied()
    is Ja11TuningReadOutcome.Disconnected -> disconnected(outcome.reason)
    is Ja11TuningReadOutcome.Failed -> failed(outcome.reason, uncertain = false)
}

/** One-line current value for the DEVICE row; never substitutes a default for unknown state. */
internal fun ja11TuningRowValue(state: Ja11TuningUiState): String {
    val level = state.level
    val gate = state.noiseGateEnabled
    if (level == null) {
        return if (state.isSimulated) "Not readable · simulated" else "Not readable · vendor-only"
    }
    val gateLabel = when (gate) {
        true -> "gate on"
        false -> "gate off"
        null -> "gate unknown"
    }
    val prefix = if (state.isCurrent) "" else "Last read · "
    return prefix + level.label + " · " + gateLabel
}

/** Short, truthful persistence line for the prototype dialog. */
internal fun ja11TuningPersistenceText(state: Ja11TuningUiState): String = when (state.persistence) {
    Ja11TuningPersistence.SAVED ->
        "Simulated startup save verified by readback in this session."
    Ja11TuningPersistence.SESSION_ONLY ->
        "Simulated: this value is session-only and is lost on a simulated unplug."
    Ja11TuningPersistence.UNKNOWN ->
        if (state.isCurrent) {
            "Simulated persistence is unknown. This says nothing about JA11 firmware behaviour."
        } else {
            "Simulated persistence unknown until a value is verified in this session."
        }
}

/** The capability and evidence line. Never phrased as hardware support. */
internal fun ja11TuningCapabilityText(capability: Ja11TuningCapability): String =
    if (capability.hardwareWritesAuthorized) {
        "Runtime tuning access is established for this device."
    } else {
        "JA11 runtime tuning is not established: no read or write command exists in the maintained " +
            "protocol. Community tuning is a firmware workflow applied outside EQ Library. " +
            "EQ Library sends nothing to the DAC from this prototype."
    }
