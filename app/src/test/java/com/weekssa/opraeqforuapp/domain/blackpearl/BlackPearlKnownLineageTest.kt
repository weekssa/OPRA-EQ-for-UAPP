package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqRepresentation
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class BlackPearlKnownLineageTest {
    @Test
    fun exactSavedRepresentationDoesNotCreateModifiedLineage() {
        val saved = savedRepresentation(gainUnits = -512)

        assertThat(buildBlackPearlKnownLineage(7, saved, saved.fingerprint)).isNull()
    }

    @Test
    fun editorProducedNativeDifferenceBuildsExplicitLineage() {
        val saved = savedRepresentation(gainUnits = -512)
        val actual = fingerprint(gainUnits = -640, qUnits = 320)

        val lineage = buildBlackPearlKnownLineage(7, saved, actual)

        assertThat(lineage).isNotNull()
        assertThat(lineage!!.sessionGeneration).isEqualTo(7)
        assertThat(lineage.savedRepresentation.identity.savedEqKey).isEqualTo("saved-1")
        assertThat(lineage.actualFingerprint).isEqualTo(actual)
        assertThat(lineage.differences.map { it.field.name })
            .containsExactly("GAIN_DB", "Q")
            .inOrder()
        assertThat(lineage.differences.first().expectedValue).isEqualTo("-2.00 dB")
        assertThat(lineage.differences.first().actualValue).isEqualTo("-2.50 dB")
    }

    @Test
    fun unknownBecomesModifiedOnlyForExactProvenSessionAndFingerprint() {
        val saved = savedRepresentation(gainUnits = -512)
        val modified = fingerprint(gainUnits = -640)
        val lineage = buildBlackPearlKnownLineage(7, saved, modified)!!
        val base = HardwareEqMatchResolution(
            match = HardwareEqMatch.Unknown,
            savedRepresentations = listOf(saved),
        )

        val result = base.withBlackPearlKnownLineage(7, modified, lineage)

        assertThat(result.match).isInstanceOf(HardwareEqMatch.ModifiedKnown::class.java)
        result.match as HardwareEqMatch.ModifiedKnown
        assertThat(result.match.savedEq).isEqualTo(saved.identity)
    }

    @Test
    fun reconnectBreaksLineageEvenWhenHardwareFingerprintIsIdentical() {
        val saved = savedRepresentation(gainUnits = -512)
        val modified = fingerprint(gainUnits = -640)
        val lineage = buildBlackPearlKnownLineage(7, saved, modified)!!
        val base = HardwareEqMatchResolution(
            match = HardwareEqMatch.Unknown,
            savedRepresentations = listOf(saved),
        )

        val result = base.withBlackPearlKnownLineage(8, modified, lineage)

        assertThat(result.match).isSameInstanceAs(HardwareEqMatch.Unknown)
    }

    @Test
    fun laterOutsideHardwareChangeBreaksLineageBackToUnknown() {
        val saved = savedRepresentation(gainUnits = -512)
        val modified = fingerprint(gainUnits = -640)
        val lineage = buildBlackPearlKnownLineage(7, saved, modified)!!
        val laterOutsideChange = fingerprint(gainUnits = -639)
        val base = HardwareEqMatchResolution(
            match = HardwareEqMatch.Unknown,
            savedRepresentations = listOf(saved),
        )

        val result = base.withBlackPearlKnownLineage(7, laterOutsideChange, lineage)

        assertThat(result.match).isSameInstanceAs(HardwareEqMatch.Unknown)
    }

    @Test
    fun flatOrExactOrdinaryResolutionAlwaysWinsOverLineage() {
        val saved = savedRepresentation(gainUnits = -512)
        val modified = fingerprint(gainUnits = -640)
        val lineage = buildBlackPearlKnownLineage(7, saved, modified)!!

        val exact = HardwareEqMatchResolution(
            match = HardwareEqMatch.Exact(saved.identity),
            savedRepresentations = listOf(saved),
        ).withBlackPearlKnownLineage(7, modified, lineage)
        val flat = HardwareEqMatchResolution(
            match = HardwareEqMatch.Flat,
            savedRepresentations = listOf(saved),
        ).withBlackPearlKnownLineage(7, modified, lineage)

        assertThat(exact.match).isEqualTo(HardwareEqMatch.Exact(saved.identity))
        assertThat(flat.match).isSameInstanceAs(HardwareEqMatch.Flat)
    }

    @Test
    fun deviceWideEqEnableMismatchCannotClaimEditorLineage() {
        val saved = savedRepresentation(gainUnits = -512)
        val actual = fingerprint(gainUnits = -640).copy(eqEnabled = false)

        assertThat(buildBlackPearlKnownLineage(7, saved, actual)).isNull()
    }

    private fun savedRepresentation(gainUnits: Long) = SavedHardwareEqRepresentation(
        identity = SavedHardwareEqIdentity("saved-1", "Edition XS · Crinacle"),
        fingerprint = fingerprint(gainUnits = gainUnits),
        fidelity = DevicePresetFidelity.EXACT,
        adaptationSummary = "source values preserved",
        representationVersion = 1,
    )

    private fun fingerprint(
        gainUnits: Long,
        qUnits: Long = 256,
    ) = HardwareEqNativeFingerprint(
        deviceId = DacDeviceId.TRN_BLACK_PEARL,
        eqEnabled = true,
        bands = listOf(
            HardwareEqNativeBandFingerprint(
                index = 0,
                enabled = true,
                type = EqFilterType.PEAK,
                frequencyUnits = 1000,
                gainUnits = gainUnits,
                qUnits = qUnits,
            ),
        ),
    )
}
