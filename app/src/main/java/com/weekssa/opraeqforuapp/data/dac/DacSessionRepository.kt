package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidJcallyJm12UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel-scoped owner/coordinator for the physical USB sessions of supported DACs.
 *
 * This repository intentionally owns lifecycle, connection requests, and connection-state exposure
 * only. Protocol-specific EQ transactions remain in their existing flashers/repositories so the
 * v0.5 qualified behavior can be migrated without rewriting packet logic.
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

    fun connectBlackPearl() = blackPearlTransport.connect()

    fun connectFiioJa11() = fiioJa11Transport.connect()

    fun connectJcallyJm12() = jcallyJm12Transport.connect()

    override fun close() {
        blackPearlTransport.close()
        fiioJa11Transport.close()
        jcallyJm12Transport.close()
    }
}
