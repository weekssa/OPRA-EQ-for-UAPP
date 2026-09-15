package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Assert.assertThrows
import org.junit.Test

class HardwareEqMatchingTest {
    @Test
    fun disabledEqIsFlatEvenWhenStoredBandsAreNonFlat() {
        val actual = fingerprint(
            eqEnabled = false,
            gainUnits = 300,
        )

        assertThat(HardwareEqMatcher.match(actual, emptyList()))
            .isSameInstanceAs(HardwareEqMatch.Flat)
    }

    @Test
    fun zeroGainBandsAreFlatRegardlessOfFrequencyTypeOrQ() {
        val actual = HardwareEqNativeFingerprint(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            eqEnabled = true,
            bands = listOf(
                HardwareEqNativeBandFingerprint(
                    index = 0,
                    enabled = true,
                    type = EqFilterType.LOW_SHELF,
                    frequencyUnits = 75,
                    gainUnits = 0,
                    qUnits = 180,
                ),
                HardwareEqNativeBandFingerprint(
                    index = 1,
                    enabled = true,
                    type = EqFilterType.HIGH_SHELF,
                    frequencyUnits = 11_000,
                    gainUnits = 0,
                    qUnits = 256,
                ),
            ),
        )

        assertThat(actual.isFlatResponse).isTrue()
    }

    @Test
    fun exactNativeFingerprintReturnsSavedIdentity() {
        val actual = fingerprint(gainUnits = -640)
        val savedIdentity = SavedHardwareEqIdentity("saved-1", "Edition XS · Crinacle")

        val result = HardwareEqMatcher.match(
            actual,
            listOf(SavedHardwareEqFingerprint(savedIdentity, actual)),
        )

        assertThat(result).isEqualTo(HardwareEqMatch.Exact(savedIdentity))
    }

    @Test
    fun ordinaryPlaybackVolumeDoesNotParticipateInNativeFingerprint() {
        val native = fingerprint(gainUnits = -640)
        val first = HardwareEqSnapshot(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            sessionGeneration = 1,
            filters = listOf(
                HardwareEqFilter(0, true, EqFilterType.PEAK, 1000.0, -2.5, 1.0),
            ),
            playbackGainDb = -20.0,
            verifiedAtEpochMillis = 1,
        )
        val second = first.copy(playbackGainDb = -10.0)

        assertThat(first.playbackGainDb).isNotEqualTo(second.playbackGainDb)
        assertThat(native).isEqualTo(native.copy())
    }

    @Test
    fun dedicatedEqPreampParticipatesInExactIdentityWhenAdapterSuppliesIt() {
        val actual = fingerprint(gainUnits = -640, dedicatedEqPreampUnits = -12_800)
        val otherwiseSame = actual.copy(dedicatedEqPreampUnits = -10_240)
        val saved = SavedHardwareEqFingerprint(
            SavedHardwareEqIdentity("saved-1", "Edition XS · Creator"),
            otherwiseSame,
        )

        assertThat(HardwareEqMatcher.match(actual, listOf(saved)))
            .isSameInstanceAs(HardwareEqMatch.Unknown)
    }

    @Test
    fun twoSavedEqsWithSameNativeRepresentationRemainAmbiguous() {
        val actual = fingerprint(gainUnits = 250)
        val result = HardwareEqMatcher.match(
            actual,
            listOf(
                SavedHardwareEqFingerprint(
                    SavedHardwareEqIdentity("source-a", "Headphone · Creator A"),
                    actual,
                ),
                SavedHardwareEqFingerprint(
                    SavedHardwareEqIdentity("source-b", "Headphone · Creator B"),
                    actual,
                ),
            ),
        )

        assertThat(result).isInstanceOf(AmbiguousExactHardwareEqMatch::class.java)
        result as AmbiguousExactHardwareEqMatch
        assertThat(result.savedEqs.map { it.savedEqKey })
            .containsExactly("source-a", "source-b")
            .inOrder()
    }

    @Test
    fun duplicateSavedIdentityDoesNotCreateFalseAmbiguity() {
        val actual = fingerprint(gainUnits = 250)
        val identity = SavedHardwareEqIdentity("same", "Same EQ")
        val result = HardwareEqMatcher.match(
            actual,
            listOf(
                SavedHardwareEqFingerprint(identity, actual),
                SavedHardwareEqFingerprint(identity, actual),
            ),
        )

        assertThat(result).isEqualTo(HardwareEqMatch.Exact(identity))
    }

    @Test
    fun similarButDifferentNativeValueRemainsUnknown() {
        val actual = fingerprint(gainUnits = 250)
        val candidate = actual.copy(
            bands = actual.bands.map { band -> band.copy(gainUnits = 249) },
        )

        val result = HardwareEqMatcher.match(
            actual,
            listOf(
                SavedHardwareEqFingerprint(
                    SavedHardwareEqIdentity("nearby", "Nearby curve"),
                    candidate,
                ),
            ),
        )

        assertThat(result).isSameInstanceAs(HardwareEqMatch.Unknown)
    }

    @Test
    fun savedRepresentationForDifferentDeviceCannotMatch() {
        val actual = fingerprint(gainUnits = 250)
        val otherDevice = actual.copy(deviceId = DacDeviceId.FIIO_JA11)

        val result = HardwareEqMatcher.match(
            actual,
            listOf(
                SavedHardwareEqFingerprint(
                    SavedHardwareEqIdentity("ja11", "JA11 representation"),
                    otherDevice,
                ),
            ),
        )

        assertThat(result).isSameInstanceAs(HardwareEqMatch.Unknown)
    }

    @Test
    fun nativeFingerprintRequiresOrderedUniqueBandIndices() {
        val first = band(index = 1, gainUnits = 0)
        val second = band(index = 0, gainUnits = 0)

        assertThrows(IllegalArgumentException::class.java) {
            HardwareEqNativeFingerprint(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                eqEnabled = true,
                bands = listOf(first, second),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            HardwareEqNativeFingerprint(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                eqEnabled = true,
                bands = listOf(second, second.copy(gainUnits = 10)),
            )
        }
    }

    private fun fingerprint(
        eqEnabled: Boolean = true,
        gainUnits: Long,
        dedicatedEqPreampUnits: Long? = null,
    ) = HardwareEqNativeFingerprint(
        deviceId = DacDeviceId.TRN_BLACK_PEARL,
        eqEnabled = eqEnabled,
        bands = listOf(band(index = 0, gainUnits = gainUnits)),
        dedicatedEqPreampUnits = dedicatedEqPreampUnits,
    )

    private fun band(index: Int, gainUnits: Long) = HardwareEqNativeBandFingerprint(
        index = index,
        enabled = true,
        type = EqFilterType.PEAK,
        frequencyUnits = 1000,
        gainUnits = gainUnits,
        qUnits = 256,
    )
}
