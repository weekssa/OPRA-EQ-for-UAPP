package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidJcallyJm12UsbTransport
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

/**
 * ViewModel-scoped owner/coordinator for the physical USB sessions of supported DACs.
 *
 * This repository owns lifecycle, read-only supported-device recognition, connection requests,
 * connection-state exposure, and verified native EQ reads. Recognition is based only on exact
 * supported VID/PID presence; a failed Connect attempt cannot manufacture My DAC visibility.
 * Recognized identities remain session-sticky after detach so navigation does not jump.
 *
 * Each Android transport assigns its monotonically increasing session generation synchronously when
 * the physical USB session opens, before Connected is published. Read results are accepted only
 * while that exact generation remains current.
 *
 * Protocol-specific EQ write transactions remain in their existing flashers/repositories so the
 * v0.5 qualified behavior is preserved while My DAC can inspect the same physical sessions safely.
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
    private val fiioJa11SnapshotReader = FiioJa11SnapshotReader(fiioJa11Transport)
    private val jcallyJm12SnapshotReader = JcallyJm12SnapshotReader(jcallyJm12Transport)

    init {
        scope.launch {
            combine(
                blackPearlTransport.present,
                fiioJa11Transport.present,
                jcallyJm12Transport.present,
            ) { blackPearlPresent, fiioJa11Present, jcallyJm12Present ->
                buildSet {
                    if (blackPearlPresent) add(DacDeviceId.TRN_BLACK_PEARL)
                    if (fiioJa11Present) add(DacDeviceId.FIIO_JA11)
                    if (jcallyJm12Present) add(DacDeviceId.JCALLY_JM12_STOCK)
                }
            }.collect { presentDeviceIds ->
                mutableRecognitionState.update { previous ->
                    previous.withPresentDevices(presentDeviceIds)
                }
            }
        }
    }

    fun connectBlackPearl() = blackPearlTransport.connect()

    fun connectFiioJa11() = fiioJa11Transport.connect()

    fun connectJcallyJm12() = jcallyJm12Transport.connect()

    fun isBlackPearlSessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            blackPearlConnectionState.value is BlackPearlConnectionState.Connected &&
            blackPearlTransport.sessionGeneration == sessionGeneration

    fun isFiioJa11SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            fiioJa11Transport.sessionGeneration == sessionGeneration

    fun isJcallyJm12SessionCurrent(sessionGeneration: Long): Boolean =
        sessionGeneration > 0L &&
            jcallyJm12ConnectionState.value is Kt02h20ConnectionState.Connected &&
            jcallyJm12Transport.sessionGeneration == sessionGeneration

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

    suspend fun readJcallyJm12Snapshot(): HardwareEqSnapshotBundle? = readVerifiedSnapshot(
        generation = { jcallyJm12Transport.sessionGeneration },
        isConnected = { jcallyJm12ConnectionState.value is Kt02h20ConnectionState.Connected },
        read = jcallyJm12SnapshotReader::read,
    )

    private fun currentPresentDeviceIds(): Set<DacDeviceId> = buildSet {
        if (blackPearlTransport.present.value) add(DacDeviceId.TRN_BLACK_PEARL)
        if (fiioJa11Transport.present.value) add(DacDeviceId.FIIO_JA11)
        if (jcallyJm12Transport.present.value) add(DacDeviceId.JCALLY_JM12_STOCK)
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
        jcallyJm12Transport.close()
    }
}
