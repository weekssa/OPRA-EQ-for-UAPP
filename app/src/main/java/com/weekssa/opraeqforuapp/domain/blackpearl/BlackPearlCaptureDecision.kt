package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity

sealed interface BlackPearlCaptureDecision {
    data object Capture : BlackPearlCaptureDecision
    data object Flat : BlackPearlCaptureDecision

    data class ExistingMatch(
        val savedEqs: List<SavedHardwareEqIdentity>,
    ) : BlackPearlCaptureDecision {
        init {
            require(savedEqs.isNotEmpty()) { "Existing capture match requires at least one saved EQ." }
        }
    }
}

/**
 * Decides whether a freshly resolved Black Pearl hardware EQ should create a Personal EQ.
 * Exact native matches link to existing My EQs records instead of creating duplicates.
 */
fun decideBlackPearlCapture(match: HardwareEqMatch): BlackPearlCaptureDecision = when (match) {
    HardwareEqMatch.Flat -> BlackPearlCaptureDecision.Flat
    is HardwareEqMatch.Exact -> BlackPearlCaptureDecision.ExistingMatch(listOf(match.savedEq))
    is AmbiguousExactHardwareEqMatch -> BlackPearlCaptureDecision.ExistingMatch(match.savedEqs)
    is HardwareEqMatch.ModifiedKnown,
    HardwareEqMatch.Unknown,
    -> BlackPearlCaptureDecision.Capture
}
