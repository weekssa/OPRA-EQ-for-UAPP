package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.kt02h20.Ja11DcrTuning

enum class Ja11DcrTuningPhase {
    UNKNOWN,
    READING,
    APPLYING,
    STOCK,
    ELLYN,
    OTHER,
    ERROR,
}

/**
 * Live state of the JA11 DCR register the community firmware modifies.
 *
 * [OTHER] is honest: the register can legitimately hold a value this app does not recognise, and a
 * mismatched readback must never be presented as the selected tuning.
 */
data class Ja11DcrTuningUiState(
    val phase: Ja11DcrTuningPhase = Ja11DcrTuningPhase.UNKNOWN,
    val value: Int? = null,
    val baseline: Int? = null,
    val error: String? = null,
) {
    val isBusy: Boolean
        get() = phase == Ja11DcrTuningPhase.READING || phase == Ja11DcrTuningPhase.APPLYING

    val tuningEnabled: Boolean
        get() = phase == Ja11DcrTuningPhase.ELLYN

    fun beginRead(): Ja11DcrTuningUiState = copy(phase = Ja11DcrTuningPhase.READING, error = null)

    fun beginApply(): Ja11DcrTuningUiState = copy(phase = Ja11DcrTuningPhase.APPLYING, error = null)

    fun read(value: Int?): Ja11DcrTuningUiState {
        if (value == null) {
            return copy(phase = Ja11DcrTuningPhase.UNKNOWN, value = null, error = null)
        }
        val phase = when {
            Ja11DcrTuning.isStock(value) -> Ja11DcrTuningPhase.STOCK
            Ja11DcrTuning.isEllyn(value) -> Ja11DcrTuningPhase.ELLYN
            else -> Ja11DcrTuningPhase.OTHER
        }
        return copy(phase = phase, value = value, error = null)
    }

    fun failed(message: String): Ja11DcrTuningUiState =
        copy(phase = Ja11DcrTuningPhase.ERROR, error = message)

    fun clear(): Ja11DcrTuningUiState = Ja11DcrTuningUiState()
}

internal fun ja11DcrRowValue(state: Ja11DcrTuningUiState): String = when {
    state.phase == Ja11DcrTuningPhase.ELLYN -> "Ellyn tuning · on"
    state.phase == Ja11DcrTuningPhase.STOCK -> "Stock · off"
    state.phase == Ja11DcrTuningPhase.OTHER && state.value == state.baseline && state.value != null ->
        "Stock · off (device value)"
    state.phase == Ja11DcrTuningPhase.OTHER -> "Unrecognised value"
    state.phase == Ja11DcrTuningPhase.READING || state.phase == Ja11DcrTuningPhase.APPLYING -> "Checking…"
    state.phase == Ja11DcrTuningPhase.ERROR -> "Not current"
    else -> "Unknown"
}
