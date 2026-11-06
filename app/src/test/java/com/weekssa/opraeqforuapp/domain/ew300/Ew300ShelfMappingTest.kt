package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300ShelfMappingTest {
    private fun bands(): List<Kt02h20Band> = listOf(
        Kt02h20Band("low_shelf", 250.0, -6.0, 0.707),
        Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 2_000.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 4_000.0, 0.0, 1.0),
        Kt02h20Band("high_shelf", 4_000.0, -6.0, 0.707),
    )

    @Test
    fun typeThreeAndFourUseTheExactAAndBWords() {
        val low = Ew300Protocol.encodeBand(
            Kt02h20Band("low_shelf", 250.0, -6.0, 0.707),
            allowExperimentalShelf = true,
        )
        val high = Ew300Protocol.encodeBand(
            Kt02h20Band("high_shelf", 4_000.0, -6.0, 0.707),
            allowExperimentalShelf = true,
        )
        assertArrayEquals(byteArrayOf(0xC4.toByte(), 0xFF.toByte(), 0xFA.toByte(), 0x00), low.first)
        assertArrayEquals(byteArrayOf(0xC3.toByte(), 0x02, 0x03, 0x00), low.second)
        assertArrayEquals(byteArrayOf(0xC4.toByte(), 0xFF.toByte(), 0xA0.toByte(), 0x0F), high.first)
        assertArrayEquals(byteArrayOf(0xC3.toByte(), 0x02, 0x04, 0x00), high.second)
    }

    @Test
    fun parityEditorExposesShelvesWithTheCutOnlyFixedQEnvelope() {
        val spec = HardwareEqEditSpecs.SIMGOT_EW300_PARITY
        assertTrue(spec.supportedFilterTypes.contains(EqFilterType.LOW_SHELF))
        assertTrue(spec.supportedFilterTypes.contains(EqFilterType.HIGH_SHELF))
        assertEquals(-6.0, spec.experimentalShelfGainRangeDb?.minimum ?: 0.0, 0.0)
        assertEquals(0.0, spec.experimentalShelfGainRangeDb?.maximum ?: 1.0, 0.0)
        assertEquals(0.707, spec.experimentalShelfQ ?: 0.0, 0.0)
    }

    @Test
    fun shelfCaptureCarriesNativeTypesAndValues() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = bands(),
                globalGainDb = -6.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
                allowExperimentalShelf = true,
            ),
        )
        val draft = buildEw300CapturedEqDraft(
            captureId = "shelf-capture",
            snapshotBundle = bundle,
            association = null,
            allowExperimentalShelf = true,
        )
        assertEquals("low_shelf", draft.profile.bands?.first()?.type)
        assertEquals("high_shelf", draft.profile.bands?.last()?.type)
        assertEquals(-6.0, draft.profile.bands?.first()?.gainDb ?: 0.0, 0.0)
        assertEquals(0.707, draft.profile.bands?.first()?.q ?: 0.0, 0.0)
    }

    @Test
    fun shelfEditorRejectsQOutsideTheExperimentalEnvelope() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = bands(),
                globalGainDb = -6.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
                allowExperimentalShelf = true,
            ),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(bundle),
            spec = HardwareEqEditSpecs.SIMGOT_EW300_PARITY,
        ) as HardwareEqEditorStartResult.Ready
        val updated = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.SIMGOT_EW300_PARITY,
            bandIndex = 0,
            type = EqFilterType.LOW_SHELF,
            frequencyHz = 250.0,
            gainDb = -3.0,
            q = 0.8,
        )
        assertTrue(updated.hasBlockingIssues)
        assertFalse(updated.issues.none { it is com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditIssue.OutOfRange })
    }
}

