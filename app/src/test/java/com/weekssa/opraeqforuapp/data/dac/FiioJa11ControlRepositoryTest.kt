package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Test

class FiioJa11ControlRepositoryTest {
    @Test
    fun completeReadReturnsOneCurrentSnapshot() = runBlocking {
        val source = FakeSource()
        val result = FiioJa11ControlRepository(source).readSnapshot()

        assertThat(result).isInstanceOf(FiioJa11ControlReadResult.Success::class.java)
        val snapshot = (result as FiioJa11ControlReadResult.Success).snapshot
        assertThat(snapshot.sessionGeneration).isEqualTo(7L)
        assertThat(snapshot.firmwareVersion).isEqualTo("2.20")
        assertThat(snapshot.outputVolume).isEqualTo(59)
        assertThat(snapshot.eqProgram).isEqualTo(FiioJa11Protocol.EqProgram.USER_1)
        assertThat(snapshot.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
    }

    @Test
    fun outputVolumeUsesFreshBaselineTargetedWriteAndVerifiedReadback() = runBlocking {
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)

        val result = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.outputVolume).isEqualTo(58)
        assertThat(source.eqProgram).isEqualTo(FiioJa11Protocol.EqProgram.USER_1)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
    }

    @Test
    fun staleExpectedGenerationCannotWrite() = runBlocking {
        val source = FakeSource()
        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 6L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
        assertThat(source.volumeWrites).isEmpty()
    }

    @Test
    fun sameSessionReadbackMismatchNeverReportsSuccess() = runBlocking {
        val source = FakeSource(ignoreVolumeWrite = true)
        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.ReadbackMismatch::class.java)
    }

    @Test
    fun headsetWriteRequiresAReplacementSessionBeforeVerification() = runBlocking {
        val source = FakeSource(replacementReady = false)
        val repository = FiioJa11ControlRepository(source)

        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(first).isInstanceOf(FiioJa11ControlWriteResult.ReconnectRequired::class.java)
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending
        assertThat(source.headsetControlEnabled).isFalse()

        val sameSession = repository.verifyRestartedControl(pending)
        assertThat(sameSession).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.replacementReady = true
        val verified = repository.verifyRestartedControl(pending)
        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun uacWriteIsVerifiedOnlyAfterNewPidAndNewSessionAgree() = runBlocking {
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.UAC_MODE,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Discrete("uac_1"),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.productId = FiioJa11Protocol.PRODUCT_ID_UAC_1
        val verified = repository.verifyRestartedControl(pending)

        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_1)
    }

    @Test
    fun seriallessReplacementWithExpectedDetachIsAccepted() = runBlocking {
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.connectedDeviceSerial = null
        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun matchingOptionalSerialReplacementIsAccepted() = runBlocking {
        val source = FakeSource().apply { connectedDeviceSerial = "JA11-A" }
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun mismatchingAvailableSerialFailsClosedAfterExpectedDetach() = runBlocking {
        val source = FakeSource().apply { connectedDeviceSerial = "JA11-A" }
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.connectedDeviceSerial = "JA11-B"
        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
    }

    @Test
    fun multipleSupportedCandidatesBlockExpectedRestartReadback() = runBlocking {
        val source = FakeSource(candidateCount = 2)
        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.NotConnected::class.java)
        assertThat(source.headsetControlWrites).isEmpty()
    }

    @Test
    fun delayedPermissionReplacementIsWaitedForOnceWithoutReplayingWrite() = runBlocking {
        val source = FakeSource(permissionDelayMillis = 20L)
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun replacementTimeoutDoesNotVerifyOrReplayTheExpectedResetWrite() = runBlocking {
        val source = FakeSource(replacementReady = false)
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
        assertThat(source.headsetControlWrites).containsExactly(false)
        Unit
    }

    @Test
    fun cancelledReplacementWaitDoesNotReplayTheExpectedResetWrite() = runBlocking {
        val source = FakeSource(permissionDelayMillis = 500L)
        val repository = FiioJa11ControlRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        try {
            withTimeout(10L) { repository.verifyRestartedControl(pending) }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            // The caller cancelled while Android permission/reconnect was still pending.
        }

        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun readFailsClosedWhenReportedUacModeDisagreesWithUsbIdentity() = runBlocking {
        val source = FakeSource().apply {
            productId = FiioJa11Protocol.PRODUCT_ID_UAC_1
            uacMode = FiioJa11Protocol.UacMode.UAC_2
        }

        val result = FiioJa11ControlRepository(source).readSnapshot()

        assertThat(result).isEqualTo(FiioJa11ControlReadResult.ReadFailed("UAC mode / USB identity"))
    }

    private class FakeSource(
        private val ignoreVolumeWrite: Boolean = false,
        private val candidateCount: Int = 1,
        private val permissionDelayMillis: Long = 0L,
        var replacementReady: Boolean = true,
    ) : FiioJa11DeviceControlSource {
        override var sessionGeneration: Long = 7L
        override var detachGeneration: Long = 0L
        override var connectedDeviceSerial: String? = null
        override val supportedCandidateCount: Int = candidateCount
        var productId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2
        var current = true
        var outputVolume = 59
        var sampleRate = "352.8 kHz"
        var firmware = "2.20"
        var headsetControlEnabled = true
        var eqProgram = FiioJa11Protocol.EqProgram.USER_1
        var uacMode = FiioJa11Protocol.UacMode.UAC_2
        val volumeWrites = mutableListOf<Int>()
        val headsetControlWrites = mutableListOf<Boolean>()
        var replacementWaits = 0

        override val connectedProductId: Int?
            get() = productId

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && candidateCount == 1 && sessionGeneration == this.sessionGeneration

        override suspend fun awaitExpectedReplacementSession(
            previousGeneration: Long,
            previousDetachGeneration: Long,
            previousSerial: String?,
            timeoutMillis: Long,
        ): Boolean {
            replacementWaits += 1
            if (!replacementReady) return false
            if (permissionDelayMillis > 0L) delay(permissionDelayMillis)
            if (sessionGeneration == previousGeneration) sessionGeneration += 1L
            if (detachGeneration == previousDetachGeneration) detachGeneration += 1L
            val expectedSerial = previousSerial?.trim()?.takeIf(String::isNotEmpty)
            val replacementSerial = connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
            return candidateCount == 1 &&
                (expectedSerial == null || replacementSerial == null || expectedSerial == replacementSerial)
        }

        override suspend fun readOutputVolume(): Int? = outputVolume
        override suspend fun readSampleRateLabel(): String? = sampleRate
        override suspend fun readFirmwareVersion(): String? = firmware
        override suspend fun readHeadsetControlEnabled(): Boolean? = headsetControlEnabled
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = eqProgram
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = uacMode

        override suspend fun writeOutputVolume(level: Int): Boolean {
            volumeWrites += level
            if (!ignoreVolumeWrite) outputVolume = level
            return true
        }

        override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean {
            headsetControlWrites += enabled
            headsetControlEnabled = enabled
            return true
        }

        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean {
            eqProgram = program
            return true
        }

        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean {
            uacMode = mode
            return true
        }
    }
}
