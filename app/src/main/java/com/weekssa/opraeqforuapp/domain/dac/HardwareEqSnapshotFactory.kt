package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.roundToLong

data class HardwareEqSnapshotBundle(
    val snapshot: HardwareEqSnapshot,
    val fingerprint: HardwareEqNativeFingerprint,
)

/** Pure conversion from complete native readback values to My DAC domain state. */
object HardwareEqSnapshotFactory {
    fun blackPearl(
        nativeBands: List<BlackPearlReadCodec.NativeBand>,
        globalGainRaw: Int,
        sessionGeneration: Long,
        verifiedAtEpochMillis: Long,
    ): HardwareEqSnapshotBundle? {
        if (nativeBands.size != BlackPearlProtocol.BAND_COUNT) return null
        val ordered = nativeBands.sortedBy { it.index }
        if (ordered.map { it.index } != (0 until BlackPearlProtocol.BAND_COUNT).toList()) return null
        val slots = ordered.map { it.activeSlot }.distinct()
        if (slots.size != 1) return null
        if (globalGainRaw !in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW) return null

        val filters = ordered.map { band ->
            HardwareEqFilter(
                index = band.index,
                enabled = true,
                type = band.type,
                frequencyHz = band.frequencyHz,
                gainDb = band.gainDb,
                q = band.q,
            )
        }
        val fingerprint = HardwareEqNativeFingerprint(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            eqEnabled = true,
            bands = ordered.map { band ->
                HardwareEqNativeBandFingerprint(
                    index = band.index,
                    enabled = true,
                    type = band.type,
                    frequencyUnits = band.frequencyRawHz.toLong(),
                    gainUnits = band.gainRaw256.toLong(),
                    qUnits = band.qRaw256.toLong(),
                )
            },
            // Black Pearl 0x03 is ordinary playback/global gain, not automatically source preamp.
            dedicatedEqPreampUnits = null,
        )
        return HardwareEqSnapshotBundle(
            snapshot = HardwareEqSnapshot(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                sessionGeneration = sessionGeneration,
                activeSlot = slots.single(),
                filters = filters,
                dedicatedEqPreampDb = null,
                playbackGainDb = BlackPearlProtocol.rawDeltaToGainDb(globalGainRaw),
                verifiedAtEpochMillis = verifiedAtEpochMillis,
            ),
            fingerprint = fingerprint,
        )
    }

    fun fiioJa11(
        nativeBands: List<FiioJa11Protocol.Band>,
        globalEqGainDb: Double,
        sessionGeneration: Long,
        verifiedAtEpochMillis: Long,
    ): HardwareEqSnapshotBundle? {
        if (nativeBands.size != FiioJa11Protocol.BAND_COUNT) return null
        if (!globalEqGainDb.isFinite() || globalEqGainDb !in FiioJa11Protocol.MIN_GLOBAL_GAIN_DB..FiioJa11Protocol.MAX_GLOBAL_GAIN_DB) {
            return null
        }

        val filters = nativeBands.mapIndexed { index, band -> band.toHardwareFilter(index) ?: return null }
        val fingerprint = HardwareEqNativeFingerprint(
            deviceId = DacDeviceId.FIIO_JA11,
            eqEnabled = true,
            bands = nativeBands.mapIndexed { index, band ->
                HardwareEqNativeBandFingerprint(
                    index = index,
                    enabled = true,
                    type = band.type.toEqFilterType() ?: return null,
                    frequencyUnits = band.frequencyHz.roundToLong(),
                    gainUnits = (band.gainDb * 10.0).roundToLong(),
                    qUnits = (band.q * 100.0).roundToLong(),
                )
            },
            // JA11 command 0x17 is the dedicated global EQ/preamp control used by its PEQ path.
            dedicatedEqPreampUnits = (globalEqGainDb * 2560.0).roundToLong(),
        )
        return HardwareEqSnapshotBundle(
            snapshot = HardwareEqSnapshot(
                deviceId = DacDeviceId.FIIO_JA11,
                sessionGeneration = sessionGeneration,
                filters = filters,
                dedicatedEqPreampDb = globalEqGainDb,
                playbackGainDb = null,
                verifiedAtEpochMillis = verifiedAtEpochMillis,
            ),
            fingerprint = fingerprint,
        )
    }

    fun jcallyJm12Stock(
        nativeBands: List<Kt02h20Band>,
        eqEnabled: Boolean,
        digitalGainSteps: IntArray,
        sessionGeneration: Long,
        verifiedAtEpochMillis: Long,
    ): HardwareEqSnapshotBundle? {
        if (nativeBands.size != JcallyJm12Protocol.BAND_COUNT) return null
        if (digitalGainSteps.isEmpty() || digitalGainSteps.any { it !in -128..127 }) return null

        val filters = nativeBands.mapIndexed { index, band -> band.toHardwareFilter(index) ?: return null }
        val fingerprint = HardwareEqNativeFingerprint(
            deviceId = DacDeviceId.JCALLY_JM12_STOCK,
            eqEnabled = eqEnabled,
            bands = nativeBands.mapIndexed { index, band ->
                HardwareEqNativeBandFingerprint(
                    index = index,
                    enabled = true,
                    type = band.type.toEqFilterType() ?: return null,
                    frequencyUnits = band.frequencyHz.roundToLong(),
                    gainUnits = (band.gainDb * 10.0).roundToLong(),
                    qUnits = (band.q * 1000.0).roundToLong(),
                )
            },
            // 0x66 is playback/DAC digital gain; it is deliberately not part of EQ identity.
            dedicatedEqPreampUnits = null,
        )
        val commonPlaybackGainDb = digitalGainSteps
            .takeIf { steps -> steps.all { it == steps.first() } }
            ?.first()
            ?.let(JcallyJm12Protocol::gainStepsToDb)
        return HardwareEqSnapshotBundle(
            snapshot = HardwareEqSnapshot(
                deviceId = DacDeviceId.JCALLY_JM12_STOCK,
                sessionGeneration = sessionGeneration,
                filters = filters,
                dedicatedEqPreampDb = null,
                playbackGainDb = commonPlaybackGainDb,
                verifiedAtEpochMillis = verifiedAtEpochMillis,
            ),
            fingerprint = fingerprint,
        )
    }

    private fun FiioJa11Protocol.Band.toHardwareFilter(index: Int): HardwareEqFilter? =
        HardwareEqFilter(
            index = index,
            enabled = true,
            type = type.toEqFilterType() ?: return null,
            frequencyHz = frequencyHz,
            gainDb = gainDb,
            q = q,
        )

    private fun Kt02h20Band.toHardwareFilter(index: Int): HardwareEqFilter? =
        HardwareEqFilter(
            index = index,
            enabled = true,
            type = type.toEqFilterType() ?: return null,
            frequencyHz = frequencyHz,
            gainDb = gainDb,
            q = q,
        )

    private fun String.toEqFilterType(): EqFilterType? = when (this) {
        "peak_dip" -> EqFilterType.PEAK
        "low_shelf" -> EqFilterType.LOW_SHELF
        "high_shelf" -> EqFilterType.HIGH_SHELF
        else -> null
    }
}
