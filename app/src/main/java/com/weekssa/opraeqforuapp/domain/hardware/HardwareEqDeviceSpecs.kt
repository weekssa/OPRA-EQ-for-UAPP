package com.weekssa.opraeqforuapp.domain.hardware

import com.weekssa.opraeqforuapp.domain.export.DeviceEqCapabilities
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandQuantization

/**
 * Authoritative acoustic/capability profiles for direct-hardware outputs.
 *
 * Protocol transports remain device-specific. The shared response adapter consumes these profiles so
 * Exact/Optimized/Not suitable means the same thing across hardware. Canonical EQ data is never
 * changed to fit a device.
 */
object HardwareEqDeviceSpecs {
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
            // JA11 global EQ gain is encoded more finely by the device codec; leave it unrounded
            // here and let the protocol layer perform the exact wire conversion.
            preampStepDb = null,
        ),
        representationVersion = 3,
    )

    val JCALLY_JM12_STOCK = FiveBandDeviceSpec(
        stableId = "jcally-jm12-stock",
        displayName = "JCALLY JM12",
        capabilities = DeviceEqCapabilities(
            maxBands = 5,
            supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
            minFrequencyHz = 20.0,
            maxFrequencyHz = 20_000.0,
            minGainDb = -30.0,
            maxGainDb = 30.0,
            minQ = 0.1,
            maxQ = 20.0,
            minPreampDb = -24.0,
            maxPreampDb = 12.0,
        ),
        quantization = FiveBandQuantization(
            frequencyStepHz = 1.0,
            gainStepDb = 0.1,
            qStep = 0.001,
            preampStepDb = 0.5,
        ),
        representationVersion = 3,
    )

    val TRN_BLACK_PEARL = FiveBandDeviceSpec(
        stableId = "trn-black-pearl",
        displayName = "TRN Black Pearl",
        capabilities = DeviceEqCapabilities(
            maxBands = 10,
            supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
            minFrequencyHz = 20.0,
            maxFrequencyHz = 20_000.0,
            // The packet field is signed 16-bit in 1/256 dB units. Exact source values outside
            // +/-10 dB remain representable and keep the existing explicit caution path.
            minGainDb = -128.0,
            maxGainDb = 127.99609375,
            minQ = 0.1,
            maxQ = 10.0,
            // The flasher validates the final absolute hardware gain against the currently read
            // device baseline, so there is intentionally no standalone preamp range here.
            minPreampDb = null,
            maxPreampDb = null,
        ),
        quantization = FiveBandQuantization(
            frequencyStepHz = 1.0,
            gainStepDb = 1.0 / 256.0,
            qStep = 1.0 / 256.0,
            preampStepDb = 1.0 / 256.0,
        ),
        // Newly synthesized Black Pearl filters stay inside the currently validated/recommended
        // per-filter gain range. Wider exact source values are preserved, never clamped.
        optimizerMinGainDb = -10.0,
        optimizerMaxGainDb = 10.0,
        representationVersion = 3,
    )
}
