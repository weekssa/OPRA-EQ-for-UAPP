package com.weekssa.opraeqforuapp.domain.kt02h20

/**
 * The DCR parameter the community JA11 firmware modifies, and the two known 32-bit values.
 *
 * Source evidence: official JadeAudio JA11 V2.2 versus the community "Ellyn_GP" image differ at
 * image offset 0x116C only within the DCR register block. The live register read of 0x6F on the
 * tested unit returned the official value, so this mapping is device-confirmed for that unit.
 */
object Ja11DcrTuning {
    const val REGISTER = 0x6F
    const val STOCK: Int = 0x01022972
    const val ELLYN: Int = 0x200331B2

    fun isStock(value: Int?): Boolean = value == STOCK

    fun isEllyn(value: Int?): Boolean = value == ELLYN
}
