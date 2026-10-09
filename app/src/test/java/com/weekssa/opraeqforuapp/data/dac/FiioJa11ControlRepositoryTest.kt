package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.kt02h20.fiioJa11PhysicalIdentityKey
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11ReportWriteOutcome
import kotlinx.coroutines.runBlocking
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
    fun sameSessionVolumeWriteDoesNotRequireRestartIdentity() = runBlocking {
        val source = FakeSource().apply { missingIdentity = true }

        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.outputVolume).isEqualTo(58)
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
    fun sessionChangeAfterRepositoryPrecheckCannotRedirectDeviceWriteToReplacement() = runBlocking {
        val source = FakeSource(changeSessionBeforeNextWrite = true)
        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isEqualTo(
            FiioJa11ControlWriteResult.StaleBaseline(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                actualSessionGeneration = 8L,
            ),
        )
        assertThat(source.volumeWrites).isEmpty()
        Unit
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
        val source = FakeSource()
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
        val verified = repository.verifyRestartedControl(pending)
        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun completedHeadsetWriteThenDetachDuringSettleKeepsBothTargetsForFreshReadbackWithoutReplay() = runBlocking {
        listOf(true to false, false to true).forEach { (initial, requested) ->
            val source = FakeSource(
                initialHeadsetControlEnabled = initial,
                restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
            )
            val repository = FiioJa11ControlRepository(source)

            val write = repository.writeControl(
                DacWriteIntent(
                    controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                    expectedSessionGeneration = 7L,
                    requestedValue = DacControlValue.Toggle(requested),
                ),
            )

            assertThat(write).isInstanceOf(FiioJa11ControlWriteResult.ReconnectRequired::class.java)
            val pending = (write as FiioJa11ControlWriteResult.ReconnectRequired).pending
            assertThat(pending.previousSessionGeneration).isEqualTo(7L)
            assertThat(source.sessionGeneration).isEqualTo(8L)
            assertThat(source.headsetWrites).containsExactly(requested)
            assertThat(source.deviceIdentityKey).isEqualTo(pending.deviceIdentityKey)

            val verified = repository.verifyRestartedControl(pending)

            assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
            assertThat(source.headsetWrites).containsExactly(requested)
        }
        Unit
    }

    @Test
    fun restartVerificationAcceptsSameJa11WhenMicStateSelectsAnotherHidInterface() = runBlocking {
        listOf(
            Triple(true, false, 3 to 2),
            Triple(false, true, 2 to 3),
        ).forEach { (initial, requested, interfaces) ->
            val source = FakeSource(
                initialHeadsetControlEnabled = initial,
                initialHidInterfaceId = interfaces.first,
                restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
            )
            val repository = FiioJa11ControlRepository(source)
            val write = repository.writeControl(
                DacWriteIntent(
                    controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                    expectedSessionGeneration = 7L,
                    requestedValue = DacControlValue.Toggle(requested),
                ),
            ) as FiioJa11ControlWriteResult.ReconnectRequired
            val pending = write.pending
            val readsBeforeReplacement = source.firmwareReadCount

            source.selectedHidInterfaceId = interfaces.second

            assertThat(source.deviceIdentityKey).isEqualTo(pending.deviceIdentityKey)
            assertThat(repository.isReplacementSessionCurrent(pending)).isTrue()
            assertThat(repository.hasStableReplacementIdentity(pending)).isTrue()
            val verified = repository.verifyRestartedControl(pending)

            assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
            assertThat((verified as FiioJa11ControlWriteResult.Verified).snapshot.sessionGeneration)
                .isEqualTo(8L)
            assertThat(source.firmwareReadCount).isGreaterThan(readsBeforeReplacement)
            assertThat(source.headsetWrites).containsExactly(requested)
        }
        Unit
    }

    @Test
    fun restartWriteRequiresAStableSerialIdentityBeforeItCanBeScheduled() = runBlocking {
        val source = FakeSource()
        source.missingIdentity = true
        val repository = FiioJa11ControlRepository(source)
        val baseline = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val pending = repository.createPendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            baseline = baseline,
        )

        assertThat(pending).isNull()
        val result = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = baseline.sessionGeneration,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(result).isEqualTo(
            FiioJa11ControlWriteResult.ReadFailed(FiioJa11DeviceControls.HEADSET_CONTROL, "USB identity"),
        )
        assertThat(source.headsetWrites).isEmpty()
    }

    @Test
    fun incompleteRestartWriteIsUncertainAndNeverReportedAsReconnectSuccess() = runBlocking {
        val source = FakeSource(
            restartWriteOutcome = FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN,
        )
        val result = FiioJa11ControlRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(result).isEqualTo(FiioJa11ControlWriteResult.WriteUncertain(FiioJa11DeviceControls.HEADSET_CONTROL))
        assertThat(source.headsetWrites).containsExactly(false)
        Unit
    }

    @Test
    fun restartReadbackMismatchAndReadFailureStayTerminalWithoutReplayingWrite() = runBlocking {
        listOf(false, true).forEach { failRead ->
            val source = FakeSource(
                ignoreHeadsetWrite = true,
                failHeadsetReadAfterWrite = failRead,
                restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
            )
            val repository = FiioJa11ControlRepository(source)
            val write = repository.writeControl(
                DacWriteIntent(
                    controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                    expectedSessionGeneration = 7L,
                    requestedValue = DacControlValue.Toggle(false),
                ),
            ) as FiioJa11ControlWriteResult.ReconnectRequired

            val verified = repository.verifyRestartedControl(write.pending)

            if (failRead) {
                assertThat(verified).isEqualTo(
                    FiioJa11ControlWriteResult.ReadFailed(FiioJa11DeviceControls.HEADSET_CONTROL, "headset control"),
                )
            } else {
                assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.ReadbackMismatch::class.java)
            }
            assertThat(source.headsetWrites).containsExactly(false)
        }
        Unit
    }

    @Test
    fun uacRestoreFromUacOneToUacTwoUsesReplacementSessionAndSameDeviceIdentity() = runBlocking {
        val source = FakeSource(
            initialProductId = FiioJa11Protocol.PRODUCT_ID_UAC_1,
            initialUacMode = FiioJa11Protocol.UacMode.UAC_1,
            restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
        )
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.UAC_MODE,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Discrete("uac_2"),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending
        val verified = repository.verifyRestartedControl(pending)

        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.sessionGeneration).isEqualTo(8L)
        assertThat(source.productId).isEqualTo(FiioJa11Protocol.PRODUCT_ID_UAC_2)
        assertThat(source.uacWrites).containsExactly(FiioJa11Protocol.UacMode.UAC_2)
        assertThat(source.deviceIdentityKey).isEqualTo(pending.deviceIdentityKey)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
    }

    @Test
    fun replacementSessionFromDifferentPhysicalJa11CannotSatisfyPendingWrite() = runBlocking {
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending
        source.sessionGeneration = 8L
        source.deviceIdentityKey = "different-ja11-identity"

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isEqualTo(FiioJa11ControlWriteResult.WrongDevice(FiioJa11DeviceControls.HEADSET_CONTROL))
        assertThat(source.headsetWrites).containsExactly(false)
        Unit
    }

    @Test
    fun replacementSessionWithoutStableIdentityIsNotReadOrAccepted() = runBlocking {
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending
        source.sessionGeneration = 8L
        source.missingIdentity = true
        val firmwareReadsBeforeVerification = source.firmwareReadCount

        val result = repository.verifyRestartedControl(pending)

        assertThat(repository.isSupportedReplacementSessionCurrent(pending)).isTrue()
        assertThat(repository.hasStableReplacementIdentity(pending)).isFalse()
        assertThat(repository.isReplacementSessionCurrent(pending)).isFalse()
        assertThat(result).isEqualTo(
            FiioJa11ControlWriteResult.ReadFailed(FiioJa11DeviceControls.HEADSET_CONTROL, "USB identity"),
        )
        assertThat(source.firmwareReadCount).isEqualTo(firmwareReadsBeforeVerification)
        assertThat(source.headsetWrites).containsExactly(false)
        Unit
    }

    @Test
    fun replacementSessionStatusDoesNotCombineIdentityFromOneGenerationWithAnother() = runBlocking {
        val source = FakeSource(restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE)
        val repository = FiioJa11ControlRepository(source)
        val write = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ) as FiioJa11ControlWriteResult.ReconnectRequired
        source.replaceIdentityWhenRead = true

        assertThat(repository.replacementSessionStatus(write.pending))
            .isEqualTo(FiioJa11ReplacementSessionStatus.OBSERVATION_CHANGED)
        assertThat(repository.isReplacementSessionCurrent(write.pending)).isFalse()
    }

    @Test
    fun replacementDeviceAppearingAfterIdentityCheckIsNotReadOrAccepted() = runBlocking {
        val source = FakeSource(restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE)
        val repository = FiioJa11ControlRepository(source)
        val write = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ) as FiioJa11ControlWriteResult.ReconnectRequired
        val firmwareReadsBeforeVerification = source.firmwareReadCount
        source.replaceIdentityWhenRead = true

        val result = repository.verifyRestartedControl(write.pending)

        assertThat(result).isEqualTo(
            FiioJa11ControlWriteResult.StaleBaseline(
                FiioJa11DeviceControls.HEADSET_CONTROL,
                write.pending.previousSessionGeneration,
                source.sessionGeneration,
            ),
        )
        assertThat(source.firmwareReadCount).isEqualTo(firmwareReadsBeforeVerification)
        assertThat(source.headsetWrites).containsExactly(false)
        Unit
    }

    @Test
    fun replacementIdentityLostAfterWatchdogObservationIsNotReportedAsWrongDevice() = runBlocking {
        val source = FakeSource(restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE)
        val repository = FiioJa11ControlRepository(source)
        val write = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ) as FiioJa11ControlWriteResult.ReconnectRequired
        val firmwareReadsBeforeVerification = source.firmwareReadCount
        assertThat(repository.isReplacementSessionCurrent(write.pending)).isTrue()
        source.missingIdentity = true

        val result = repository.verifyRestartedControl(write.pending)

        assertThat(result).isEqualTo(
            FiioJa11ControlWriteResult.ReadFailed(FiioJa11DeviceControls.HEADSET_CONTROL, "USB identity"),
        )
        assertThat(source.firmwareReadCount).isEqualTo(firmwareReadsBeforeVerification)
        assertThat(source.headsetWrites).containsExactly(false)
        Unit
    }

    @Test
    fun uacWrite0102To0101IsVerifiedOnlyAfterNewPidAndNewSessionAgree() = runBlocking {
        val source = FakeSource(restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE)
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.UAC_MODE,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Discrete("uac_1"),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending

        val verified = repository.verifyRestartedControl(pending)

        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.sessionGeneration).isEqualTo(8L)
        assertThat(source.productId).isEqualTo(FiioJa11Protocol.PRODUCT_ID_UAC_1)
        assertThat(source.uacWrites).containsExactly(FiioJa11Protocol.UacMode.UAC_1)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_1)
        Unit
    }

    @Test
    fun uacWrite0101To0102IsVerifiedOnlyAfterNewPidAndNewSessionAgree() = runBlocking {
        val source = FakeSource(
            initialProductId = FiioJa11Protocol.PRODUCT_ID_UAC_1,
            initialUacMode = FiioJa11Protocol.UacMode.UAC_1,
            restartWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
        )
        val repository = FiioJa11ControlRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.UAC_MODE,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Discrete("uac_2"),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending

        val verified = repository.verifyRestartedControl(pending)

        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.sessionGeneration).isEqualTo(8L)
        assertThat(source.productId).isEqualTo(FiioJa11Protocol.PRODUCT_ID_UAC_2)
        assertThat(source.uacWrites).containsExactly(FiioJa11Protocol.UacMode.UAC_2)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
        Unit
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
        private val initialHeadsetControlEnabled: Boolean = true,
        private val ignoreHeadsetWrite: Boolean = false,
        private val failHeadsetReadAfterWrite: Boolean = false,
        var changeSessionBeforeNextWrite: Boolean = false,
        initialProductId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        initialUacMode: FiioJa11Protocol.UacMode = FiioJa11Protocol.UacMode.UAC_2,
        initialHidInterfaceId: Int = 3,
        private val restartWriteOutcome: FiioJa11ReportWriteOutcome = FiioJa11ReportWriteOutcome.COMPLETED,
    ) : FiioJa11DeviceControlSource {
        override var sessionGeneration: Long = 7L
        var productId: Int = initialProductId
        private var identityKey: String = "fixture-only-ja11-identity"
        private var identityOverride: String? = null
        var missingIdentity: Boolean = false
        var selectedHidInterfaceId: Int = initialHidInterfaceId
        var replaceIdentityWhenRead: Boolean = false
        var firmwareReadCount: Int = 0
        override var deviceIdentityKey: String?
            get() {
                if (missingIdentity) return null
                val current = identityOverride ?: fiioJa11PhysicalIdentityKey(
                    "vid=2972|pid=${productId.toString(16)}|manufacturer=FiiO|product=JA11|" +
                        "serial=$identityKey|interface=$selectedHidInterfaceId",
                )
                if (replaceIdentityWhenRead) {
                    replaceIdentityWhenRead = false
                    identityKey = "different-ja11-identity"
                    sessionGeneration++
                }
                return current
            }
            set(value) {
                identityOverride = value
            }
        var current = true
        var outputVolume = 59
        var sampleRate = "352.8 kHz"
        var firmware = "2.20"
        var headsetControlEnabled = initialHeadsetControlEnabled
        var eqProgram = FiioJa11Protocol.EqProgram.USER_1
        var uacMode = initialUacMode
        val volumeWrites = mutableListOf<Int>()
        val headsetWrites = mutableListOf<Boolean>()
        val uacWrites = mutableListOf<FiioJa11Protocol.UacMode>()

        override val connectedProductId: Int?
            get() = productId

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && sessionGeneration == this.sessionGeneration

        override suspend fun readOutputVolume(): Int? = outputVolume
        override suspend fun readSampleRateLabel(): String? = sampleRate
        override suspend fun readFirmwareVersion(): String? {
            firmwareReadCount++
            return firmware
        }
        override suspend fun readHeadsetControlEnabled(): Boolean? =
            if (failHeadsetReadAfterWrite && headsetWrites.isNotEmpty()) null else headsetControlEnabled
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = eqProgram
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = uacMode

        override suspend fun writeOutputVolume(
            level: Int,
            expectedSessionGeneration: Long,
        ): FiioJa11ReportWriteOutcome {
            if (changeBeforeExpectedWrite(expectedSessionGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            volumeWrites += level
            if (!ignoreVolumeWrite) outputVolume = level
            return FiioJa11ReportWriteOutcome.COMPLETED
        }

        override suspend fun writeHeadsetControlEnabled(
            enabled: Boolean,
            expectedSessionGeneration: Long,
        ): FiioJa11ReportWriteOutcome {
            if (changeBeforeExpectedWrite(expectedSessionGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            headsetWrites += enabled
            if (!ignoreHeadsetWrite && (restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED ||
                restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE
            )) {
                headsetControlEnabled = enabled
            }
            if (restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE) {
                sessionGeneration++
            }
            return restartWriteOutcome
        }

        override suspend fun writeEqProgram(
            program: FiioJa11Protocol.EqProgram,
            expectedSessionGeneration: Long,
        ): FiioJa11ReportWriteOutcome {
            if (changeBeforeExpectedWrite(expectedSessionGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            eqProgram = program
            return FiioJa11ReportWriteOutcome.COMPLETED
        }

        override suspend fun writeUacMode(
            mode: FiioJa11Protocol.UacMode,
            expectedSessionGeneration: Long,
        ): FiioJa11ReportWriteOutcome {
            if (changeBeforeExpectedWrite(expectedSessionGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            uacWrites += mode
            if (restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED ||
                restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE
            ) {
                uacMode = mode
                productId = mode.productId
            }
            if (restartWriteOutcome == FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE) {
                sessionGeneration++
            }
            return restartWriteOutcome
        }

        private fun changeBeforeExpectedWrite(expectedSessionGeneration: Long): Boolean {
            if (changeSessionBeforeNextWrite) {
                changeSessionBeforeNextWrite = false
                sessionGeneration++
            }
            return !isSessionCurrent(expectedSessionGeneration)
        }
    }
}
