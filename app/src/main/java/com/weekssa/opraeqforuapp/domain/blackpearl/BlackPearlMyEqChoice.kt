package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord

/**
 * Device-aware My EQs choice for the approved My DAC Change EQ flow.
 *
 * This is presentation-ready domain state, not a second flash planner. Ready choices carry the same
 * deterministic native representation produced by [BlackPearlSavedEqRepresentationDeriver], which
 * itself delegates to the qualified Black Pearl flash-plan authority. Not-suitable choices remain
 * visible with their exact reason and cannot be silently adapted by Compose.
 */
sealed interface BlackPearlMyEqChoice {
    val candidate: BlackPearlSavedEqCandidate

    data class Ready(
        override val candidate: BlackPearlSavedEqCandidate,
        val representation: BlackPearlSavedEqRepresentation,
    ) : BlackPearlMyEqChoice

    data class NotSuitable(
        override val candidate: BlackPearlSavedEqCandidate,
        val reason: String,
    ) : BlackPearlMyEqChoice
}

fun buildBlackPearlMyEqChoices(
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
): List<BlackPearlMyEqChoice> = buildBlackPearlMyEqsCandidates(
    managedHeadphones = managedHeadphones,
    savedEqs = savedEqs,
    savedGeneralEqs = savedGeneralEqs,
).map { candidate ->
    when (val derived = BlackPearlSavedEqRepresentationDeriver.derive(candidate.profile)) {
        is BlackPearlSavedEqRepresentationResult.Ready -> BlackPearlMyEqChoice.Ready(
            candidate = candidate,
            representation = derived.representation,
        )
        is BlackPearlSavedEqRepresentationResult.NotRepresentable -> BlackPearlMyEqChoice.NotSuitable(
            candidate = candidate,
            reason = derived.reason,
        )
    }
}