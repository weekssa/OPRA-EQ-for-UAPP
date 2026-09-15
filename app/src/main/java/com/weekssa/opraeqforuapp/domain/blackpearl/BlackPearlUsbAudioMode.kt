package com.weekssa.opraeqforuapp.domain.blackpearl

/** USB Audio Class mode reported by the Black Pearl's current USB descriptors. */
enum class BlackPearlUsbAudioMode {
    UAC_1_0,
    UAC_2_0,
}

/**
 * Parses the active USB AudioControl class revision from a raw USB descriptor stream.
 *
 * This is deliberately independent from any speculative vendor HID command. The current USB mode
 * can therefore be established from standard descriptors after enumeration without guessing a
 * Black Pearl register. Malformed, unsupported, or conflicting descriptor streams return null.
 */
object BlackPearlUsbAudioDescriptorParser {
    private const val USB_DT_INTERFACE = 0x04
    private const val USB_DT_CS_INTERFACE = 0x24
    private const val USB_CLASS_AUDIO = 0x01
    private const val AUDIO_SUBCLASS_CONTROL = 0x01
    private const val AUDIO_CS_HEADER_SUBTYPE = 0x01
    private const val UAC_1_BCD_ADC = 0x0100
    private const val UAC_2_BCD_ADC = 0x0200

    fun parse(rawDescriptors: ByteArray): BlackPearlUsbAudioMode? {
        var offset = 0
        var inAudioControlInterface = false
        var discovered: BlackPearlUsbAudioMode? = null

        while (offset + 1 < rawDescriptors.size) {
            val length = rawDescriptors[offset].u8()
            val type = rawDescriptors[offset + 1].u8()
            if (length < 2 || offset + length > rawDescriptors.size) return null

            when (type) {
                USB_DT_INTERFACE -> {
                    if (length < 9) return null
                    val interfaceClass = rawDescriptors[offset + 5].u8()
                    val interfaceSubclass = rawDescriptors[offset + 6].u8()
                    inAudioControlInterface =
                        interfaceClass == USB_CLASS_AUDIO && interfaceSubclass == AUDIO_SUBCLASS_CONTROL
                }

                USB_DT_CS_INTERFACE -> {
                    if (inAudioControlInterface && length >= 5) {
                        val subtype = rawDescriptors[offset + 2].u8()
                        if (subtype == AUDIO_CS_HEADER_SUBTYPE) {
                            val bcdAdc = rawDescriptors[offset + 3].u8() or
                                (rawDescriptors[offset + 4].u8() shl 8)
                            val mode = when (bcdAdc) {
                                UAC_1_BCD_ADC -> BlackPearlUsbAudioMode.UAC_1_0
                                UAC_2_BCD_ADC -> BlackPearlUsbAudioMode.UAC_2_0
                                else -> null
                            }
                            if (mode != null) {
                                if (discovered != null && discovered != mode) return null
                                discovered = mode
                            }
                        }
                    }
                }
            }

            offset += length
        }

        return discovered
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
}
