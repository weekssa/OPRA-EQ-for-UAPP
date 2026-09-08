package com.weekssa.opraeqforuapp.data.hardware

import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidJcallyJm12UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlatResetResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository boundary for the three explicitly supported Direct Flash transports.
 *
 * The Android USB data sources are process-safe because they retain only application Context. The
 * repository itself receives no Context and is owned by the screen ViewModel across configuration
 * changes, preventing a recreated Activity from accidentally replacing an active USB session.
 */
class HardwareEqRepository(
    private val blackPearlTransport: AndroidBlackPearlUsbTransport,
    private val blackPearlFlasher: BlackPearlFlasher,
    private val fiioJa11Transport: AndroidFiioJa11UsbTransport,
    private val fiioJa11Flasher: FiioJa11Flasher,
    private val jcallyJm12Transport: AndroidJcallyJm12UsbTransport,
    private val jcallyJm12Flasher: JcallyJm12Flasher,
) : Closeable {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> = blackPearlTransport.state
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> = fiioJa11Transport.state
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> = jcallyJm12Transport.state

    fun connectBlackPearl() = blackPearlTransport.connect()

    fun connectFiioJa11() = fiioJa11Transport.connect()

    fun connectJcallyJm12() = jcallyJm12Transport.connect()

    suspend fun flashBlackPearl(profile: OpraEqProfile): BlackPearlFlashResult =
        blackPearlFlasher.flash(profile)

    suspend fun resetBlackPearl(): BlackPearlFlatResetResult = blackPearlFlasher.resetToFlat()

    suspend fun flashFiioJa11(profile: OpraEqProfile): Kt02h20FlashResult = fiioJa11Flasher.flash(profile)

    suspend fun resetFiioJa11(): Kt02h20FlatResetResult = fiioJa11Flasher.resetToFlat()

    suspend fun flashJcallyJm12(profile: OpraEqProfile): Kt02h20FlashResult = jcallyJm12Flasher.flash(profile)

    suspend fun resetJcallyJm12(): Kt02h20FlatResetResult = jcallyJm12Flasher.resetToFlat()

    override fun close() {
        blackPearlTransport.close()
        fiioJa11Transport.close()
        jcallyJm12Transport.close()
    }
}
