package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Assert.assertThrows
import org.junit.Test

class DacDomainTest {
    @Test
    fun deviceIdentityPreservesMetadataOriginAndUsbIdentity() {
        val identity = blackPearlIdentity()

        assertThat(identity.deviceId).isEqualTo(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(identity.usbVendorId).isEqualTo(0x3302)
        assertThat(identity.usbProductId).isEqualTo(0x43e8)
        assertThat(identity.manufacturer.origin).isEqualTo(DacMetadataOrigin.STATIC_KNOWN)
        assertThat(identity.firmwareVersion?.origin).isEqualTo(DacMetadataOrigin.DEVICE_REPORTED)
    }

    @Test
    fun deviceIdentityRejectsInvalidUsbIds() {
        assertThrows(IllegalArgumentException::class.java) {
            blackPearlIdentity().copy(usbVendorId = 0x1_0000)
        }
    }

    @Test
    fun numericControlSeparatesAbsoluteRangeStepAndNormalRangeCaution() {
        val descriptor = DacControlDescriptor.Numeric(
            id = DacControlId("black_pearl.band_gain"),
            section = DacControlSection.ADVANCED,
            safetyClass = DacControlSafetyClass.NORMAL,
            unit = DacNumericUnit.DB,
            absoluteRange = DacNumericRange(-20.0, 20.0),
            step = 0.5,
            normalRange = DacNumericRange(-10.0, 10.0),
        )

        assertThat(descriptor.validateForWrite(DacControlValue.Numeric(4.5)))
            .isEqualTo(DacControlValidation.Valid)
        assertThat(descriptor.validateForWrite(DacControlValue.Numeric(11.5)))
            .isEqualTo(DacControlValidation.CautionOutsideNormalRange(DacNumericRange(-10.0, 10.0)))
        assertThat(descriptor.validateForWrite(DacControlValue.Numeric(20.5)))
            .isEqualTo(DacControlValidation.OutOfRange(DacNumericRange(-20.0, 20.0)))
        assertThat(descriptor.validateForWrite(DacControlValue.Numeric(4.25)))
            .isEqualTo(DacControlValidation.NotRepresentableAtStep(0.5))
    }

    @Test
    fun numericControlRejectsWrongValueType() {
        val descriptor = DacControlDescriptor.Numeric(
            id = DacControlId("volume"),
            section = DacControlSection.PLAYBACK,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            unit = DacNumericUnit.DB,
            absoluteRange = DacNumericRange(-40.0, 0.0),
            step = 0.5,
        )

        assertThat(descriptor.validateForWrite(DacControlValue.Toggle(true)))
            .isEqualTo(DacControlValidation.WrongValueType)
    }

    @Test
    fun discreteControlRejectsUnknownOption() {
        val descriptor = DacControlDescriptor.Discrete(
            id = DacControlId("dac_filter"),
            section = DacControlSection.DAC_FILTER,
            safetyClass = DacControlSafetyClass.NORMAL,
            options = listOf(
                DacDiscreteOption("fast_ll", "FAST-LL"),
                DacDiscreteOption("fast_pc", "Fast-PC"),
            ),
        )

        assertThat(descriptor.validateForWrite(DacControlValue.Discrete("fast_pc")))
            .isEqualTo(DacControlValidation.Valid)
        assertThat(descriptor.validateForWrite(DacControlValue.Discrete("nos")))
            .isEqualTo(DacControlValidation.UnsupportedOption("nos"))
    }

    @Test
    fun readOnlyControlCannotBeWritten() {
        val descriptor = DacControlDescriptor.ReadOnlyText(DacControlId("firmware"))

        assertThat(descriptor.validateForWrite(DacControlValue.Text("1.2.3")))
            .isEqualTo(DacControlValidation.NotWritable)
    }

    @Test
    fun controlStateRequiresPositiveSessionGeneration() {
        assertThrows(IllegalArgumentException::class.java) {
            DacControlState(
                controlId = DacControlId("volume"),
                value = DacControlValue.Numeric(-12.0),
                freshness = DacStateFreshness.CURRENT,
                sessionGeneration = 0,
                verifiedAtEpochMillis = 1,
            )
        }
    }

    @Test
    fun hardwareEqSnapshotKeepsPlaybackGainDistinctFromDedicatedEqPreamp() {
        val snapshot = HardwareEqSnapshot(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            sessionGeneration = 3,
            activeSlot = 2,
            filters = listOf(
                HardwareEqFilter(
                    index = 0,
                    enabled = true,
                    type = EqFilterType.PEAK,
                    frequencyHz = 1000.0,
                    gainDb = 3.0,
                    q = 1.0,
                ),
            ),
            dedicatedEqPreampDb = null,
            playbackGainDb = -18.5,
            verifiedAtEpochMillis = 42,
        )

        assertThat(snapshot.dedicatedEqPreampDb).isNull()
        assertThat(snapshot.playbackGainDb).isEqualTo(-18.5)
    }

    @Test
    fun hardwareEqSnapshotRejectsDuplicateNativeBandIndices() {
        val band = HardwareEqFilter(
            index = 0,
            enabled = true,
            type = EqFilterType.PEAK,
            frequencyHz = 1000.0,
            gainDb = 0.0,
            q = 1.0,
        )

        assertThrows(IllegalArgumentException::class.java) {
            HardwareEqSnapshot(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                sessionGeneration = 1,
                filters = listOf(band, band.copy(frequencyHz = 2000.0)),
                verifiedAtEpochMillis = 0,
            )
        }
    }

    @Test
    fun modifiedKnownRequiresExplicitDifferences() {
        assertThrows(IllegalArgumentException::class.java) {
            HardwareEqMatch.ModifiedKnown(
                savedEq = SavedHardwareEqIdentity("saved-1", "Edition XS · Crinacle"),
                differences = emptyList(),
            )
        }
    }

    @Test
    fun unknownMatchCarriesNoSavedEqAttribution() {
        val match: HardwareEqMatch = HardwareEqMatch.Unknown

        assertThat(match).isSameInstanceAs(HardwareEqMatch.Unknown)
        assertThat(match).isNotInstanceOf(HardwareEqMatch.Exact::class.java)
        assertThat(match).isNotInstanceOf(HardwareEqMatch.ModifiedKnown::class.java)
    }

    @Test
    fun headroomAssessmentReportsAdditionalAttenuationWithoutChangingPlan() {
        val assessment = DacHeadroomAssessment(
            requiredGainDb = -8.2,
            plannedGainDb = -5.8,
            minimumVerifiedGainDb = -20.0,
            status = DacHeadroomStatus.ADJUSTMENT_REQUIRED,
        )

        assertThat(assessment.additionalAttenuationDb).isWithin(1e-9).of(2.4)
        assertThat(assessment.plannedGainDb).isEqualTo(-5.8)
    }

    @Test
    fun staleBaselineResultKeepsExpectedAndActualSessionGenerations() {
        val result: VerifiedDacWriteResult = VerifiedDacWriteResult.StaleBaseline(
            controlId = DacControlId("volume"),
            expectedSessionGeneration = 4,
            actualSessionGeneration = 5,
        )

        result as VerifiedDacWriteResult.StaleBaseline
        assertThat(result.expectedSessionGeneration).isEqualTo(4)
        assertThat(result.actualSessionGeneration).isEqualTo(5)
    }

    private fun blackPearlIdentity() = DacDeviceIdentity(
        deviceId = DacDeviceId.TRN_BLACK_PEARL,
        manufacturer = DacMetadataValue("TRN", DacMetadataOrigin.STATIC_KNOWN),
        model = DacMetadataValue("Black Pearl", DacMetadataOrigin.STATIC_KNOWN),
        usbVendorId = 0x3302,
        usbProductId = 0x43e8,
        firmwareVersion = DacMetadataValue("1.2.3", DacMetadataOrigin.DEVICE_REPORTED),
        validationStatus = DacValidationStatus.HARDWARE_QUALIFIED,
    )
}
