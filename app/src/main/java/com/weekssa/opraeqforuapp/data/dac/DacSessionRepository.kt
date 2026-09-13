package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ViewModel-scoped owner/coordinator for current-product physical USB sessions.
 *
 * Current My DAC recognition and session ownership are intentionally limited to TRN Black Pearl
 * and FiiO. Historical JCALLY protocol knowledge may remain in research/reference files, but no
 * JCALLY transport is opened or owned by the current product.
 *
 * Per-device operation locks live here, at the physical-session owner, so EQ and DEVICE repositories
 * can share one serialization boundary rather than each believing it exclusively owns the same HID
 * session.
 */
class DacSessionRepository(
    internal val blackPearlTransport: AndroidBlackPearlUsbTransport,
    internal val fiioJa11Transport: AndroidFiioJa11UsbTransport,
) : Closeable {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> = blackPearlTransport.state
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> = fiioJa11Transport.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blackPearlOperationMutex = Mutex()
    private val fiioJa11OperationMutex = Mutex()
    private val mutableRecognitionState = MutableStateFlow(
        DacRecognitionState().withPresentDevices(currentPresentDeviceIds()),
    )
    val recognitionState: StateFlow<DacRecognitionState> = mutableRecognitionState.asStateFlow()

    private val blackPearlSnapshotReader = BlackPearlSnapshotReader(
        source = object : BlackPearlSnapshotSource {
            override suspend fun readNativeBand(index: Int) = blackPearlTransport.readNativeBand(index)
            override suspend fun readGlobalGainRaw(): Int? = blackPearlTransport.readGlobalGainRaw()
        },
    )
    private val fiioJa11SnapshotReader = FiioJa11SnapshotReader(
        source = object : FiioJa11SnapshotSource {
            override suspend fun readEqProgram() = fiioJa11Transport.readEqProgram()
            override suspend fun readBand(index: Int) = fiioJa11Transport.readBand(index)
            override suspend fun readGlobalGainDb() = fiioJa11Transport.readGlobalGainDb()
        },
    )

    init {
        scope.launch {
            combine(
                blackPearlTransport.present,
                fiioJa11Transport.present,
            ) { blackPearlPresent, fiioJa11Present ->
                buildSet {
                    if (blackPearlPresent) add(DacDeviceId.TRN_BLACK_PEARL)
                    if (fiioJa11Present) add(DacDeviceId.FIIO_JA11)
                }
            }.collect { presentDeviceIds ->
                mutableRecognitionState.update { previous -> previous.withPresentDevices(presentDeviceIds) }
            }
        }
    }

    fun connectBlackPearl() = blackPearlTransport.connect()
    fun connectFiioJa11() = fiioJa11Transport.connect()

    suspend fun <T> withExclusiveBlackPearlOperation(block: suspend () -> T): T =
        blackPearlOperationMutex.withLock { block() }

    suspend fun <T> withExclusiveFiioJa11Operation(block: suspend () -> T): T =
        fiioJa11OperationMutex.withLock { block() }

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            blackPearlConnectionState.value is BlackPearlConnectionState.Connected &&
            blackPearlTransport.sessionGeneration == sessionGeneration

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            fiioJa11Transport.sessionGeneration == sessionGeneration

    suspend fun readBlackPearlSnapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = { blackPearlTransport.sessionGeneration },
        isConnected = { blackPearlConnectionState.value is BlackPearlConnectionState.Connected },
        read = blackPearlSnapshotReader::read,
    )

    suspend fun readFiioJa11Snapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = { fiioJa11Transport.sessionGeneration },
        isConnected = { fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected },
        read = fiioJa11SnapshotReader::read,
    )

    private fun currentPresentDeviceIds(): Set<DacDeviceId> = buildSet {
        if (blackPearlTransport.present.value) add(DacDeviceId.TRN_BLACK_PEARL)
        if (fiioJa11Transport.present.value) add(DacDeviceId.FIIO_JA11)
    }

    private suspend fun readVerifiedSnapshot(
        generation: () -> Long,
        isConnected: () -> Boolean,
        read: suspend (Long) -> HardwareEqSnapshotBundle?,
    ): HardwareEqSnapshotBundle? {
        val expectedGeneration = generation()
        if (expectedGeneration <= 0L || !isConnected()) return null
        val snapshot = read(expectedGeneration) ?: return null
        if (!isConnected() || generation() != expectedGeneration) return null
        return snapshot
    }

    override fun close() {
        scope.cancel()
        blackPearlTransport.close()
        fiioJa11Transport.close()
    }
}
