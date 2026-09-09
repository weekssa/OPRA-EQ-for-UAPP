package com.weekssa.opraeqforuapp.data.hardware

import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlatResetResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Flasher
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository boundary for deterministic hardware-EQ transactions and verified native EQ reads.
 *
 * Physical USB-session lifecycle belongs to [DacSessionRepository]. This repository serializes each
 * device's multi-step EQ reads/writes so a snapshot cannot interleave with a Flash/Reset transaction.
 * Black Pearl publishes the most recently verified native snapshot with explicit current/stale state;
 * the other two devices keep their existing v0.5 behavior until their My DAC exposure is qualified.
 */
class HardwareEqRepository(
    private val dacSessionRepository: DacSessionRepository,
    private val blackPearlFlasher: BlackPearlFlasher,
    private val fiioJa11Flasher: FiioJa11Flasher,
    private val jcallyJm12Flasher: JcallyJm12Flasher,
) : Closeable {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> =
        dacSessionRepository.blackPearlConnectionState
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.fiioJa11ConnectionState
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.jcallyJm12ConnectionState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blackPearlOperationMutex = Mutex()
    private val fiioJa11OperationMutex = Mutex()
    private val jcallyJm12OperationMutex = Mutex()
    private val mutableBlackPearlSnapshotState = MutableStateFlow(HardwareEqSnapshotState())
    val blackPearlSnapshotState: StateFlow<HardwareEqSnapshotState> =
        mutableBlackPearlSnapshotState.asStateFlow()

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
    }

    fun connectBlackPearl() = dacSessionRepository.connectBlackPearl()

    fun connectFiioJa11() = dacSessionRepository.connectFiioJa11()

    fun connectJcallyJm12() = dacSessionRepository.connectJcallyJm12()

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isBlackPearlSessionCurrent(sessionGeneration)

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isFiioJa11SessionCurrent(sessionGeneration)

    fun isJcallyJm12SessionCurrent(sessionGeneration: Long): Boolean =
        dacSessionRepository.isJcallyJm12SessionCurrent(sessionGeneration)

    suspend fun readBlackPearlSnapshot(): HardwareEqSnapshotBundle? = refreshBlackPearlSnapshot()

    suspend fun readFiioJa11Snapshot(): HardwareEqSnapshotBundle? = fiioJa11OperationMutex.withLock {
        dacSessionRepository.readFiioJa11Snapshot()
    }

    suspend fun readJcallyJm12Snapshot(): HardwareEqSnapshotBundle? = jcallyJm12OperationMutex.withLock {
        dacSessionRepository.readJcallyJm12Snapshot()
    }

    suspend fun flashBlackPearl(profile: OpraEqProfile): BlackPearlFlashResult {
        mutableBlackPearlSnapshotState.update { it.markStale() }
        return try {
            blackPearlOperationMutex.withLock { blackPearlFlasher.flash(profile) }
        } finally {
            scheduleBlackPearlSnapshotRefresh()
        }
    }

    suspend fun resetBlackPearl(): BlackPearlFlatResetResult {
        mutableBlackPearlSnapshotState.update { it.markStale() }
        return try {
            blackPearlOperationMutex.withLock { blackPearlFlasher.resetToFlat() }
        } finally {
            scheduleBlackPearlSnapshotRefresh()
        }
    }

    suspend fun flashFiioJa11(profile: OpraEqProfile): Kt02h20FlashResult = fiioJa11OperationMutex.withLock {
        fiioJa11Flasher.flash(profile)
    }

    suspend fun resetFiioJa11(): Kt02h20FlatResetResult = fiioJa11OperationMutex.withLock {
        fiioJa11Flasher.resetToFlat()
    }

    suspend fun flashJcallyJm12(profile: OpraEqProfile): Kt02h20FlashResult = jcallyJm12OperationMutex.withLock {
        jcallyJm12Flasher.flash(profile)
    }

    suspend fun resetJcallyJm12(): Kt02h20FlatResetResult = jcallyJm12OperationMutex.withLock {
        jcallyJm12Flasher.resetToFlat()
    }

    private suspend fun refreshBlackPearlSnapshot(): HardwareEqSnapshotBundle? =
        blackPearlOperationMutex.withLock {
            if (blackPearlConnectionState.value !is BlackPearlConnectionState.Connected) {
                mutableBlackPearlSnapshotState.update { it.markStale() }
                return@withLock null
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

    private fun scheduleBlackPearlSnapshotRefresh() {
        scope.launch {
            if (blackPearlConnectionState.value is BlackPearlConnectionState.Connected) {
                refreshBlackPearlSnapshot()
            } else {
                mutableBlackPearlSnapshotState.update { it.markStale() }
            }
        }
    }

    override fun close() {
        scope.cancel()
        dacSessionRepository.close()
    }
}
