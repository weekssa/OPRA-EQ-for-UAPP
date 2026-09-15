package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BlackPearlCaptureDecisionTest {
    @Test
    fun flatDoesNotCreatePersonalEq() {
        assertSame(BlackPearlCaptureDecision.Flat, decideBlackPearlCapture(HardwareEqMatch.Flat))
    }

    @Test
    fun unknownAndModifiedKnownCanBeCaptured() {
        assertSame(BlackPearlCaptureDecision.Capture, decideBlackPearlCapture(HardwareEqMatch.Unknown))
        assertSame(
            BlackPearlCaptureDecision.Capture,
            decideBlackPearlCapture(
                HardwareEqMatch.ModifiedKnown(
                    savedEq = identity("known", "Known EQ"),
                    differences = listOf(
                        com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifference(
                            bandIndex = 0,
                            field = com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifferenceField.GAIN_DB,
                            expectedValue = "-2.0",
                            actualValue = "-2.5",
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun exactMatchLinksInsteadOfDuplicating() {
        val existing = identity("saved:one", "Existing EQ")
        val decision = decideBlackPearlCapture(HardwareEqMatch.Exact(existing))

        assertEquals(
            BlackPearlCaptureDecision.ExistingMatch(listOf(existing)),
            decision,
        )
    }

    @Test
    fun ambiguousExactMatchRetainsEveryExactIdentity() {
        val first = identity("saved:first", "First")
        val second = identity("saved:second", "Second")
        val decision = decideBlackPearlCapture(
            AmbiguousExactHardwareEqMatch(listOf(first, second)),
        )

        assertEquals(
            BlackPearlCaptureDecision.ExistingMatch(listOf(first, second)),
            decision,
        )
    }

    private fun identity(key: String, name: String) = SavedHardwareEqIdentity(
        savedEqKey = key,
        displayName = name,
    )
}
