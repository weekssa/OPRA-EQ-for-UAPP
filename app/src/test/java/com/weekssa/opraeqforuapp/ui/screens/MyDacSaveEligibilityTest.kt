package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifference
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifferenceField
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyDacSaveEligibilityTest {
    @Test
    fun currentConnectedUnknownCanBeSaved() {
        assertTrue(
            shouldOfferSaveDacEq(
                match = HardwareEqMatch.Unknown,
                freshness = DacStateFreshness.CURRENT,
                connected = true,
            ),
        )
    }

    @Test
    fun modifiedKnownCanBeSavedOnlyWhenCurrentAndConnected() {
        val modified = HardwareEqMatch.ModifiedKnown(
            savedEq = SavedHardwareEqIdentity("saved:eq", "Known EQ"),
            differences = listOf(
                HardwareEqDifference(
                    bandIndex = 0,
                    field = HardwareEqDifferenceField.GAIN_DB,
                    expectedValue = "-2.0",
                    actualValue = "-2.5",
                ),
            ),
        )
        assertTrue(shouldOfferSaveDacEq(modified, DacStateFreshness.CURRENT, true))
        assertFalse(shouldOfferSaveDacEq(modified, DacStateFreshness.LAST_READ_STALE, true))
        assertFalse(shouldOfferSaveDacEq(modified, DacStateFreshness.CURRENT, false))
    }

    @Test
    fun flatAndExactNeverOfferDuplicateSave() {
        assertFalse(shouldOfferSaveDacEq(HardwareEqMatch.Flat, DacStateFreshness.CURRENT, true))
        assertFalse(
            shouldOfferSaveDacEq(
                HardwareEqMatch.Exact(SavedHardwareEqIdentity("saved:eq", "Known EQ")),
                DacStateFreshness.CURRENT,
                true,
            ),
        )
    }
}
