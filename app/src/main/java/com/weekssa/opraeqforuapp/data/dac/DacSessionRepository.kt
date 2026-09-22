package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidEw300UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Session-sticky reconnect policy shared by current and future supported DAC transports.
 *
 * Merely detecting a supported USB identity never opens a session. Automatic reconnect is armed
 * only after that exact DAC has connected successfully once during the current ViewModel lifetime.
 * A later physical reattach may then reopen the same app-owned session path without requiring a
 * second Connect tap. Connecting/error states never loop or auto-retry.
 */
internal class DacReconnectPolicy {
    private var hasConnectedSuccessfully = false

    fun shouldReconnect(
        isPresent: Boolean,
        isConnected: Boolean,
        isDisconnected: Boolean,
    ): Boolean {
        if (isConnected) {
            hasConnectedSuccessfully = true
            return false
        }
        return hasConnectedSuccessfully && isPresent && isDisconnected
    }
}

/**
 * ViewModel-scoped owner/coordinator for current-product physical USB sessions.
 *
 * Current My DAC recognition and session ownership are intentionally limited to TRN Black Pearl,
 * FiiO JA11, and SIMGOT EW300. Historical JCALLY protocol knowledge may remain in
 * research/reference files, but no JCALLY transport is opened or owned by the current product.
 *
 * Per-device operation locks live here, at the physical-session owner, so EQ and DEVICE repositories
 * can share one serialization boundary rather than each believing it exclusively owns the same HID
 * session.
 */
