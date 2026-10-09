package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11ReportWriteOutcome
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11SessionToken
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11TransportEvent
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Timing
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.ja11RestartWriteWasAccepted
import com.weekssa.opraeqforuapp.domain.kt02h20.toJa11TraceHex
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

private const val JA11_PERMISSION_PROMPT_MAX_DURATION_MILLIS = 25_000L

class AndroidFiioJa11UsbTransport(
    context: Context,
) : FiioJa11Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = FiioJa11Protocol.VENDOR_ID,
        productIds = FiioJa11Protocol.SUPPORTED_PRODUCT_IDS,
        deviceLabel = "FiiO JA11",
        permissionSuffix = "FIIO_JA11",
        blockRetryWhilePermissionPending = true,
        permissionPromptMaxDurationMillis = JA11_PERMISSION_PROMPT_MAX_DURATION_MILLIS,
        allowConnectionSerialFallback = true,
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    val connectedProductId: Int?
        get() = hid.connectedProductId
    override val deviceFingerprintKey: String?
        get() = hid.deviceFingerprintKey
    val deviceIdentityKey: String?
        get() = fiioJa11PhysicalIdentityKey(hid.deviceFingerprintKey)
    override val usbProductId: Int?
        get() = hid.connectedProductId
    override val sessionGeneration: Long
        get() = hid.sessionGeneration
    override val detachGeneration: Long
        get() = hid.detachGeneration
    override val permissionRequestCount: Long
        get() = hid.permissionRequestCount

    override fun isCurrentSession(expected: FiioJa11SessionToken): Boolean =
        deviceFingerprintKey == expected.deviceFingerprintKey &&
            usbProductId == expected.usbProductId &&
            hid.isCurrentSession(expected.sessionGeneration, expected.detachGeneration)

    private val traceLock = Any()
    private var traceStartMillis: Long? = null
    private val traceEvents = mutableListOf<FiioJa11TransportEvent>()

    override fun beginTrace(operationId: String) {
        synchronized(traceLock) {
            traceStartMillis = System.currentTimeMillis()
            traceEvents.clear()
        }
    }

    override fun endTrace(): List<FiioJa11TransportEvent> =
        synchronized(traceLock) {
            val result = traceEvents.toList()
            traceStartMillis = null
            traceEvents.clear()
            result
        }

    fun connect() = hid.connect()

    fun connectAutomatically() = hid.connectAutomatically()

    fun cancelPendingConnectAttempt(terminalErrorMessage: String? = null) =
        hid.cancelPendingConnectAttempt(terminalErrorMessage)

    suspend fun readOutputVolume(): Int? = exchangeOneByte(
        request = FiioJa11Protocol.readOutputVolumeReport(),
        decoder = FiioJa11Protocol::outputVolumeFromResponse,
    )

    suspend fun readSampleRateLabel(): String? = exchangeOneByte(
        request = FiioJa11Protocol.readSampleRateReport(),
        decoder = FiioJa11Protocol::sampleRateLabelFromResponse,
    )

    override suspend fun readFirmwareVersion(): String? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readFirmwareVersionReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.firmwareVersionFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::firmwareVersionFromResponse,
        )
    }

    override suspend fun readFirmwareVersionInSession(expected: FiioJa11SessionToken): String? =
        exchangeOnStableSession(
            request = FiioJa11Protocol.readFirmwareVersionReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.firmwareVersionFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::firmwareVersionFromResponse,
            expectedSession = expected,
        )

    suspend fun readHeadsetControlEnabled(): Boolean? = exchangeOneByte(
        request = FiioJa11Protocol.readHeadsetControlReport(),
        decoder = FiioJa11Protocol::headsetControlFromResponse,
    )

    override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = exchangeOneByte(
        request = FiioJa11Protocol.readEqProgramReport(),
        decoder = FiioJa11Protocol::eqProgramFromResponse,
    )

    override suspend fun readEqProgramInSession(expected: FiioJa11SessionToken): FiioJa11Protocol.EqProgram? =
        exchangeOneByte(
            request = FiioJa11Protocol.readEqProgramReport(),
            decoder = FiioJa11Protocol::eqProgramFromResponse,
            expectedSession = expected,
        )

    suspend fun readUacMode(): FiioJa11Protocol.UacMode? = exchangeOneByte(
        request = FiioJa11Protocol.readUacModeReport(),
        decoder = FiioJa11Protocol::uacModeFromResponse,
    )

    suspend fun writeOutputVolume(level: Int, expectedSessionGeneration: Long): FiioJa11ReportWriteOutcome =
        sendReportInExpectedSession(
            FiioJa11Protocol.writeOutputVolumeReport(level),
            expectedSessionGeneration,
        )

    suspend fun writeHeadsetControlEnabled(
        enabled: Boolean,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sendRestartControlReport(
            report = FiioJa11Protocol.writeHeadsetControlReport(enabled),
            expectedCommand = 0x12,
            expectedSessionGeneration = expectedSessionGeneration,
        )

    suspend fun writeEqProgram(
        program: FiioJa11Protocol.EqProgram,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sendReportInExpectedSession(
            FiioJa11Protocol.writeEqProgramReport(program),
            expectedSessionGeneration,
        )

    suspend fun writeUacMode(
        mode: FiioJa11Protocol.UacMode,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sendRestartControlReport(
            report = FiioJa11Protocol.writeUacModeReport(mode),
            expectedCommand = 0x20,
            expectedSessionGeneration = expectedSessionGeneration,
        )

    override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readBandReport(index),
            minResponseBytes = 15,
            acceptResponse = { candidate -> FiioJa11Protocol.bandFromResponse(candidate)?.first == index },
            decoder = { candidate ->
                FiioJa11Protocol.bandFromResponse(candidate)?.takeIf { it.first == index }?.second
            },
        )
    }

    override suspend fun readBandInSession(
        index: Int,
        expected: FiioJa11SessionToken,
    ): FiioJa11Protocol.Band? = exchangeOnStableSession(
        request = FiioJa11Protocol.readBandReport(index),
        minResponseBytes = 15,
        acceptResponse = { candidate -> FiioJa11Protocol.bandFromResponse(candidate)?.first == index },
        decoder = { candidate ->
            FiioJa11Protocol.bandFromResponse(candidate)?.takeIf { it.first == index }?.second
        },
        expectedSession = expected,
    )

    override suspend fun readGlobalGainDb(): Double? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readGlobalGainReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.globalGainFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::globalGainFromResponse,
        )
    }

    override suspend fun readGlobalGainDbInSession(expected: FiioJa11SessionToken): Double? =
        exchangeOnStableSession(
            request = FiioJa11Protocol.readGlobalGainReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.globalGainFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::globalGainFromResponse,
            expectedSession = expected,
        )

    override suspend fun saveToFlash(expected: FiioJa11SessionToken): Boolean {
        if (!isCurrentSession(expected)) return false
        val previousGeneration = expected.sessionGeneration
        val previousDetachGeneration = expected.detachGeneration
        if (!ja11RestartWriteWasAccepted(
                sendReportInSession(FiioJa11Protocol.saveToFlashReport(), expected),
            )
        ) return false

        // FiiO documents Save as a chip power-cycle/restart boundary. Final readback must use a
        // fresh session when Android observed detach/attach, while unchanged healthy sessions
        // remain accepted for firmware variants that persist without re-enumerating.
        return hid.awaitOptionalReconnectAfterMutation(
            previousGeneration = previousGeneration,
            previousDetachGeneration = previousDetachGeneration,
        )
    }

    override suspend fun sendReport(report: ByteArray): Boolean {
        val command = report.getOrNull(5)?.toInt()?.and(0xFF)
        val outcome = sendJa11Report(report)
        val accepted = when {
            command == 0x19 -> ja11RestartWriteWasAccepted(outcome)
            else -> outcome == FiioJa11ReportWriteOutcome.COMPLETED
        }
        return accepted
    }

    override suspend fun sendReportInSession(
        report: ByteArray,
        expected: FiioJa11SessionToken,
    ): FiioJa11ReportWriteOutcome = sendJa11Report(report, expected)

    private suspend fun sendRestartControlReport(
        report: ByteArray,
        expectedCommand: Int,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome {
        require(expectedCommand == 0x12 || expectedCommand == 0x20)
        require(report.getOrNull(5)?.toInt()?.and(0xFF) == expectedCommand) {
            "JA11 restart-control command did not match its expected report."
        }
        return sendReportInExpectedSession(report, expectedSessionGeneration)
    }

    /**
     * Carries the repository's baseline generation through to the HID mutex. Capturing a fresh
     * current session here would otherwise allow a detach/reconnect race to redirect a write to
     * the replacement session before the repository can reject its stale baseline.
     */
    private suspend fun sendReportInExpectedSession(
        report: ByteArray,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome {
        val expected = captureCurrentSessionToken(expectedSessionGeneration)
        if (expected == null) {
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "JA11_REPORT_REJECTED_STALE_BASELINE",
                "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
                "bytesExpected" to report.size,
                "expectedSessionGeneration" to expectedSessionGeneration,
                "actualSessionGeneration" to sessionGeneration,
                "actualDetachGeneration" to detachGeneration,
                "actualPid" to usbProductId,
            )
            return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
        }
        return sendReportInSession(report, expected)
    }

    private fun captureCurrentSessionToken(expectedSessionGeneration: Long): FiioJa11SessionToken? {
        if (expectedSessionGeneration <= 0L || hid.sessionGeneration != expectedSessionGeneration) return null
        val fingerprint = deviceFingerprintKey ?: return null
        val productId = usbProductId ?: return null
        return runCatching {
            FiioJa11SessionToken(
                deviceFingerprintKey = fingerprint,
                usbProductId = productId,
                sessionGeneration = expectedSessionGeneration,
                detachGeneration = hid.detachGeneration,
            )
        }.getOrNull()?.takeIf(::isCurrentSession)
    }

    private suspend fun sendJa11Report(
        report: ByteArray,
        expected: FiioJa11SessionToken? = null,
    ): FiioJa11ReportWriteOutcome {
        val generation = expected?.sessionGeneration ?: hid.sessionGeneration
        val detachGeneration = expected?.detachGeneration ?: hid.detachGeneration
        val identityMatches = expected == null ||
            expected.deviceFingerprintKey == deviceFingerprintKey &&
            expected.usbProductId == usbProductId
        val outcome = if (identityMatches) {
            hid.sendJa11Report(
                report = report,
                expectedGeneration = generation,
                expectedDetachGeneration = detachGeneration,
                settleMillis = FiioJa11Timing.settleMillisForMutation(report),
            )
        } else {
            FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
        }
        Ja11DiagnosticLog.eventForDevice(
            "FiiO JA11",
            "JA11_REPORT_OUTCOME",
            "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
            "bytesExpected" to report.size,
            "identityMatches" to identityMatches,
            "expectedPid" to expected?.usbProductId,
            "actualPid" to usbProductId,
            "expectedSessionGeneration" to generation,
            "actualSessionGeneration" to sessionGeneration,
            "expectedDetachGeneration" to detachGeneration,
            "actualDetachGeneration" to this.detachGeneration,
            "outcome" to outcome.name,
        )
        recordTrace(
            direction = "WRITE",
            request = report,
            response = null,
            sessionGeneration = generation,
            detachGeneration = detachGeneration,
            succeeded = ja11RestartWriteWasAccepted(outcome),
        )
        return outcome
    }

    private suspend fun <T> exchangeOneByte(
        request: ByteArray,
        decoder: (ByteArray) -> T?,
        expectedSession: FiioJa11SessionToken? = null,
    ): T? = exchangeOnStableSession(
        request = request,
        minResponseBytes = 7,
        acceptResponse = { candidate -> decoder(candidate) != null },
        decoder = decoder,
        expectedSession = expectedSession,
    )

    private suspend fun <T> exchangeOnStableSession(
        request: ByteArray,
        minResponseBytes: Int,
        acceptResponse: (ByteArray) -> Boolean,
        decoder: (ByteArray) -> T?,
        expectedSession: FiioJa11SessionToken? = null,
    ): T? {
        val generation = expectedSession?.sessionGeneration ?: hid.sessionGeneration
        val detachGeneration = expectedSession?.detachGeneration ?: hid.detachGeneration
        val expectedIsCurrent = expectedSession?.let(::isCurrentSession) ?: true
        if (!expectedIsCurrent || !hid.isCurrentSession(generation, detachGeneration)) {
            recordTrace(
                direction = "READ",
                request = request,
                response = null,
                sessionGeneration = generation,
                detachGeneration = detachGeneration,
                succeeded = false,
            )
            return null
        }
        val response = hid.exchangeJa11(
            report = request,
            expectedGeneration = generation,
            expectedDetachGeneration = detachGeneration,
            minResponseBytes = minResponseBytes,
            acceptResponse = acceptResponse,
        )
        val stable = response != null &&
            hid.isCurrentSession(generation, detachGeneration) &&
            (expectedSession == null || isCurrentSession(expectedSession))
        Ja11DiagnosticLog.eventForDevice(
            "FiiO JA11",
            "JA11_READ_RESULT",
            "command" to request.getOrNull(5)?.toInt()?.and(0xFF),
            "responseBytes" to response?.size,
            "pid" to hid.connectedProductId,
            "sessionGeneration" to generation,
            "detachGeneration" to detachGeneration,
            "stableSession" to stable,
        )
        recordTrace(
            direction = "READ",
            request = request,
            response = response,
            sessionGeneration = generation,
            detachGeneration = detachGeneration,
            succeeded = stable,
        )
        if (!stable) return null
        return decoder(requireNotNull(response))
    }

    private fun recordTrace(
        direction: String,
        request: ByteArray,
        response: ByteArray?,
        sessionGeneration: Long,
        detachGeneration: Long,
        succeeded: Boolean,
    ) {
        synchronized(traceLock) {
            val start = traceStartMillis ?: return
            traceEvents += FiioJa11TransportEvent(
                sequence = traceEvents.size + 1,
                elapsedMillis = (System.currentTimeMillis() - start).coerceAtLeast(0L),
                direction = direction,
                command = request.getOrNull(5)?.let { "0x%02x".format(it.toInt() and 0xFF) } ?: "unknown",
                requestHex = request.toJa11TraceHex(),
                responseHex = response?.toJa11TraceHex(),
                sessionGeneration = sessionGeneration,
                detachGeneration = detachGeneration,
                succeeded = succeeded,
            )
        }
    }

    override fun close() = hid.close()
}
