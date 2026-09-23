package com.weekssa.opraeqforuapp.domain.ew300

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import org.junit.Test

class Ew300HardwareEqMatchTest {
    @Test
    fun exactSavedProfileMatchesCompleteNativeFiveBandIdentity() {
        val profile = exactFiveBandProfile()
        val nativeBands = profile.bands.orEmpty().map { band ->
            Kt02h20Band(
                type = requireNotNull(band.type),
                frequencyHz = requireNotNull(band.frequency),
                gainDb = requireNotNull(band.gainDb),
                q = requireNotNull(band.q),
            )
        }
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = nativeBands,
                globalGainDb = -4.0,
                sessionGeneration = 7,
                verifiedAtEpochMillis = 99,
            ),
        )
        val saved = SavedEqRecord(
            entryId = "favorite:test",
            kind = SavedEqKind.Favorite,
            sourceProfileId = profile.id,
            productId = profile.productId,
            manufacturer = "Test",
            model = "Headphone",
            displayName = "Exact target",
            profile = profile,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )

        val resolution = resolveEw300HardwareEq(
            bundle = bundle,
            managedHeadphones = emptyList(),
            savedEqs = listOf(saved),
            savedGeneralEqs = emptyList(),
        )

        val match = resolution.match
        assertThat(match).isInstanceOf(HardwareEqMatch.Exact::class.java)
        assertThat((match as HardwareEqMatch.Exact).savedEq.savedEqKey).isEqualTo("canonical:${profile.id}")
        assertThat(resolution.representation(match.savedEq.savedEqKey)?.fidelity)
            .isEqualTo(DevicePresetFidelity.EXACT)
    }

    @Test
    fun flatHardwareAlwaysReportsFlatBeforeSavedMatching() {
        val flatBands = List(Ew300Protocol.BAND_COUNT) {
            Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
        }
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = flatBands,
                globalGainDb = 0.0,
                sessionGeneration = 2,
                verifiedAtEpochMillis = 3,
            ),
        )

        val resolution = resolveEw300HardwareEq(
            bundle = bundle,
            managedHeadphones = emptyList(),
            savedEqs = emptyList(),
            savedGeneralEqs = emptyList(),
        )

        assertThat(resolution.match).isEqualTo(HardwareEqMatch.Flat)
    }

    @Test
    fun shelfSourceDerivesPeakOnlyOptimizedIdentityWithoutMutatingSource() {
        val sourceBands = listOf(
            OpraBand(type = "low_shelf", frequency = 120.0, gainDb = 3.0, q = 0.7),
            OpraBand(type = "high_shelf", frequency = 8_000.0, gainDb = -2.0, q = 0.8),
        )
        val profile = OpraEqProfile(
            id = "shelf-profile",
            productId = "test-product",
            author = "Test",
            details = "Shelf fixture",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -3.0,
            bands = sourceBands,
        )

        val result = Ew300SavedEqRepresentationDeriver.derive(profile)

        assertThat(result).isInstanceOf(Ew300SavedEqRepresentationResult.Ready::class.java)
        val representation = (result as Ew300SavedEqRepresentationResult.Ready).representation
        assertThat(representation.fidelity).isEqualTo(DevicePresetFidelity.OPTIMIZED)
        assertThat(representation.fingerprint.bands).hasSize(Ew300Protocol.BAND_COUNT)
        assertThat(representation.fingerprint.bands.map { it.type }.distinct())
            .containsExactly(EqFilterType.PEAK)
        assertThat(profile.bands).isEqualTo(sourceBands)
    }

    private fun exactFiveBandProfile(): OpraEqProfile = OpraEqProfile(
        id = "exact-profile",
        productId = "test-product",
        author = "Test",
        details = "Exact five-band fixture",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(
            OpraBand(type = "peak_dip", frequency = 80.0, gainDb = 1.0, q = 0.7),
            OpraBand(type = "peak_dip", frequency = 250.0, gainDb = -1.5, q = 1.0),
            OpraBand(type = "peak_dip", frequency = 1_000.0, gainDb = 2.0, q = 1.2),
            OpraBand(type = "peak_dip", frequency = 4_000.0, gainDb = -2.5, q = 0.9),
            OpraBand(type = "peak_dip", frequency = 12_000.0, gainDb = 1.5, q = 0.8),
        ),
    )
}