class DacSessionRepository(
    internal val blackPearlTransport: AndroidBlackPearlUsbTransport,
    internal val fiioJa11Transport: AndroidFiioJa11UsbTransport,
    internal val ew300Transport: AndroidEw300UsbTransport,
    private val ew300ReconnectGate: Ew300ReconnectGate = ew300Transport.reconnectGate,
) : Closeable {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> = blackPearlTransport.state
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> = fiioJa11Transport.state
    val ew300ConnectionState: StateFlow<Kt02h20ConnectionState> = ew300Transport.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blackPearlOperationMutex = Mutex()
    private val fiioJa11OperationMutex = Mutex()
    private val ew300OperationMutex = Mutex()
    private val ew300MutationExecutor = Ew300MutationExecutor(
        scope = scope,
        operationMutex = ew300OperationMutex,
        reconnectGate = ew300ReconnectGate,
    )
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
                ew300Transport.present,
            ) { blackPearlPresent, fiioJa11Present, ew300Present ->
                buildSet {
                    if (blackPearlPresent) add(DacDeviceId.TRN_BLACK_PEARL)
                    if (fiioJa11Present) add(DacDeviceId.FIIO_JA11)
                    if (ew300Present) add(DacDeviceId.SIMGOT_EW300)
                }
            }.collect { presentDeviceIds ->
                mutableRecognitionState.update { previous -> previous.withPresentDevices(presentDeviceIds) }
            }
        }

        observeAutomaticReconnect(
            present = blackPearlTransport.present,
            connectionState = blackPearlConnectionState,
            isConnected = { state -> state is BlackPearlConnectionState.Connected },
            isDisconnected = { state -> state is BlackPearlConnectionState.Disconnected },
            connect = blackPearlTransport::connect,
        )
        observeAutomaticReconnect(
            present = fiioJa11Transport.present,
            connectionState = fiioJa11ConnectionState,
            isConnected = { state -> state is Kt02h20ConnectionState.Connected },
            isDisconnected = { state -> state is Kt02h20ConnectionState.Disconnected },
            connect = fiioJa11Transport::connect,
        )
        observeAutomaticReconnect(
            present = ew300Transport.present,
            connectionState = ew300ConnectionState,
            isConnected = { state -> state is Kt02h20ConnectionState.Connected },
            isDisconnected = { state -> state is Kt02h20ConnectionState.Disconnected },
            connect = ew300Transport::connectAutomatically,
            allowReconnect = ew300ReconnectGate::canAutomaticReconnect,
            allowReconnectState = ew300ReconnectGate.automaticReconnectAllowed,
        )
    }

    fun connectBlackPearl() = blackPearlTransport.connect()
    fun connectFiioJa11() = fiioJa11Transport.connect()
    fun connectEw300() {
        ew300ReconnectGate.beginManualConnect()
        ew300Transport.connect()
    }

    suspend fun <T> withExclusiveBlackPearlOperation(block: suspend () -> T): T =
        blackPearlOperationMutex.withLock { block() }

    suspend fun <T> withExclusiveFiioJa11Operation(block: suspend () -> T): T =
        fiioJa11OperationMutex.withLock { block() }

    suspend fun <T> withExclusiveEw300Operation(block: suspend () -> T): T =
        ew300OperationMutex.withLock { block() }

    /**
     * Hardware mutation execution belongs to this session owner, not to the calling Compose scope.
     * Cancelling a UI waiter therefore cannot abort an in-flight Save/replacement verification.
     */
    suspend fun <T> withExclusiveEw300Mutation(block: suspend () -> T): T =
        ew300MutationExecutor.execute(block)

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            blackPearlConnectionState.value is BlackPearlConnectionState.Connected &&
            blackPearlTransport.sessionGeneration == sessionGeneration

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            fiioJa11Transport.sessionGeneration == sessionGeneration

    fun isEw300SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            ew300ConnectionState.value is Kt02h20ConnectionState.Connected &&
            ew300Transport.sessionGeneration == sessionGeneration

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

    suspend fun readEw300Snapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = { ew300Transport.sessionGeneration },
        isConnected = { ew300ConnectionState.value is Kt02h20ConnectionState.Connected },
        read = { generation ->
            val bands = (0 until Ew300Protocol.BAND_COUNT).map { index ->
                val gain = ew300Transport.readRegister(Ew300Protocol.bandRegister(index))
                val q = ew300Transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
                if (gain == null || q == null) null else Ew300Protocol.decodeBand(index, gain, q)
            }
            val gain = ew300Transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            if (bands.any { it == null } || gain == null) {
                null
            } else {
                HardwareEqSnapshotFactory.ew300(
                    nativeBands = bands.filterNotNull(),
                    globalGainDb = Ew300Protocol.globalGainDb(gain),
                    sessionGeneration = generation,
                    verifiedAtEpochMillis = System.currentTimeMillis(),
                )
            }
        },
    )

    private fun currentPresentDeviceIds(): Set<DacDeviceId> = buildSet {
        if (blackPearlTransport.present.value) add(DacDeviceId.TRN_BLACK_PEARL)
        if (fiioJa11Transport.present.value) add(DacDeviceId.FIIO_JA11)
        if (ew300Transport.present.value) add(DacDeviceId.SIMGOT_EW300)
    }

    private fun <T> observeAutomaticReconnect(
        present: StateFlow<Boolean>,
        connectionState: StateFlow<T>,
        isConnected: (T) -> Boolean,
        isDisconnected: (T) -> Boolean,
        connect: () -> Unit,
        allowReconnect: () -> Boolean = { true },
        allowReconnectState: StateFlow<Boolean>? = null,
    ) {
        scope.launch {
            val policy = DacReconnectPolicy()
            val allowed = allowReconnectState ?: flowOf(true)
            combine(connectionState, present, allowed) { state, isPresent, canReconnect ->
                Triple(state, isPresent, canReconnect)
            }.collect { (state, isPresent, canReconnect) ->
                if (
                    policy.shouldReconnect(
                        isPresent = isPresent,
                        isConnected = isConnected(state),
                        isDisconnected = isDisconnected(state),
                    ) && canReconnect && allowReconnect()
                ) {
                    connect()
                }
            }
        }
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
        ew300Transport.close()
    }
}
