package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidJcallyJm12UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel-scoped owner/coordinator for the physical USB sessions of supported DACs.
 *
 * This repository owns lifecycle, connection requests, connection-state exposure, and monotonically
 * increasing session generations. Protocol-specific EQ write transactions remain in their existing
 * flashers/repositories so the v0.5 qualified behavior is preserved while read-only My DAC state can
 * share the same physical sessions safely.
 *
 * The transports are created once in the manual composition root with application Context and are
 * shared with the existing protocol flashers. No Activity/Compose Context reaches this repository.
 */
class DacSessionRepository(
    internal val blackPearlTransport: AndroidBlackPearlUsbTransport,
    internal val fiioJa11Transport: AndroidFiioJa11UsbTransport,
    internal val jcallyJm12Transport: AndroidJcallyJm12UsbTransport,
) : Closeable {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> = blackPearlTransport.state
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> = fiioJa11Transport.state
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> = jcallyJm12Transport.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blackPearlSessionGeneration = MutableStateFlow(0L)
    private val fiioJa11SessionGeneration = MutableStateFlow(0L)
    private val jcallyJm12SessionGeneration = MutableStateFlow(0L)

    private val blackPearlSnapshotReader = BlackPearlSnapshotReader(
        source = object : BlackPearlSnapshotSource {
            override suspend fun readNativeBand(index: Int) = blackPearlTransport.readNativeBand(index)

            override suspend fun readGlobalGainRaw(): Int? = blackPearlTransport.readGlobalGainRaw()
        },
    )
    private val fiioJa11SnapshotReader = FiioJa11SnapshotReader(fiioJa11Transport)
    private val jcallyJm12SnapshotReader = JcallyJm12SnapshotReader(jcallyJm12Transport)

    init {
        trackSessionGeneration(
            state = blackPearlConnectionState,
            isConnected = { it is BlackPearlConnectionState.Connected },
            generation = blackPearlSessionGeneration,
        )
        trackSessionGeneration(
            state = fiioJa11ConnectionState,
            isConnected = { it is Kt02h20ConnectionState.Connected },
            generation = fiioJa11SessionGeneration,
        )
        trackSessionGeneration(
            state = jcallyJm12ConnectionState,
            isConnected = { it is Kt02h20ConnectionState.Connected },
            generation = jcallyJm12SessionGeneration,
        )
    }

    fun connectBlackPearl() = blackPearlTransport.connect()

    fun connectFiioJa11() = fiioJa11Transport.connect()

    fun connectJcallyJm12() = jcallyJm12Transport.connect()

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            blackPearlConnectionState.value is BlackPearlConnectionState.Connected &&
            blackPearlSessionGeneration.value == sessionGeneration

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            fiioJa11SessionGeneration.value == sessionGeneration

    fun isJcallyJm12SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            jcallyJm12ConnectionState.value is Kt02h20ConnectionState.Connected &&
            jcallyJm12SessionGeneration.value == sessionGeneration

    suspend fun readBlackPearlSnapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = blackPearlSessionGeneration,
        isConnected = { blackPearlConnectionState.value is BlackPearlConnectionState.Connected },
        read = blackPearlSnapshotReader::read,
    )

    suspend fun readFiioJa11Snapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = fiioJa11SessionGeneration,
        isConnected = { fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected },
        read = fiioJa11SnapshotReader::read,
    )

    suspend fun readJcallyJm12Snapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = jcallyJm12SessionGeneration,
        isConnected = { jcallyJm12ConnectionState.value is Kt02h20ConnectionState.Connected },
        read = jcallyJm12SnapshotReader::read,
    )

    private suspend fun readVerifiedSnapshot(
        generation: StateFlow<Long>,
        isConnected: () -> Boolean,
        read: suspend (Long) -> HardwareEqSnapshotBundle?,
    ): HardwareEqSnapshotBundle? {
        val expectedGeneration = generation.value
        if (expectedGeneration <= 0L || !isConnected()) return null
        val snapshot = read(expectedGeneration) ?: return null
        if (!isConnected() || generation.value != expectedGeneration) return null
        return snapshot
    }

    private fun <T> trackSessionGeneration(
        state: StateFlow<T>,
        isConnected: (T) -> Boolean,
        generation: MutableStateFlow<Long>,
    ) {
        scope.launch {
            var previouslyConnected = false
            state.collect { value ->
                val connected = isConnected(value)
                if (connected && !previouslyConnected) {
                    generation.value += 1L
                }
                previouslyConnected = connected
            }
        }
    }

    override fun close() {
        scope.cancel()
        blackPearlTransport.close()
        fiioJa11Transport.close()
        jcallyJm12Transport.close()
    }
}
