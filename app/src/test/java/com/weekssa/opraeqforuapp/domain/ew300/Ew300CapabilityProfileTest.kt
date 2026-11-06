package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300CapabilityProfileTest {
    @Test
    fun exactQualifiedFingerprintAuthorizesMutation() {
        assertTrue(Ew300CapabilityProfile.authorizesMutation(exactFingerprint()))
    }

    @Test
    fun vidAndPidAloneDoNotAuthorizeMutation() {
        assertFalse(Ew300CapabilityProfile.authorizesMutation("vid=31b2|pid=111|interface=3|serial=unit"))
    }

    @Test
    fun wrongProductOrInterfaceDoesNotAuthorizeMutation() {
        assertFalse(
            Ew300CapabilityProfile.authorizesMutation(
                exactFingerprint().replace("product=SIMGOT EW300 DSP", "product=Other DAC"),
            ),
        )
        assertFalse(
            Ew300CapabilityProfile.authorizesMutation(
                exactFingerprint().replace("interface=3", "interface=2"),
            ),
        )
    }

    @Test
    fun blankSerialDoesNotAuthorizeMutation() {
        assertFalse(
            Ew300CapabilityProfile.authorizesMutation(
                exactFingerprint().replace(Regex("serial=[^|]*"), "serial="),
            ),
        )
    }

    @Test
    fun unknownOrMissingDeviceRevisionDoesNotAuthorizeMutation() {
        assertFalse(
            Ew300CapabilityProfile.authorizesMutation(
                exactFingerprint().replace("deviceRevision=${Ew300CapabilityProfile.DEVICE_REVISION}", "deviceRevision=1.02"),
            ),
        )
        assertFalse(
            Ew300CapabilityProfile.authorizesMutation(
                exactFingerprint().replace("|deviceRevision=${Ew300CapabilityProfile.DEVICE_REVISION}", ""),
            ),
        )
    }

    @Test
    fun productionFlagsKeepParityAndShelfExposureOff() {
        val flags = Ew300CapabilityProfile.PRODUCTION_FLAGS

        assertFalse(flags.parityEnabled)
        assertFalse(flags.shelfQualificationEnabled)
        assertFalse(flags.shelfEditorEnabled)
        assertFalse(Ew300CapabilityProfile.exposesShelfEditor(flags, ownerListeningQualified = true))
    }

    @Test
    fun shelfEditorRequiresParityQualificationAndListeningEvidence() {
        val flags = Ew300CapabilityProfile.ExperimentFlags(
            parityEnabled = true,
            shelfQualificationEnabled = true,
            shelfEditorEnabled = true,
        )

        assertFalse(Ew300CapabilityProfile.exposesShelfEditor(flags, ownerListeningQualified = false))
        assertTrue(Ew300CapabilityProfile.exposesShelfEditor(flags, ownerListeningQualified = true))
        assertFalse(
            Ew300CapabilityProfile.exposesShelfEditor(
                flags.copy(shelfQualificationEnabled = false, shelfEditorEnabled = false),
                ownerListeningQualified = true,
            ),
        )
    }

    private fun exactFingerprint(): String =
        Ew300CapabilityProfile.QUALIFIED_FINGERPRINT
}
