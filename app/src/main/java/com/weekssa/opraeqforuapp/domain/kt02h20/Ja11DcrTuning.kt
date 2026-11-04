package com.weekssa.opraeqforuapp.domain.kt02h20

/**
 * The DCR parameter the community JA11 firmware modifies, and the two known 32-bit values.
 *
 * Source evidence: official JadeAudio JA11 V2.2 versus the community "Ellyn_GP" image differ at
 * image offsets 0x116C/0x10E8/0x10EC. The image-to-register mapping (0x11C4 + (n - 0x71) * 4) was
 * confirmed against live reads on the tested unit, which held the official values.
 */
object Ja11DcrTuning {
    data class Parameter(val register: Int, val stock: Int, val ellyn: Int)

    const val REGISTER = 0x3A
    const val STOCK: Int = 0x00000004
    const val ELLYN: Int = 0x00000000

    /**
     * The complete community tuning deltas from official V2.2 to Ellyn_GP.
     * Registers 0x3A/0x3B carry the noise-gate side; 0x5B carries DCR.
     */
    val parameters: List<Parameter> = listOf(
        Parameter(register = 0x3A, stock = 0x00000004, ellyn = 0x00000000),
        Parameter(register = 0x3B, stock = 0x0000000D, ellyn = 0x0000000C),
    )

    /**
     * USB identity registers. Register 0x5B carries VID 0x2972 / PID 0x0102 and must never be
     * written by this app: the community image changes it to VID 0x31B2 / PID 0x2003, which makes
     * the DAC stop matching the FiiO identity and can persist across power cycles.
     */
    const val USB_IDENTITY_REGISTER = 0x5B

    fun canWriteRegister(address: Int): Boolean = address != USB_IDENTITY_REGISTER

    fun isStock(value: Int?): Boolean = value == STOCK

    fun isEllyn(value: Int?): Boolean = value == ELLYN
}
