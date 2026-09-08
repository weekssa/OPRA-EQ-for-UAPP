package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

interface JcallyJm12Transport {
    suspend fun handshake(): Boolean
    suspend fun readRegister(address: Int): Int?
    suspend fun writeRegister(address: Int, value: Int): Boolean
}

interface JcallyJm12GainStateStore {
    fun readAppliedGainDeltaSteps(): Int
    fun writeAppliedGainDeltaSteps(steps: Int)
}

/**
 * Direct hardware PEQ transaction for a stock-firmware JCALLY JM12.
 *
 * Stock run-mode register writes are live and no separate EQ-persistence command has yet been
 * corroborated. The transaction therefore disables DAC EQ before changing filters, verifies every
 * register write, establishes the required relative digital-gain delta, then re-enables the complete
 * five-band set. `explicitPersistenceCommandUsed` remains false until physical qualification proves
 * a distinct safe persistence operation (or proves run-mode register writes persist automatically).
 */
class JcallyJm12Flasher(
    private val transport: JcallyJm12Transport,
    private val gainStateStore: JcallyJm12GainStateStore,
) {
    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        val representation = when (
            val optimized = Kt02h20FiveBandOptimizer.optimize(profile, Kt02h20DeviceSpecs.JCALLY_JM12_STOCK)
        ) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(optimized.reason)
            is FiveBandOptimizationResult.Ready -> optimized.representation
        }

        val initial = readBaselineState()
            ?: return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t verify the stock JCALLY JM12 DSP state. Reconnect the DAC and try again.",
            )
        val requestedDeltaSteps = JcallyJm12Protocol.gainDbToSteps(representation.playbackGainDb)
        val baselineSteps = initial.currentGainSteps.map { it - initial.previousEqDeltaSteps }
        val targetSteps = baselineSteps.map { it + requestedDeltaSteps }
        if (targetSteps.any { it !in -128..127 }) {
            return Kt02h20FlashResult.NotSuitable(
                "Applying the required ${formatDb(representation.playbackGainDb)} dB playback-gain adjustment would exceed the stock JM12 digital-gain register range. Adjust device volume and try again.",
            )
        }

        if (!setEqEnabled(initial.eqEnableRegister, enabled = false)) {
            return Kt02h20FlashResult.TransferFailed(
                "JCALLY JM12 could not be placed in the safe EQ-bypassed state before writing the preset.",
            )
        }

        val targetBands = JcallyJm12Protocol.completeBands(representation.bands)
        targetBands.forEachIndexed { index, band ->
            val registers = JcallyJm12Protocol.encodeBand(band)
            val address = JcallyJm12Protocol.bandRegisterAddress(index)
            if (!writeAndVerify(address, registers.a) || !writeAndVerify(address + 1, registers.b)) {
                return Kt02h20FlashResult.TransferFailed(
                    "JCALLY JM12 stopped accepting PEQ data at band ${index + 1} of ${JcallyJm12Protocol.BAND_COUNT}. DAC EQ was left bypassed so a partial filter set is not active; reconnect and retry.",
                )
            }
        }

        val targetGainRegister = JcallyJm12Protocol.withDigitalGainSteps(
            initial.digitalGainRegister,
            initial.protocolFlags,
            targetSteps.toIntArray(),
        )
        if (targetGainRegister != initial.digitalGainRegister &&
            !writeAndVerify(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN, targetGainRegister)
        ) {
            return Kt02h20FlashResult.TransferFailed(
                "JCALLY JM12 did not accept the required playback-gain adjustment. DAC EQ was left bypassed; reconnect and retry.",
            )
        }
        // Once hardware gain is verified, track the applied delta immediately. If the final enable
        // fails, a retry must replace this delta rather than stack it.
        gainStateStore.writeAppliedGainDeltaSteps(requestedDeltaSteps)

        verifyBands(targetBands)?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(
                "$reason DAC EQ remains bypassed; reconnect and retry.",
            )
        }
        if (!verifyDigitalGain(initial.protocolFlags, targetSteps.toIntArray())) {
            return Kt02h20FlashResult.VerificationFailed(
                "JCALLY JM12 playback-gain readback did not match the intended value. DAC EQ remains bypassed; reconnect and retry.",
            )
        }

        if (!setEqEnabled(initial.eqEnableRegister, enabled = true)) {
            return Kt02h20FlashResult.TransferFailed(
                "JCALLY JM12 received and verified the preset but DAC EQ could not be re-enabled. The gain state is retained for a safe retry.",
            )
        }

        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = false,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult {
        val initial = readBaselineState()
            ?: return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t verify the stock JCALLY JM12 DSP state. Reconnect the DAC and try again.",
            )
        val baselineSteps = initial.currentGainSteps.map { it - initial.previousEqDeltaSteps }
        if (baselineSteps.any { it !in -128..127 }) {
            return Kt02h20FlatResetResult.NotSuitable(
                "Restoring the playback gain that existed before EQ Library's adjustment would exceed the stock JM12 digital-gain register range. Adjust device volume and try again.",
            )
        }
        if (!setEqEnabled(initial.eqEnableRegister, enabled = false)) {
            return Kt02h20FlatResetResult.TransferFailed(
                "JCALLY JM12 could not be placed in the safe EQ-bypassed state before Reset.",
            )
        }

        val flatBands = JcallyJm12Protocol.completeBands(emptyList())
        flatBands.forEachIndexed { index, band ->
            val registers = JcallyJm12Protocol.encodeBand(band)
            val address = JcallyJm12Protocol.bandRegisterAddress(index)
            if (!writeAndVerify(address, registers.a) || !writeAndVerify(address + 1, registers.b)) {
                return Kt02h20FlatResetResult.TransferFailed(
                    "JCALLY JM12 stopped accepting the flat-EQ reset at band ${index + 1}. DAC EQ remains bypassed and playback gain was not restored.",
                )
            }
        }

        val baselineGainRegister = JcallyJm12Protocol.withDigitalGainSteps(
            initial.digitalGainRegister,
            initial.protocolFlags,
            baselineSteps.toIntArray(),
        )
        if (baselineGainRegister != initial.digitalGainRegister &&
            !writeAndVerify(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN, baselineGainRegister)
        ) {
            return Kt02h20FlatResetResult.TransferFailed(
                "JCALLY JM12 PEQ bands are flat and bypassed, but the prior EQ Library playback-gain adjustment could not be removed. The tracked delta is retained so Reset can be retried safely.",
            )
        }
        if (!verifyDigitalGain(initial.protocolFlags, baselineSteps.toIntArray())) {
            return Kt02h20FlatResetResult.VerificationFailed(
                "JCALLY JM12 PEQ bands are flat and bypassed, but playback-gain readback did not match the restored baseline. The tracked delta is retained for retry.",
            )
        }
        // Gain is now at baseline. Clear before the final harmless flat-EQ enable so an enable failure
        // cannot leave stale tracking that would be subtracted again on retry.
        gainStateStore.writeAppliedGainDeltaSteps(0)

        verifyBands(flatBands)?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed("$reason DAC EQ remains bypassed.")
        }
        if (!setEqEnabled(initial.eqEnableRegister, enabled = true)) {
            return Kt02h20FlatResetResult.TransferFailed(
                "JCALLY JM12 is flat with playback gain restored, but the flat DAC EQ block could not be re-enabled. No EQ Library gain delta remains tracked.",
            )
        }

        val restoredSteps = baselineSteps.firstOrNull()?.minus(initial.currentGainSteps.firstOrNull() ?: 0) ?: 0
        return Kt02h20FlatResetResult.Success(
            restoredPlaybackGainDb = JcallyJm12Protocol.gainStepsToDb(restoredSteps),
            explicitPersistenceCommandUsed = false,
        )
    }

    private suspend fun readBaselineState(): BaselineState? {
        if (!transport.handshake()) return null
        val flags = transport.readRegister(JcallyJm12Protocol.REG_PROTOCOL_FLAGS) ?: return null
        val eqEnable = transport.readRegister(JcallyJm12Protocol.REG_EQ_DAC_ENABLE) ?: return null
        val gainRegister = transport.readRegister(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN) ?: return null
        val currentSteps = JcallyJm12Protocol.decodeDigitalGainSteps(gainRegister, flags)
        // Require one complete band read before any write so an unrelated 31B2:0111 interface is not
        // accepted merely because a handshake happened to respond.
        val a = transport.readRegister(JcallyJm12Protocol.bandRegisterAddress(0)) ?: return null
        val b = transport.readRegister(JcallyJm12Protocol.bandRegisterAddress(0) + 1) ?: return null
        if (JcallyJm12Protocol.decodeBand(a, b) == null) return null
        return BaselineState(
            protocolFlags = flags,
            eqEnableRegister = eqEnable,
            digitalGainRegister = gainRegister,
            currentGainSteps = currentSteps.toList(),
            previousEqDeltaSteps = gainStateStore.readAppliedGainDeltaSteps(),
        )
    }

    private suspend fun setEqEnabled(originalRegister: Int, enabled: Boolean): Boolean {
        val value = JcallyJm12Protocol.setEqEnabled(originalRegister, enabled)
        if (!writeAndVerify(JcallyJm12Protocol.REG_EQ_DAC_ENABLE, value)) return false
        val readback = transport.readRegister(JcallyJm12Protocol.REG_EQ_DAC_ENABLE) ?: return false
        return JcallyJm12Protocol.isEqEnabled(readback) == enabled
    }

    private suspend fun verifyBands(expected: List<Kt02h20Band>): String? {
        expected.forEachIndexed { index, band ->
            val address = JcallyJm12Protocol.bandRegisterAddress(index)
            val a = transport.readRegister(address) ?: return "Couldn’t read back JM12 band ${index + 1}."
            val b = transport.readRegister(address + 1) ?: return "Couldn’t read back JM12 band ${index + 1}."
            val actual = JcallyJm12Protocol.decodeBand(a, b)
                ?: return "JM12 band ${index + 1} readback could not be decoded."
            if (!JcallyJm12Protocol.nearlyMatches(band, actual)) {
                return "JM12 band ${index + 1} readback did not match the intended value."
            }
        }
        return null
    }

    private suspend fun verifyDigitalGain(protocolFlags: Int, targetSteps: IntArray): Boolean {
        val value = transport.readRegister(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN) ?: return false
        return JcallyJm12Protocol.decodeDigitalGainSteps(value, protocolFlags).contentEquals(targetSteps)
    }

    private suspend fun writeAndVerify(address: Int, value: Int): Boolean {
        if (!transport.writeRegister(address, value)) return false
        return transport.readRegister(address) == value
    }

    private data class BaselineState(
        val protocolFlags: Int,
        val eqEnableRegister: Int,
        val digitalGainRegister: Int,
        val currentGainSteps: List<Int>,
        val previousEqDeltaSteps: Int,
    )

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
}
