package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlin.math.abs
import kotlin.math.round

interface FiioJa11Transport {
    suspend fun readBand(index: Int): FiioJa11Protocol.Band?
    suspend fun readGlobalGainDb(): Double?
    suspend fun sendReport(report: ByteArray): Boolean
}

/** Direct-Flash transaction for the normal FiiO JA11 run-mode PEQ protocol. */
class FiioJa11Flasher(
    private val transport: FiioJa11Transport,
) {
    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        val representation = when (val optimized = Kt02h20FiveBandOptimizer.optimize(profile, Kt02h20DeviceSpecs.FIIO_JA11)) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(optimized.reason)
            is FiveBandOptimizationResult.Ready -> optimized.representation
        }

        // Read before any write. Besides proving the run-mode PEQ interface is responsive, this
        // prevents a wrong interface/firmware combination from becoming a write-only experiment.
        if (transport.readGlobalGainDb() == null || transport.readBand(0) == null) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the FiiO JA11 PEQ state. Reconnect the DAC and try again.",
            )
        }

        val targetBands = FiioJa11Protocol.completeBands(representation.bands)
        targetBands.forEachIndexed { index, band ->
            if (!transport.sendReport(FiioJa11Protocol.writeBandReport(index, band))) {
                return Kt02h20FlashResult.TransferFailed(
                    "FiiO JA11 stopped accepting PEQ data at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}. The preset was not reported as applied.",
                )
            }
        }
        if (!transport.sendReport(FiioJa11Protocol.writeGlobalGainReport(representation.playbackGainDb))) {
            return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 did not accept the required global EQ gain. The preset was not reported as applied.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.applyReport())) {
            return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 did not accept the Apply command. Reconnect the DAC and try again.",
            )
        }

        verifyTarget(targetBands, representation.playbackGainDb)?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(reason)
        }

        if (!transport.sendReport(FiioJa11Protocol.saveToFlashReport())) {
            return Kt02h20FlashResult.TransferFailed(
                "The PEQ was applied to the FiiO JA11, but the device did not accept the persistent Save command.",
            )
        }

        verifyTarget(targetBands, representation.playbackGainDb)?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(
                "FiiO JA11 accepted Save, but the final readback did not match the intended PEQ. $reason",
            )
        }

        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult {
        if (transport.readGlobalGainDb() == null || transport.readBand(0) == null) {
            return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t read the FiiO JA11 PEQ state. Reconnect the DAC and try again.",
            )
        }
        val flatBands = FiioJa11Protocol.completeBands(emptyList())
        flatBands.forEachIndexed { index, band ->
            if (!transport.sendReport(FiioJa11Protocol.writeBandReport(index, band))) {
                return Kt02h20FlatResetResult.TransferFailed(
                    "FiiO JA11 stopped accepting the flat-EQ reset at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}.",
                )
            }
        }
        if (!transport.sendReport(FiioJa11Protocol.writeGlobalGainReport(0.0))) {
            return Kt02h20FlatResetResult.TransferFailed(
                "FiiO JA11 did not accept the 0 dB global EQ gain for Reset.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.applyReport())) {
            return Kt02h20FlatResetResult.TransferFailed("FiiO JA11 did not accept the flat-EQ Apply command.")
        }
        verifyTarget(flatBands, 0.0)?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed(reason)
        }
        if (!transport.sendReport(FiioJa11Protocol.saveToFlashReport())) {
            return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 PEQ is flat in the current session, but the device did not accept the persistent Save command.",
            )
        }
        verifyTarget(flatBands, 0.0)?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed(
                "FiiO JA11 accepted Save, but the final flat-EQ readback did not match. $reason",
            )
        }
        return Kt02h20FlatResetResult.Success(
            restoredPlaybackGainDb = 0.0,
            explicitPersistenceCommandUsed = true,
        )
    }

    private suspend fun verifyTarget(
        expectedBands: List<FiioJa11Protocol.Band>,
        expectedGlobalGainDb: Double,
    ): String? {
        expectedBands.forEachIndexed { index, expected ->
            val actual = transport.readBand(index)
                ?: return "Couldn’t read back JA11 band ${index + 1}."
            if (!FiioJa11Protocol.nearlyMatches(expected, actual)) {
                return "JA11 band ${index + 1} readback did not match the intended value."
            }
        }
        val actualGain = transport.readGlobalGainDb()
            ?: return "Couldn’t read back the JA11 global EQ gain."
        val wireExpected = round(expectedGlobalGainDb * 2560.0) / 2560.0
        if (abs(actualGain - wireExpected) > GLOBAL_GAIN_READBACK_TOLERANCE_DB) {
            return "JA11 global EQ gain readback did not match the intended value."
        }
        return null
    }

    private companion object {
        const val GLOBAL_GAIN_READBACK_TOLERANCE_DB = 0.001
    }
}
