package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandRepresentation
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult

interface Ew300Transport {
    suspend fun readRegister(register: Int): ByteArray?
    suspend fun writeRegister(register: Int, data: ByteArray): Boolean
    suspend fun commit(): Boolean
}

/** Five-band EW300 transaction using the shared finite-target response optimizer. */
class Ew300Flasher(private val transport: Ew300Transport) {
    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        // The 0x66 global-gain field is intentionally not part of the production contract yet.
        // Refuse source/generated preamp rather than silently dropping required headroom.
        if (profile.preampGainDb != null) {
            return Kt02h20FlashResult.NotSuitable(
                "This EW300 adapter currently applies five PEQ bands only; the profile's preamp requires a verified global-gain mapping.",
            )
        }
        val representation = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(result.reason)
            is FiveBandOptimizationResult.Ready -> result.representation
        }
        if (representation.playbackGainDb != 0.0) {
            return Kt02h20FlashResult.NotSuitable(
                "The EW300 response needs ${formatDb(representation.playbackGainDb)} dB of headroom, but its global-gain mapping is not enabled yet.",
            )
        }
        val target = completeBands(representation.bands)
        if (!readAndDecodeCurrent()) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the EW300 PEQ state. Reconnect the DAC and try again.",
            )
        }
        target.forEachIndexed { index, band ->
            val (gain, q) = runCatching { Ew300Protocol.encodeBand(band) }.getOrElse {
                return Kt02h20FlashResult.NotSuitable(it.message ?: "EW300 band is not representable.")
            }
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return Kt02h20FlashResult.TransferFailed(
                    "EW300 stopped accepting PEQ data at band ${index + 1} of ${Ew300Protocol.BAND_COUNT}.",
                )
            }
        }
        if (!transport.commit()) {
            return Kt02h20FlashResult.TransferFailed(
                "EW300 accepted the PEQ writes but did not accept the persistence command.",
            )
        }
        val verificationFailure = verify(target)
        if (verificationFailure != null) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final PEQ readback did not match band ${verificationFailure.band + 1}: " +
                    "expected ${verificationFailure.expected}, read ${verificationFailure.actual}.",
            )
        }
        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult {
        val flat = completeBands(emptyList())
        flat.forEachIndexed { index, band ->
            val (gain, q) = Ew300Protocol.encodeBand(band)
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return Kt02h20FlatResetResult.TransferFailed("EW300 stopped accepting the flat-EQ reset at band ${index + 1}.")
            }
        }
        if (!transport.commit()) {
            return Kt02h20FlatResetResult.TransferFailed("EW300 did not accept the flat-EQ persistence command.")
        }
        val verificationFailure = verify(flat)
        if (verificationFailure != null) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ readback did not match.")
        }
        return Kt02h20FlatResetResult.Success(restoredPlaybackGainDb = 0.0, explicitPersistenceCommandUsed = true)
    }

    private suspend fun readAndDecodeCurrent(): Boolean = (0 until Ew300Protocol.BAND_COUNT).all { index ->
        val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
        val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
        gain?.size == 4 && q?.size == 4
    }

    private suspend fun verify(expected: List<Kt02h20Band>): VerificationFailure? {
        expected.indices.forEach { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
                ?: return VerificationFailure(index, expected[index], null)
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
                ?: return VerificationFailure(index, expected[index], null)
            val actual = Ew300Protocol.decodeBand(index, gain, q)
            if (actual != expected[index]) return VerificationFailure(index, expected[index], actual)
        }
        return null
    }

    private data class VerificationFailure(
        val band: Int,
        val expected: Kt02h20Band,
        val actual: Kt02h20Band?,
    )

    private fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> =
        bands + List(Ew300Protocol.BAND_COUNT - bands.size) { Kt02h20Band("peak_dip", 1000.0, 0.0, 1.0) }

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
}
