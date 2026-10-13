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
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11EditorBaseline
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11EditorApplyResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Flasher
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityBatch
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualificationResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualifier
import com.weekssa.opraeqforuapp.domain.ew300.Ew300EditorApplyResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStatus
import com.weekssa.opraeqforuapp.domain.ew300.Ew300RestorationResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PlaybackGainResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceDiagnostics
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceEvent
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceComponent
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceOwner
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergencePhase
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceReadContext
import kotlinx.coroutines.CancellationException
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
    internal val convergenceDiagnostics: Ja11ConvergenceDiagnostics? = null,
) : Closeable {
    val recognitionState: StateFlow<DacRecognitionState> = dacSessionRepository.recognitionState
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> =
        dacSessionRepository.blackPearlConnectionState
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.fiioJa11ConnectionState
    val ew300ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.ew300ConnectionState
    val ew300OperationTrace: StateFlow<Ew300OperationTrace?> = ew300Flasher.lastOperationTrace
    val ew300OperationStatus: StateFlow<Ew300OperationStatus> = ew300Flasher.operationStatus
    val fiioJa11OperationTrace: StateFlow<FiioJa11OperationTrace?> = fiioJa11Flasher.lastOperationTrace
    val fiioJa11OperationStatus: StateFlow<FiioJa11OperationStatus> = fiioJa11Flasher.operationStatus

    private val mutableUnsupportedJcallyState = MutableStateFlow<Kt02h20ConnectionState>(
        Kt02h20ConnectionState.Disconnected,
    )
    @Deprecated("JCALLY is not part of the current product; remove remaining callers.")
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> =
        mutableUnsupportedJcallyState.asStateFlow()

    internal val diagnosticInstanceId = convergenceDiagnostics?.allocateId() ?: 0L
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
        recordConvergence(Ja11ConvergencePhase.CREATED)
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
                recordConvergence(
                    if (state is Kt02h20ConnectionState.Connected) Ja11ConvergencePhase.CONNECTED
                    else Ja11ConvergencePhase.NOT_CONNECTED,
                    owner = Ja11ConvergenceOwner.CONNECTED_OBSERVER,
                )
                if (state is Kt02h20ConnectionState.Connected) {
                    refreshFiioJa11Snapshot(owner = Ja11ConvergenceOwner.CONNECTED_OBSERVER)
                } else {
                    mutableFiioJa11SnapshotState.update { it.markStale() }
                    recordConvergence(Ja11ConvergencePhase.STALE, owner = Ja11ConvergenceOwner.CONNECTED_OBSERVER)
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

    /**
     * Connects EW300 when closed; when the authoritative session is already open, this is the
     * My DAC manual Refresh escape hatch and performs a read-only snapshot refresh instead of
     * opening/replacing the USB session.
     */
    fun connectEw300() {
        if (ew300ConnectionState.value is Kt02h20ConnectionState.Connected) {
            scheduleEw300SnapshotRefresh()
        } else {
            dacSessionRepository.connectEw300()
        }
    }

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

    fun readBlackPearlTrackedGainDeltaDb(): Double? =
        blackPearlFlasher.readTrackedAppliedPlaybackGainDb()

    suspend fun readBlackPearlSnapshot(): HardwareEqSnapshotBundle? = refreshBlackPearlSnapshot()

    suspend fun readFiioJa11Snapshot(
        owner: Ja11ConvergenceOwner = Ja11ConvergenceOwner.EXPLICIT_READ,
    ): HardwareEqSnapshotBundle? = refreshFiioJa11Snapshot(owner = owner)

    /** Captures exact identity metadata for one already-current verified User 1 snapshot. */
    fun fiioJa11EditorBaseline(bundle: HardwareEqSnapshotBundle): FiioJa11EditorBaseline? {
        if (fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected) return null
        if (fiioJa11SnapshotState.value.freshness != DacStateFreshness.CURRENT) return null
        if (fiioJa11SnapshotState.value.bundle != bundle) return null
        if (!isFiioJa11SessionCurrent(bundle.snapshot.sessionGeneration)) return null
        val fingerprintKey = dacSessionRepository.fiioJa11Transport.deviceFingerprintKey ?: return null
        val productId = dacSessionRepository.fiioJa11Transport.usbProductId ?: return null
        return runCatching {
            FiioJa11EditorBaseline(
                deviceFingerprintKey = fingerprintKey,
                usbProductId = productId,
                snapshotBundle = bundle,
            )
        }.getOrNull()
    }

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
                blackPearlFlasher.flash(
                    profile = profile,
                    expectedSessionGeneration = dacSessionRepository.blackPearlTransport.sessionGeneration,
                    isSessionCurrent = dacSessionRepository::isBlackPearlSessionCurrent,
                )
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
        recordConvergence(Ja11ConvergencePhase.FLASH_STARTED, owner = Ja11ConvergenceOwner.FLASH_COMPLETION)
        return try {
            completeFiioJa11Flash(
                flash = {
                    dacSessionRepository.withExclusiveFiioJa11Operation {
                        fiioJa11Flasher.flash(profile)
                    }.also {
                        recordConvergence(Ja11ConvergencePhase.MUTATION_RETURNED, owner = Ja11ConvergenceOwner.FLASH_COMPLETION)
                    }
                },
                isConnected = { fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected },
                refreshSnapshot = { refreshFiioJa11Snapshot(allowBoundedRetry = true, owner = Ja11ConvergenceOwner.FLASH_COMPLETION) },
                markSnapshotStale = {
                    mutableFiioJa11SnapshotState.update { it.markStale() }
                    recordConvergence(Ja11ConvergencePhase.STALE, owner = Ja11ConvergenceOwner.FLASH_COMPLETION)
                },
            ).also { recordConvergence(Ja11ConvergencePhase.FLASH_RETURNED, owner = Ja11ConvergenceOwner.FLASH_COMPLETION) }
        } catch (cancelled: CancellationException) {
            recordConvergence(Ja11ConvergencePhase.CANCELLED, owner = Ja11ConvergenceOwner.FLASH_COMPLETION)
            throw cancelled
        } catch (failure: Throwable) {
            recordConvergence(Ja11ConvergencePhase.THREW, owner = Ja11ConvergenceOwner.FLASH_COMPLETION)
            throw failure
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

    suspend fun applyFiioJa11Editor(
        workingCopy: HardwareEqEditWorkingCopy,
        baseline: FiioJa11EditorBaseline,
    ): FiioJa11EditorApplyResult {
        mutableFiioJa11SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveFiioJa11Operation {
                fiioJa11Flasher.applyEditorWorkingCopy(
                    workingCopy = workingCopy,
                    baseline = baseline,
                )
            }
        } finally {
            scheduleFiioJa11SnapshotRefresh()
        }
    }

    suspend fun flashEw300(profile: OpraEqProfile): Kt02h20FlashResult {
        mutableEw300SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveEw300Mutation {
                ew300Flasher.flash(profile)
            }
        } finally {
            scheduleEw300SnapshotRefresh()
        }
    }

    /** Restores the exact baseline captured by the last verified EW300 Flash. */
    suspend fun restoreEw300LastFlashBaseline(): Ew300RestorationResult {
        mutableEw300SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveEw300Mutation {
                ew300Flasher.restoreLastFlashBaseline()
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
            dacSessionRepository.withExclusiveEw300Mutation {
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
        dacSessionRepository.withExclusiveEw300Mutation { ew300PersistenceQualifier.advance() }

    suspend fun resetEw300(): Kt02h20FlatResetResult =
        dacSessionRepository.withExclusiveEw300Mutation { ew300Flasher.resetToFlat() }.also {
            scheduleEw300SnapshotRefresh()
        }

    suspend fun setEw300PlaybackGain(gainDb: Double): Ew300PlaybackGainResult {
        mutableEw300SnapshotState.update { it.markStale() }
        return try {
            dacSessionRepository.withExclusiveEw300Mutation {
                ew300Flasher.setPlaybackGain(gainDb)
            }
        } finally {
            refreshEw300Snapshot()
        }
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
                    // A complete read-only snapshot is the only recovery boundary that may clear
                    // an unknown anti-stacking baseline. This never retries or mutates hardware.
                    blackPearlFlasher.establishFreshGainBaselineAfterVerifiedSnapshot()
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

    private suspend fun refreshFiioJa11Snapshot(
        allowBoundedRetry: Boolean = false,
        owner: Ja11ConvergenceOwner = Ja11ConvergenceOwner.EXPLICIT_READ,
    ): HardwareEqSnapshotBundle? {
        val acquisitionId = convergenceDiagnostics?.allocateId()
        var attempt = 0
        fun record(phase: Ja11ConvergencePhase, bundleGeneration: Long? = null) {
            recordConvergence(phase, owner, acquisitionId, attempt.takeIf { it > 0 }, bundleGeneration)
        }
        record(Ja11ConvergencePhase.REFRESH_REQUESTED)
        return try {
            dacSessionRepository.withExclusiveFiioJa11Operation {
                record(Ja11ConvergencePhase.REFRESH_ENTERED)
                if (fiioJa11ConnectionState.value !is Kt02h20ConnectionState.Connected) {
                    mutableFiioJa11SnapshotState.update { it.markStale() }
                    record(Ja11ConvergencePhase.GUARD_REJECTED)
                    return@withExclusiveFiioJa11Operation null
                }

                readFiioJa11SnapshotWithBoundedRetry(
                    allowRetry = allowBoundedRetry,
                    isConnected = { fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected },
                    currentSessionGeneration = { dacSessionRepository.fiioJa11Transport.sessionGeneration },
                    isSessionCurrent = dacSessionRepository::isFiioJa11SessionCurrent,
                    read = {
                        mutableFiioJa11SnapshotState.update { it.beginRead() }
                        dacSessionRepository.readFiioJa11Snapshot(acquisitionId?.let {
                            Ja11ConvergenceReadContext(it, diagnosticInstanceId, owner, currentOperationId(), attempt)
                        })
                    },
                    publishCurrent = { bundle -> mutableFiioJa11SnapshotState.update { it.publishCurrent(bundle) } },
                    markReadFailed = { mutableFiioJa11SnapshotState.update { it.markReadFailed() } },
                    markStale = { mutableFiioJa11SnapshotState.update { it.markStale() } },
                    observe = if (convergenceDiagnostics == null) null else { phase, index, generation ->
                        attempt = index
                        record(phase, generation)
                    },
                )
            }
        } catch (cancelled: CancellationException) {
            record(Ja11ConvergencePhase.CANCELLED)
            throw cancelled
        } catch (failure: Throwable) {
            record(Ja11ConvergencePhase.THREW)
            throw failure
        } finally {
            record(Ja11ConvergencePhase.REFRESH_ENDED)
        }
    }

    private fun currentOperationId(): String? = when (val status = fiioJa11OperationStatus.value) {
        is FiioJa11OperationStatus.Running -> status.operationId
        is FiioJa11OperationStatus.Completed -> status.trace.operationId
        FiioJa11OperationStatus.Idle -> null
    }

    private fun recordConvergence(
        phase: Ja11ConvergencePhase,
        owner: Ja11ConvergenceOwner = Ja11ConvergenceOwner.NONE,
        acquisitionId: Long? = null,
        attempt: Int? = null,
        bundleGeneration: Long? = null,
    ) {
        val recorder = convergenceDiagnostics ?: return
        val state = fiioJa11SnapshotState.value
        val generation = dacSessionRepository.fiioJa11Transport.sessionGeneration
        recorder.record(Ja11ConvergenceEvent(
            Ja11ConvergenceComponent.HARDWARE_REPOSITORY, diagnosticInstanceId, phase,
            owner = owner, acquisitionId = acquisitionId,
            operationId = if (phase == Ja11ConvergencePhase.FLASH_STARTED) null else currentOperationId(),
            attempt = attempt,
            generation = generation, bundleGeneration = bundleGeneration ?: state.bundle?.snapshot?.sessionGeneration,
            connected = fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected,
            sessionCurrent = dacSessionRepository.isFiioJa11SessionCurrent(generation),
            bundleCurrent = state.freshness == DacStateFreshness.CURRENT && state.bundle?.let {
                dacSessionRepository.isFiioJa11SessionCurrent(it.snapshot.sessionGeneration)
            } == true,
            bundlePresent = state.bundle != null, reading = state.isReading, readFailed = state.readFailed,
        ))
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
                refreshFiioJa11Snapshot(owner = Ja11ConvergenceOwner.SCHEDULED_REFRESH)
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
                    // A Save may have completed while Android's replacement permission/reconnect
                    // window was still open. Reconcile only after this fresh, exact snapshot has
                    // passed the authoritative session check; the flasher performs a second
                    // complete read-only byte comparison and never replays the mutation.
                    ew300Flasher.reconcilePendingVerification()
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
        recordConvergence(Ja11ConvergencePhase.CLOSED)
        scope.cancel()
        dacSessionRepository.close()
    }
}

/**
 * Keeps a successful JA11 Flash and its replacement-session snapshot publication one completion
 * boundary. The UI must not observe Flash success before the authoritative EQ state is available.
 */
internal suspend fun <T> completeFiioJa11Flash(
    flash: suspend () -> T,
    isConnected: () -> Boolean,
    refreshSnapshot: suspend () -> Unit,
    markSnapshotStale: () -> Unit,
): T = try {
    flash()
} finally {
    if (isConnected()) refreshSnapshot() else markSnapshotStale()
}

/**
 * Completes the post-Flash read boundary without replaying any hardware mutation. A missing or
 * rejected first bundle gets exactly one fresh read, but only while the replacement session is
 * still connected and current.
 */
internal suspend fun readFiioJa11SnapshotWithBoundedRetry(
    allowRetry: Boolean,
    isConnected: () -> Boolean,
    currentSessionGeneration: () -> Long,
    isSessionCurrent: (Long) -> Boolean,
    read: suspend () -> HardwareEqSnapshotBundle?,
    publishCurrent: (HardwareEqSnapshotBundle) -> Unit,
    markReadFailed: () -> Unit,
    markStale: () -> Unit,
    observe: ((Ja11ConvergencePhase, Int, Long?) -> Unit)? = null,
): HardwareEqSnapshotBundle? {
    val attempts = if (allowRetry) 2 else 1
    repeat(attempts) { attempt ->
        if (!isConnected()) {
            observe?.invoke(Ja11ConvergencePhase.GUARD_REJECTED, attempt + 1, null)
            markStale()
            return null
        }

        val sessionGeneration = currentSessionGeneration()
        if (sessionGeneration <= 0L || !isSessionCurrent(sessionGeneration)) {
            observe?.invoke(Ja11ConvergencePhase.GUARD_REJECTED, attempt + 1, null)
            markReadFailed()
            return null
        }

        observe?.invoke(Ja11ConvergencePhase.ATTEMPT_STARTED, attempt + 1, null)
        val bundle = read()
        observe?.invoke(
            if (bundle == null) Ja11ConvergencePhase.READ_NO_BUNDLE else Ja11ConvergencePhase.READ_BUNDLE,
            attempt + 1, bundle?.snapshot?.sessionGeneration,
        )
        if (
            bundle != null &&
            isConnected() &&
            isSessionCurrent(bundle.snapshot.sessionGeneration)
        ) {
            publishCurrent(bundle)
            observe?.invoke(Ja11ConvergencePhase.PUBLISHED, attempt + 1, bundle.snapshot.sessionGeneration)
            return bundle
        }

        if (attempt == 0 && allowRetry && isConnected()) return@repeat
    }

    if (isConnected()) {
        markReadFailed()
        observe?.invoke(Ja11ConvergencePhase.READ_FAILED, attempts, null)
    } else {
        markStale()
        observe?.invoke(Ja11ConvergencePhase.STALE, attempts, null)
    }
    return null
}
