package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatcher
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class BlackPearlSavedEqRepresentationTest {
    @Test
    fun derivesExactNativeWireIdentityIncludingRoundingAndFlatPadding() {
        val result = BlackPearlSavedEqRepresentationDeriver.derive(
            profile(
                preampDb = -3.0,
                bands = listOf(
                    OpraBand("peak_dip", 1000.4, 1.234, 0.777, null),
                    OpraBand("low_shelf", 120.0, -2.0, 0.7, null),
                ),
            ),
        )

        assertThat(result).isInstanceOf(BlackPearlSavedEqRepresentationResult.Ready::class.java)
        val representation = (result as BlackPearlSavedEqRepresentationResult.Ready).representation
        assertThat(representation.fidelity).isEqualTo(DevicePresetFidelity.OPTIMIZED)
        assertThat(representation.adaptationSummary).contains("native hardware rounding")
        assertThat(representation.requiredPlaybackGainDb).isEqualTo(-3.0)
        assertThat(representation.fingerprint.dedicatedEqPreampUnits).isNull()
        assertThat(representation.fingerprint.bands).hasSize(10)

        val first = representation.fingerprint.bands[0]
        assertThat(first.index).isEqualTo(0)
        assertThat(first.type).isEqualTo(EqFilterType.PEAK)
        assertThat(first.frequencyUnits).isEqualTo(1000L)
        assertThat(first.gainUnits).isEqualTo(316L)
        assertThat(first.qUnits).isEqualTo(199L)

        val second = representation.fingerprint.bands[1]
        assertThat(second.type).isEqualTo(EqFilterType.LOW_SHELF)
        assertThat(second.frequencyUnits).isEqualTo(120L)
        assertThat(second.gainUnits).isEqualTo(-512L)
        assertThat(second.qUnits).isEqualTo(179L)

        // Flash pads every unused native slot to the same deterministic flat filters.
        val padding = representation.fingerprint.bands.drop(2)
        assertThat(padding.map { it.frequencyUnits })
            .containsExactly(125L, 250L, 500L, 1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
            .inOrder()
        assertThat(padding.all { it.type == EqFilterType.PEAK && it.gainUnits == 0L && it.qUnits == 256L })
            .isTrue()
    }

    @Test
    fun sameNativeFiltersWithDifferentSourcePreampRemainAmbiguous() {
        val bands = listOf(
            OpraBand("peak_dip", 1000.0, 2.0, 1.0, null),
        )
        val first = ready(profile(id = "eq-a", preampDb = -3.0, bands = bands))
        val second = ready(profile(id = "eq-b", preampDb = -6.0, bands = bands))

        assertThat(first.fingerprint).isEqualTo(second.fingerprint)
        assertThat(first.requiredPlaybackGainDb).isEqualTo(-3.0)
        assertThat(second.requiredPlaybackGainDb).isEqualTo(-6.0)

        val match = HardwareEqMatcher.match(
            actual = first.fingerprint,
            saved = listOf(
                first.asSavedFingerprint(SavedHardwareEqIdentity("eq-a", "Headphone · EQ A")),
                second.asSavedFingerprint(SavedHardwareEqIdentity("eq-b", "Headphone · EQ B")),
            ),
        )

        assertThat(match).isInstanceOf(AmbiguousExactHardwareEqMatch::class.java)
        match as AmbiguousExactHardwareEqMatch
        assertThat(match.savedEqs.map { it.savedEqKey }).containsExactly("eq-a", "eq-b").inOrder()
    }

    @Test
    fun unsupportedSavedProfileCannotEnterNativeMatchSet() {
        val result = BlackPearlSavedEqRepresentationDeriver.derive(
            profile(
                bands = listOf(OpraBand("band_pass", 1000.0, 1.0, 1.0, null)),
            ),
        )

        assertThat(result).isInstanceOf(BlackPearlSavedEqRepresentationResult.NotRepresentable::class.java)
    }

    @Test
    fun writeReportDecoderUsesSameNativePayloadContractAsReader() {
        val report = BlackPearlProtocol.writeBandReport(
            index = 3,
            band = BlackPearlProtocol.Band(
                type = "high_shelf",
                frequencyHz = 12_345.0,
                gainDb = -11.9,
                q = 1.25,
            ),
            activeSlot = 4,
        )

        val native = BlackPearlReadCodec.bandFromWriteReport(report)

        assertThat(native).isNotNull()
        assertThat(native!!.index).isEqualTo(3)
        assertThat(native.type).isEqualTo(EqFilterType.HIGH_SHELF)
        assertThat(native.frequencyRawHz).isEqualTo(12_345)
        assertThat(native.gainRaw256).isEqualTo((-11.9 * 256.0).toInt())
        assertThat(native.qRaw256).isEqualTo(320)
        assertThat(native.activeSlot).isEqualTo(4)
        assertThat(BlackPearlReadCodec.bandFromResponse(report)).isNull()
    }

    private fun ready(profile: OpraEqProfile): BlackPearlSavedEqRepresentation =
        (BlackPearlSavedEqRepresentationDeriver.derive(profile) as BlackPearlSavedEqRepresentationResult.Ready)
            .representation

    private fun profile(
        id: String = "saved-eq",
        preampDb: Double? = -3.0,
        bands: List<OpraBand>,
    ) = OpraEqProfile(
        id = id,
        productId = "headphone",
        author = "Creator",
        details = "Test",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preampDb,
        bands = bands,
    )
}
