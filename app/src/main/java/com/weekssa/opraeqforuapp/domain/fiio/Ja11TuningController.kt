package com.weekssa.opraeqforuapp.domain.fiio

/**
 * Applies a tuning request through the same discipline as a physical device transaction:
 * one exclusive operation, a fresh baseline read, a session-generation fence, exactly one write,
 * a readback that must match, and no replay after an uncertain completion.
 *
 * Hardware writes are refused unless the capability model establishes both a read and a write
 * path. Every transport shipped in this build is [Ja11TuningTransport.isSimulated].
 */
class Ja11TuningController(
    private val transport: Ja11TuningTransport,
    private val exclusivity: Ja11TuningExclusivity = MutexJa11TuningExclusivity(),
    private val capability: Ja11TuningCapability = JA11_TESTED_FIRMWARE_2_20_CAPABILITY,
) {
    suspend fun read(): Ja11TuningReadOutcome = exclusivity.withExclusiveOperation { readLocked() }

    suspend fun apply(request: Ja11TuningApplyRequest): Ja11TuningApplyOutcome =
        exclusivity.withExclusiveOperation { applyLocked(request) }

    /**
     * Simulated startup commit. Hardware startup persistence of tuning is not established, so a
     * non-simulated transport reports [Ja11TuningSaveOutcome.Unsupported] rather than guessing.
     */
    suspend fun saveStartupDefaults(): Ja11TuningSaveOutcome =
        exclusivity.withExclusiveOperation {
            val controls = transport as? Ja11TuningPrototypeControls
                ?: return@withExclusiveOperation Ja11TuningSaveOutcome.Unsupported
            if (!controls.saveStartupDefaults()) {
                return@withExclusiveOperation Ja11TuningSaveOutcome.Failed(
                    "No simulated tuning value is available to save.",
                )
            }
            when (val read = transport.read()) {
                is Ja11TuningReadResult.Success -> Ja11TuningSaveOutcome.Saved(read.snapshot)
                Ja11TuningReadResult.PermissionDenied -> Ja11TuningSaveOutcome.PermissionDenied
                is Ja11TuningReadResult.Offline -> Ja11TuningSaveOutcome.Failed(read.reason)
                is Ja11TuningReadResult.Failure -> Ja11TuningSaveOutcome.Failed(read.reason)
            }
        }

    /** Simulates unplug/replug; the next read reports the resulting state. */
    suspend fun simulateReconnect(): Ja11TuningReadOutcome =
        exclusivity.withExclusiveOperation {
            (transport as? Ja11TuningPrototypeControls)?.simulateReconnect()
            readLocked()
        }

    private suspend fun readLocked(): Ja11TuningReadOutcome {
        if (!transport.isSimulated && !capability.runtimeReadEstablished) {
            return Ja11TuningReadOutcome.Unsupported
        }
        return when (val result = transport.read()) {
            is Ja11TuningReadResult.Success -> Ja11TuningReadOutcome.Read(result.snapshot)
            Ja11TuningReadResult.PermissionDenied -> Ja11TuningReadOutcome.PermissionDenied
            is Ja11TuningReadResult.Offline -> Ja11TuningReadOutcome.Disconnected(result.reason)
            is Ja11TuningReadResult.Failure -> Ja11TuningReadOutcome.Failed(result.reason)
        }
    }

    private suspend fun applyLocked(request: Ja11TuningApplyRequest): Ja11TuningApplyOutcome {
        if (!transport.isSimulated && !capability.hardwareWritesAuthorized) {
            return Ja11TuningApplyOutcome.Unsupported
        }

        val baseline = when (val read = readLocked()) {
            is Ja11TuningReadOutcome.Read -> read.snapshot
            Ja11TuningReadOutcome.Unsupported -> return Ja11TuningApplyOutcome.Unsupported
            Ja11TuningReadOutcome.PermissionDenied -> return Ja11TuningApplyOutcome.PermissionDenied
            is Ja11TuningReadOutcome.Disconnected ->
                return Ja11TuningApplyOutcome.Disconnected(read.reason, uncertain = false)
            is Ja11TuningReadOutcome.Failed ->
                return Ja11TuningApplyOutcome.Failed(read.reason, uncertain = false)
        }
        if (baseline.sessionGeneration != request.expectedSessionGeneration) {
            return Ja11TuningApplyOutcome.StaleSession(
                expectedSessionGeneration = request.expectedSessionGeneration,
                actualSessionGeneration = baseline.sessionGeneration,
            )
        }

        // Exactly one write attempt. Any failure below returns; nothing is retried.
        when (val write = transport.write(request.values)) {
            is Ja11TuningWriteResult.Accepted -> Unit
            Ja11TuningWriteResult.PermissionDenied -> return Ja11TuningApplyOutcome.PermissionDenied
            is Ja11TuningWriteResult.DisconnectedBeforeWrite ->
                return Ja11TuningApplyOutcome.Disconnected(write.reason, uncertain = false)
            is Ja11TuningWriteResult.DisconnectedDuringWrite ->
                return Ja11TuningApplyOutcome.Disconnected(write.reason, uncertain = true)
            is Ja11TuningWriteResult.TimedOut ->
                return Ja11TuningApplyOutcome.Failed(write.reason, uncertain = true)
            is Ja11TuningWriteResult.Rejected ->
                return Ja11TuningApplyOutcome.VerificationFailed(write.reason, baseline)
        }

        val readback = when (val read = readLocked()) {
            is Ja11TuningReadOutcome.Read -> read.snapshot
            Ja11TuningReadOutcome.PermissionDenied ->
                return Ja11TuningApplyOutcome.PermissionDenied
            is Ja11TuningReadOutcome.Disconnected ->
                return Ja11TuningApplyOutcome.Disconnected(read.reason, uncertain = true)
            is Ja11TuningReadOutcome.Failed ->
                return Ja11TuningApplyOutcome.VerificationFailed(
                    "The write was sent, but readback was unavailable: "
                        + read.reason,
                    null,
                )
            Ja11TuningReadOutcome.Unsupported ->
                return Ja11TuningApplyOutcome.Unsupported
        }
        if (readback.sessionGeneration != baseline.sessionGeneration) {
            return Ja11TuningApplyOutcome.VerificationFailed(
                "The session was replaced before readback completed. Read again before deciding.",
                readback,
            )
        }
        if (readback.values != request.values) {
            return Ja11TuningApplyOutcome.VerificationFailed(
                "Readback did not match the requested tuning.",
                readback,
            )
        }
        return Ja11TuningApplyOutcome.Verified(readback)
    }

    companion object {
        /**
         * Facts for the tested JA11 firmware 2.20 tuple. This is a device-specific statement, not
         * a claim about silicon, other units, other firmware or future vendor interfaces.
         */
        val JA11_TESTED_FIRMWARE_2_20_CAPABILITY = Ja11TuningCapability(
            runtimeReadEstablished = false,
            runtimeWriteEstablished = false,
            installedTuningReadable = false,
            firmwareOnly = true,
            evidenceNote = "Community DCR/noise-gate changes use KT tools and a firmware image. " +
                "No JA11 run-mode read or write command for these values has been established.",
        )
    }
}
