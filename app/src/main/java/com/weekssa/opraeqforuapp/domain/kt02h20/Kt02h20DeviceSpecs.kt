package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.export.DeviceEqCapabilities

/**
 * Software capability profiles for the two v0.5 hardware targets.
 *
 * These are intentionally separate even where values overlap. Each remains hardware-validation
 * pending until its own Pixel 9/device checklist passes.
 */
object Kt02h20DeviceSpecs {
    val FIIO_JA11 = FiveBandDeviceSpec(
        stableId = "fiio-ja11",
        displayName = "FiiO JA11",
        capabilities = DeviceEqCapabilities(
            maxBands = 5,
            supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
            minFrequencyHz = 20.0,
            maxFrequencyHz = 20_000.0,
            minGainDb = -24.0,
            maxGainDb = 12.0,
            minQ = 0.1,
            maxQ = 10.0,
            minPreampDb = -12.0,
            maxPreampDb = 12.0,
        ),
        quantization = FiveBandQuantization(
            frequencyStepHz = 1.0,
            gainStepDb = 0.1,
            qStep = 0.01,
            // The observed protocol stores global gain in 1/2560 dB units. Keep the source value
            // as Double here and let the JA11 codec perform the exact wire quantization.
            preampStepDb = null,
        ),
    )

    val JCALLY_JM12_STOCK = FiveBandDeviceSpec(
        stableId = "jcally-jm12-stock",
        displayName = "JCALLY JM12",
        capabilities = DeviceEqCapabilities(
            maxBands = 5,
            supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
            // The stock KT02H20 register/UI evidence accepts a wider range, but v0.5 keeps the
            // fitting grid inside the normal audible PEQ range until physical qualification.
            minFrequencyHz = 20.0,
            maxFrequencyHz = 20_000.0,
            minGainDb = -30.0,
            maxGainDb = 30.0,
            minQ = 0.1,
            maxQ = 20.0,
            // Stock firmware exposes playback digital gain rather than the JA11's dedicated global
            // PEQ-gain command. The flasher applies this as a tracked relative delta in 0.5 dB steps.
            minPreampDb = -24.0,
            maxPreampDb = 12.0,
        ),
        quantization = FiveBandQuantization(
            frequencyStepHz = 1.0,
            gainStepDb = 0.1,
            qStep = 0.001,
            preampStepDb = 0.5,
        ),
    )
}
