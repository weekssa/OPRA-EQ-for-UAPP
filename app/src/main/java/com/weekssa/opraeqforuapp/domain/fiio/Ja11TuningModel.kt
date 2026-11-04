package com.weekssa.opraeqforuapp.domain.fiio

/**
 * What EQ Library can truthfully establish about JA11 tuning today.
 *
 * The JA11's maintained run-mode protocol (report `0x02`, bands `0x15`, EQ gain `0x17`,
 * Apply `0x18`, preset-valued Save `0x19`, vendor filter `0x04`) contains no DCR, DRC,
 * compander, expander, AGC or noise-gate command. Public community material describes editing
 * those values in KT tools and flashing a firmware image; it is a firmware workflow, not a runtime
 * API, and it publishes no authoritative values. Other KT-based products use a different report
 * (`0x4B`) and different register numbers, so nothing transfers to JA11.
 *
 * This model therefore separates "what the app can do" from "what a simulated prototype shows".
 * It is deliberately not a hardware-support claim.
 */
data class Ja11TuningCapability(
    val runtimeReadEstablished: Boolean,
    val runtimeWriteEstablished: Boolean,
    val installedTuningReadable: Boolean,
    val firmwareOnly: Boolean,
    val evidenceNote: String,
) {
    /** True only when both a read path and a write path are independently established. */
    val hardwareWritesAuthorized: Boolean
        get() = runtimeReadEstablished && runtimeWriteEstablished
}

/**
 * One simulated tuning step. These are interaction steps for the prototype, not documented JA11
 * register values, units or firmware revisions.
 */
enum class Ja11TuningLevel(val id: String, val label: String) {
    STOCK("stock", "Stock"),
    WARMER("warmer", "Warmer"),
    WARMEST("warmest", "Warmest"),
}

/**
 * Where a tuning value is believed to live. [UNKNOWN] is a real state: JA11 exposes no read path
 * for installed tuning, so "unknown" must never be rendered as "Off" or "Stock".
 */
enum class Ja11TuningPersistence {
    UNKNOWN,
    /** Applied in the current simulated session; lost on a simulated power cycle. */
    SESSION_ONLY,
    /** Simulated startup save verified by readback within this session. */
    SAVED,
}

data class Ja11TuningValues(
    val level: Ja11TuningLevel,
    val noiseGateEnabled: Boolean,
)

/**
 * A verified tuning snapshot. A `null` [values] means the state could not be read; callers must
 * render that as unknown rather than substituting a default.
 */
data class Ja11TuningSnapshot(
    val sessionGeneration: Long,
    val values: Ja11TuningValues?,
    val persistence: Ja11TuningPersistence,
) {
    init {
        require(sessionGeneration > 0L) { "sessionGeneration must be positive" }
    }
}

data class Ja11TuningApplyRequest(
    val values: Ja11TuningValues,
    val expectedSessionGeneration: Long,
)

sealed interface Ja11TuningReadOutcome {
    data class Read(val snapshot: Ja11TuningSnapshot) : Ja11TuningReadOutcome

    /** No established read path for this device; nothing is guessed. */
    data object Unsupported : Ja11TuningReadOutcome

    data object PermissionDenied : Ja11TuningReadOutcome

    data class Disconnected(val reason: String) : Ja11TuningReadOutcome

    data class Failed(val reason: String) : Ja11TuningReadOutcome
}

sealed interface Ja11TuningApplyOutcome {
    data class Verified(val snapshot: Ja11TuningSnapshot) : Ja11TuningApplyOutcome

    data class StaleSession(
        val expectedSessionGeneration: Long,
        val actualSessionGeneration: Long,
    ) : Ja11TuningApplyOutcome

    data class VerificationFailed(
        val reason: String,
        val snapshot: Ja11TuningSnapshot?,
    ) : Ja11TuningApplyOutcome

    data object Unsupported : Ja11TuningApplyOutcome

    data object PermissionDenied : Ja11TuningApplyOutcome

    /**
     * [uncertain] is true when the write may have reached the device. Callers must treat that as
     * "completion unknown" and re-read rather than replaying the mutation.
     */
    data class Disconnected(val reason: String, val uncertain: Boolean) : Ja11TuningApplyOutcome

    data class Failed(val reason: String, val uncertain: Boolean) : Ja11TuningApplyOutcome
}
