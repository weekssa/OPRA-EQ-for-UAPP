package com.weekssa.opraeqforuapp.data.hardware

import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlatResetResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository boundary for deterministic hardware-EQ transactions.
 *
 * v0.6 moves physical USB-session lifecycle/connection ownership to [DacSessionRepository]. This
 * repository deliberately keeps the existing protocol flashers and delegates connection state and
 * connect requests to the shared session owner so qualified v0.5 Flash/Reset behavior is preserved
 * while future generic DAC controls can share the same physical sessions safely.
 */
class HardwareEqRepository(
    private val dacSessionRepository: DacSessionRepository,
    private val blackPearlFlasher: BlackPearlFlasher,
    private val fiioJa11Flasher: FiioJa11Flasher,
    private val jcallyJm12Flasher: JcallyJm12Flasher,
) {
    val blackPearlConnectionState: StateFlow<BlackPearlConnectionState> =
        dacSessionRepository.blackPearlConnectionState
    val fiioJa11ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.fiioJa11ConnectionState
    val jcallyJm12ConnectionState: StateFlow<Kt02h20ConnectionState> =
        dacSessionRepository.jcallyJm12ConnectionState

    fun connectBlackPearl() = dacSessionRepository.connectBlackPearl()

    fun connectFiioJa11() = dacSessionRepository.connectFiioJa11()

    fun connectJcallyJm12() = dacSessionRepository.connectJcallyJm12()

    suspend fun flashBlackPearl(profile: OpraEqProfile): BlackPearlFlashResult =
        blackPearlFlasher.flash(profile)

    suspend fun resetBlackPearl(): BlackPearlFlatResetResult = blackPearlFlasher.resetToFlat()

    suspend fun flashFiioJa11(profile: OpraEqProfile): Kt02h20FlashResult = fiioJa11Flasher.flash(profile)

    suspend fun resetFiioJa11(): Kt02h20FlatResetResult = fiioJa11Flasher.resetToFlat()

    suspend fun flashJcallyJm12(profile: OpraEqProfile): Kt02h20FlashResult = jcallyJm12Flasher.flash(profile)

    suspend fun resetJcallyJm12(): Kt02h20FlatResetResult = jcallyJm12Flasher.resetToFlat()
}
