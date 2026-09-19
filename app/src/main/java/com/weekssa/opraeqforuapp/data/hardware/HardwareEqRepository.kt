package com.weekssa.opraeqforuapp.data.hardware

import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlEditorApplyResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlatResetResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Flasher
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityBatch
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualificationResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualifier
import com.weekssa.opraeqforuapp.domain.ew300.Ew300EditorApplyResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Repository boundary for deterministic hardware-EQ transactions and verified native EQ reads.
 *
 * Physical USB-session lifecycle, recognition, and the Black Pearl/FiiO operation serialization
 * boundaries belong to [DacSessionRepository]. Sharing those gates with DEVICE repositories prevents
 * an EQ read/Flash/Reset from interleaving with a DEVICE transaction on the same physical DAC.
 *
 * JCALLY is intentionally not a current hardware runtime. Temporary compatibility stubs below keep
 * older UI/data call sites non-destructive while the remaining product plumbing is removed; they
 * never open a JCALLY USB session or perform a write.
 */
class HardwareEqRepository(
    private val dacSessionRepository: DacSessionRepository,
    private val blackPearlFlasher: BlackPearlFlasher,
    private val fiioJa11Flasher: FiioJa11Flasher,
    private val ew300Flasher: Ew300Flasher,
    private val ew300CapabilityBatch: Ew300CapabilityBatch,
    private val ew300PersistenceQualifier: Ew300PersistenceQualifier,
) : Closeable {
    val recognitionState: StateFlow<DacRecognitionState> = dacSessionRepository.recognitionState
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> =
        dacSessionRepository.blackPearlConnectionState
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.fiioJa11ConnectionState
    val ew300ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.ew300ConnectionState

    private val mutableUnsupportedJcallyState = MutableStateFlow<Kt02h20ConnectionState>(
        Kt02h20ConnectionState.Disconnected,
    )
    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> =
        mutableUnsupportedJcallyState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableBlackPearlSnapshotState = MutableStateFlow(HardwareEqSnapshotState())
    val blackPearlSnapshotState: StateFlow<HardwareEqSnapshotState> =
        mutableBlackPearlSnapshotState.asStateFlow()
    private val mutableFiioJa11SnapshotState = MutableStateFlow(HardwareEqSnapshotState())
    val fiioJa11SnapshotState: StateFlow<HardwareEqSnapshotState> =
        mutableFiioJa11SnapshotState.asStateFlow()
    private val mutableEw300SnapshotState = MutableStateFlow(HardwareEqSnapshotState())
    val ew300SnapshotState: StateFlow<HardwareEqSnapshotState> =
        mutableEw300SnapshotState.asStateFlow()

    init {
        scope.launch {
            blackPearlConnectionState.collectLatest { state ->
                if (state is BlackPearlConnectionState.Connected) {
                    refreshBlackPearlSnapshot()
                } else {
                    mutableBlackPearlSnapshotState.update { it.markStale() }
                }
            }
        }
        scope.launch {
            fiioJa11ConnectionState.collectLatest { state ->
                if (state is Kt02h20ConnectionState.Connected) {
                    refreshFiioJa11Snapshot()
                } else {
                    mutableFiioJa11SnapshotState.update { it.markStale() }
                }
            }
        }
        scope.launch {
            ew300ConnectionState.collectLatest { state ->
                if (state is Kt02h20ConnectionState.Connected) {
                    refreshEw300Snapshot()
                } else {
                    mutableEw300SnapshotState.update { it.markStale() }
                }
            }
        }
    }

    fun connectBlackPearl() = dacSessionRepository.connectBlackPearl()
    fun connectFiioJa11() = dacSessionRepository.connectFiioJa11()
    fun connectEw300() = dacSessionRepository.connectEw300()

    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    fun connectJcallyJm12() = Unit

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isBlackPearlSessionCurrent(sessionGeneration)

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isFiioJa11SessionCurrent(sessionGeneration)

    fun isEw300SessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isEw300SessionCurrent(sessionGeneration)

    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    fun isJcallyJm12SessionCurrent(sessionGeneration: Long): Boolean = false

    fun readBlackPearlTrackedGainDeltaDb(): Double =
        blackPearlFlasher.readTrackedAppliedPlaybackGainDb()

    suspend fun readBlackPearlSnapshot(): HardwareEqSnapshotBundle? = refreshBlackPearlSnapshot()

    suspend fun readFiioJa11Snapshot(): HardwareEqSnapshotBundle? = refreshFiioJa11Snapshot()

    suspend fun readEw300Snapshot(): HardwareEqSnapshotBundle? = refreshEw300Snapshot()

    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    suspend fun readJcallyJm12Snapshot(): HardwareEqSnapshotBundle? = null

    suspend fun applyBlackPearlEditor(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
    ): BlackPearlEditorApplyResult {
        if (workingCopy.cautions.isNotEmpty() && !allowCautions) {
            return BlackPearlEditorApplyResult.ConfirmationRequired(workingCopy.cautions.size)
        }

        mutableBlackPearlSnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveBlackPearlOperation {
                blackPearlFlasher.applyEditorWorkingCopy(
                    workingCopy = workingCopy,
                    allowCautions = allowCautions,
                    isSessionCurrent = dacSessionRepository::isBlackPearlSessionCurrent,
                )
            }
        } finally {
            refreshBlackPearlSnapshot()
        }
    }

    suspend fun flashBlackPearl(profile: OpraEqProfile): BlackPearlFlashResult {
        mutableBlackPearlSnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveBlackPearlOperation {
                blackPearlFlasher.flash(profile)
            }
        } finally {
            scheduleBlackPearlSnapshotRefresh()
        }
    }

    suspend fun resetBlackPearl(): BlackPearlFlatResetResult {
        mutableBlackPearlSnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveBlackPearlOperation {
                blackPearlFlasher.resetToFlat()
            }
        } finally {
            scheduleBlackPearlSnapshotRefresh()
        }
    }

    suspend fun flashFiioJa11(profile: OpraEqProfile): Kt02h20FlashResult {
        mutableFiioJa11SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveFiioJa11Operation {
                fiioJa11Flasher.flash(profile)
            }
        } finally {
            scheduleFiioJa11SnapshotRefresh()
        }
    }

    suspend fun resetFiioJa11(): Kt02h20FlatResetResult {
        mutableFiioJa11SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveFiioJa11Operation {
                fiioJa11Flasher.resetToFlat()
            }
        } finally {
            scheduleFiioJa11SnapshotRefresh()
        }
    }

    suspend fun flashEw300(profile: OpraEqProfile): Kt02h20FlashResult {
        mutableEw300SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveEw300Operation {
                ew300Flasher.flash(profile)
            }
        } finally {
            scheduleEw300SnapshotRefresh()
        }
    }

    suspend fun applyEw300Editor(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
    ): Ew300EditorApplyResult {
        mutableEw300SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveEw300Operation {
                ew300Flasher.applyEditorWorkingCopy(
                    workingCopy = workingCopy,
                    allowCautions = allowCautions,
                    isSessionCurrent = dacSessionRepository::isEw300SessionCurrent,
                )
            }
        } finally {
            refreshEw300Snapshot()
        }
    }

    /** Runs the allowlisted read-only beta diagnostic under the shared EW300 operation gate. */
    suspend fun runEw300CapabilityBatch(): Ew300CapabilityReport =
        dacSessionRepository.withExclusiveEw300Operation { ew300CapabilityBatch.run() }

    suspend fun advanceEw300PersistenceQualification(): Ew300PersistenceQualificationResult =
        dacSessionRepository.withExclusiveEw300Operation { ew300PersistenceQualifier.advance() }

    suspend fun resetEw300(): Kt02h20FlatResetResult =
        dacSessionRepository.withExclusiveEw300Operation { ew300Flasher.resetToFlat() }.also {
            scheduleEw300SnapshotRefresh()
        }

    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    suspend fun flashJcallyJm12(profile: OpraEqProfile): Kt02h20FlashResult =
        Kt02h20FlashResult.DeviceUnavailable("JCALLY JM12 is not supported by the current product.")

    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    suspend fun resetJcallyJm12(): Kt02h20FlatResetResult =
        Kt02h20FlatResetResult.DeviceUnavailable("JCALLY JM12 is not supported by the current product.")

    private suspend fun refreshBlackPearlSnapshot(): HardwareEqSnapshotBundle? =
        dacSessionRepository.withExclusiveBlackPearlOperation {
            if (blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
                mutableBlackPearlSnapshotState.update { it.markStale() }
                return@withExclusiveBlackPearlOperation null
            }

            mutableBlackPearlSnapshotState.update { it.beginRead() }
            val bundle = dacSessionRepository.readBlackPearlSnapshot()
            val stillCurrent = bundle != null &&
                dacSessionRepository.isBlackPearlSessionCurrent(bundle.snapshot.sessionGeneration)

            when {
                stillCurrent -> {
                    mutableBlackPearlSnapshotState.update { it.publishCurrent(requireNotNull(bundle)) }
                    bundle
                }
                blackPearlConnectionState.value !is BlackPearlConnectionState.Connected -> {
                    mutableBlackPearlSnapshotState.update { it.markStale() }
                    null
                }
                else -> {
                    mutableBlackPearlSnapshotState.update { it.markReadFailed() }
                    null
                }
            }
        }

    private suspend fun refreshFiioJa11Snapshot(): HardwareEqSnapshotBundle? =
        dacSessionRepository.withExclusiveFiioJa11Operation {
            if (fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected) {
                mutableFiioJa11SnapshotState.update { it.markStale() }
                return@withExclusiveFiioJa11Operation null
            }

            mutableFiioJa11SnapshotState.update { it.beginRead() }
            val bundle = dacSessionRepository.readFiioJa11Snapshot()
            val stillCurrent = bundle != null &&
                dacSessionRepository.isFiioJa11SessionCurrent(bundle.snapshot.sessionGeneration)

            when {
                stillCurrent -> {
                    mutableFiioJa11SnapshotState.update { it.publishCurrent(requireNotNull(bundle)) }
                    bundle
                }
                fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected -> {
                    mutableFiioJa11SnapshotState.update { it.markStale() }
                    null
                }
                else -> {
                    // Built-in JA11 programs intentionally have no established coefficient snapshot.
                    // The DEVICE state still reports the active program exactly; no fake curve is shown.
                    mutableFiioJa11SnapshotState.update { it.markReadFailed() }
                    null
                }
            }
        }

    private fun scheduleBlackPearlSnapshotRefresh() {
        scope.launch {
            if (blackPearlConnectionState.value is BlackPearlConnectionState.Connected) {
                refreshBlackPearlSnapshot()
            } else {
                mutableBlackPearlSnapshotState.update { it.markStale() }
            }
        }
    }

    private fun scheduleFiioJa11SnapshotRefresh() {
        scope.launch {
            if (fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected) {
                refreshFiioJa11Snapshot()
            } else {
                mutableFiioJa11SnapshotState.update { it.markStale() }
            }
        }
    }

    private suspend fun refreshEw300Snapshot(): HardwareEqSnapshotBundle? =
        dacSessionRepository.withExclusiveEw300Operation {
            if (ew300ConnectionState.value !is Kt02h20ConnectionState.Connected) {
                mutableEw300SnapshotState.update { it.markStale() }
                return@withExclusiveEw300Operation null
            }
            mutableEw300SnapshotState.update { it.beginRead() }
            val bundle = dacSessionRepository.readEw300Snapshot()
            val stillCurrent = bundle != null &&
                dacSessionRepository.isEw300SessionCurrent(bundle.snapshot.sessionGeneration)
            when {
                stillCurrent -> {
                    mutableEw300SnapshotState.update { it.publishCurrent(requireNotNull(bundle)) }
                    bundle
                }
                ew300ConnectionState.value !is Kt02h20ConnectionState.Connected -> {
                    mutableEw300SnapshotState.update { it.markStale() }
                    null
                }
                else -> {
                    mutableEw300SnapshotState.update { it.markReadFailed() }
                    null
                }
            }
        }

    private fun scheduleEw300SnapshotRefresh() {
        scope.launch {
            if (ew300ConnectionState.value is Kt02h20ConnectionState.Connected) {
                refreshEw300Snapshot()
            } else {
                mutableEw300SnapshotState.update { it.markStale() }
            }
        }
    }

    override fun close() {
        scope.cancel()
        dacSessionRepository.close()
    }
}
