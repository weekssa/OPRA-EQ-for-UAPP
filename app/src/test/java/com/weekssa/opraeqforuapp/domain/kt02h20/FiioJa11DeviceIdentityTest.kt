package com.weekssa.opraeqforuapp.domain.kt02h20

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FiioJa11DeviceIdentityTest {
    @Test
    fun serialIdentityIgnoresPidAndInterfaceFields() {
        val micOn = "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=3"
        val micOff = "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=2"
        val uac1 = "vid=2972|pid=101|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=3"

        assertThat(fiioJa11SerialIdentity(micOn)).isEqualTo("fixture-only")
        assertThat(fiioJa11SerialIdentity(micOff)).isEqualTo(fiioJa11SerialIdentity(micOn))
        assertThat(fiioJa11SerialIdentity(uac1)).isEqualTo(fiioJa11SerialIdentity(micOn))
    }

    @Test
    fun serialIdentityIsOptionalAndNeverSynthesizedFromNonSerialDescriptorFields() {
        assertThat(fiioJa11SerialIdentity("vid=2972|pid=102|manufacturer=FiiO|product=JA11|interface=3"))
            .isNull()
        assertThat(fiioJa11SerialIdentity("vid=2972|pid=102|serial=   |interface=3")).isNull()
        assertThat(fiioJa11SerialIdentity("vid=2972|pid=102|serial=one|serial=two|interface=3"))
            .isNull()
        assertThat(fiioJa11SerialIdentity(null)).isNull()
    }

    @Test
    fun matchingSerialAddsSameDeviceContinuityAcrossUacPidTransition() {
        val before = "vid=2972|pid=102|serial=fixture-only|interface=3"
        val after = "vid=2972|pid=101|serial=fixture-only|interface=2"

        assertThat(fiioJa11RestartContinuity(before, after, supportedCandidateCount = 1))
            .isEqualTo(FiioJa11RestartContinuity.SAME_DEVICE_SERIAL_MATCHED)
    }

    @Test
    fun serialMismatchFailsAndDuplicateCandidateSerialsCannotDisambiguate() {
        val before = "vid=2972|pid=102|serial=fixture-one|interface=3"
        val after = "vid=2972|pid=101|serial=fixture-two|interface=2"

        assertThat(fiioJa11RestartContinuity(before, after, supportedCandidateCount = 1)).isNull()
        assertThat(fiioJa11RestartContinuity(before, before, supportedCandidateCount = 2)).isNull()
    }

    @Test
    fun seriallessSoleReplacementVerifiesStateWithoutClaimingSamePhysicalUnit() {
        val before = "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=|interface=3"
        val after = "vid=2972|pid=101|manufacturer=FiiO|product=JA11|serial=|interface=2"

        assertThat(fiioJa11RestartContinuity(before, after, supportedCandidateCount = 1))
            .isEqualTo(FiioJa11RestartContinuity.SOLE_RETURNING_JA11_STATE_VERIFIED)
    }

    @Test
    fun oneSerialAvailableAndOneAbsentUsesCardinalityWithoutOverclaiming() {
        val before = "vid=2972|pid=102|serial=fixture-only|interface=3"
        val after = "vid=2972|pid=101|serial=|interface=2"

        assertThat(fiioJa11RestartContinuity(before, after, supportedCandidateCount = 1))
            .isEqualTo(FiioJa11RestartContinuity.SOLE_RETURNING_JA11_STATE_VERIFIED)
    }

    @Test
    fun manualSessionReconnectReportsOnlyPrivateSerialContinuityCategory() {
        assertThat(
            fiioJa11SessionIdentityContinuity(
                hasPreviousSession = false,
                previousSerialIdentity = null,
                currentSerialIdentity = "fixture-only",
            ),
        ).isEqualTo(FiioJa11SessionIdentityContinuity.NO_PREVIOUS_SESSION)
        assertThat(
            fiioJa11SessionIdentityContinuity(
                hasPreviousSession = true,
                previousSerialIdentity = "fixture-only",
                currentSerialIdentity = "fixture-only",
            ),
        ).isEqualTo(FiioJa11SessionIdentityContinuity.SAME_DEVICE_SERIAL_MATCHED)
        assertThat(
            fiioJa11SessionIdentityContinuity(
                hasPreviousSession = true,
                previousSerialIdentity = "fixture-one",
                currentSerialIdentity = "fixture-two",
            ),
        ).isEqualTo(FiioJa11SessionIdentityContinuity.SERIAL_MISMATCH)
        assertThat(
            fiioJa11SessionIdentityContinuity(
                hasPreviousSession = true,
                previousSerialIdentity = "fixture-only",
                currentSerialIdentity = null,
            ),
        ).isEqualTo(FiioJa11SessionIdentityContinuity.SERIAL_UNAVAILABLE)
    }
}
