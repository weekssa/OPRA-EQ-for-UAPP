package com.weekssa.opraeqforuapp.domain.ew300

/** Exact UAC1 descriptor facts relevant to sample rate, microphone mute, and UAC mode. */
data class Ew300UacFeatureUnit(
    val unitId: Int,
    val sourceId: Int,
    val sourceTerminalType: Int?,
    val muteSupported: Boolean,
    val volumeSupported: Boolean,
)

data class Ew300UacStream(
    val interfaceNumber: Int,
    val alternateSetting: Int,
    val endpointAddress: Int?,
    val terminalLink: Int?,
    val formatTag: Int?,
    val channels: Int?,
    val bitResolution: Int?,
    val sampleRates: List<Int>,
    val continuousRange: IntRange?,
) {
    val hasSamplingFrequencyControl: Boolean
        get() = sampleRates.isNotEmpty() || continuousRange != null
}

data class Ew300UacDescriptor(
    val uacVersion: String,
    val audioControlInterface: Int?,
    val streams: List<Ew300UacStream>,
    val featureUnits: List<Ew300UacFeatureUnit>,
    val microphoneFeature: Ew300UacFeatureUnit?,
    val headsetFeature: Ew300UacFeatureUnit?,
) {
    val hasMultipleUacModes: Boolean
        get() = uacVersion == "UAC 2.0"
}

private fun ByteArray.u8(at: Int): Int = this[at].toInt() and 0xFF
private fun ByteArray.le16(at: Int): Int = u8(at) or (u8(at + 1) shl 8)
private fun ByteArray.le24(at: Int): Int = u8(at) or (u8(at + 1) shl 8) or (u8(at + 2) shl 16)

object Ew300UacDescriptorParser {
    private const val TYPE_CONFIGURATION = 2
    private const val TYPE_INTERFACE = 4
    private const val TYPE_ENDPOINT = 5
    private const val TYPE_CLASS_SPECIFIC_INTERFACE = 0x24

    private const val AUDIO_CLASS = 1
    private const val AUDIO_CONTROL_SUBCLASS = 1
    private const val AUDIO_STREAMING_SUBCLASS = 2
    private const val UAC2_AUDIO_CONTROL_SUBCLASS = 0x20

    private const val AC_INPUT_TERMINAL = 2
    private const val AC_FEATURE_UNIT = 6
    private const val AS_GENERAL = 1
    private const val AS_FORMAT_TYPE = 2

    const val TERMINAL_MICROPHONE = 0x0201
    const val TERMINAL_HEADSET = 0x0402

    fun parse(raw: ByteArray): Ew300UacDescriptor? {
        if (raw.size < 9) return null
        var uacVersion = "Unknown"
        var audioControlInterface: Int? = null
        var currentInterface: Int? = null
        var currentSubclass: Int? = null
        var currentAlternate = 0
        val inputTerminals = mutableMapOf<Int, Int>()
        val features = mutableListOf<Ew300UacFeatureUnit>()
        val streams = linkedMapOf<Int, MutableStream>()

        var offset = 0
        while (offset + 2 <= raw.size) {
            val length = raw.u8(offset)
            if (length < 2 || offset + length > raw.size) break
            when (val type = raw.u8(offset + 1)) {
                TYPE_CONFIGURATION -> Unit
                TYPE_INTERFACE -> {
                    if (length < 9) return null
                    currentInterface = raw.u8(offset + 2)
                    currentAlternate = raw.u8(offset + 3)
                    val interfaceClass = raw.u8(offset + 5)
                    val interfaceSubclass = raw.u8(offset + 6)
                    currentSubclass = interfaceSubclass
                    if (interfaceClass == AUDIO_CLASS) {
                        when (interfaceSubclass) {
                            AUDIO_CONTROL_SUBCLASS -> {
                                uacVersion = "UAC 1.0"
                                audioControlInterface = currentInterface
                            }
                            UAC2_AUDIO_CONTROL_SUBCLASS -> {
                                uacVersion = "UAC 2.0"
                                audioControlInterface = currentInterface
                            }
                            AUDIO_STREAMING_SUBCLASS -> {
                                val existing = streams[currentInterface]
                                if (existing == null) {
                                    streams[currentInterface] = MutableStream(currentInterface, currentAlternate)
                                } else if (currentAlternate > existing.alternateSetting) {
                                    existing.alternateSetting = currentAlternate
                                }
                            }
                        }
                    }
                }
                TYPE_CLASS_SPECIFIC_INTERFACE -> {
                    if (length < 3) break
                    val subtype = raw.u8(offset + 2)
                    val subclass = currentSubclass
                    if (subclass == AUDIO_CONTROL_SUBCLASS) {
                        when (subtype) {
                            AC_INPUT_TERMINAL -> if (length >= 12) {
                                inputTerminals[raw.u8(offset + 3)] = raw.le16(offset + 4)
                            }
                            AC_FEATURE_UNIT -> if (length >= 7) {
                                val unitId = raw.u8(offset + 3)
                                val sourceId = raw.u8(offset + 4)
                                val controlSize = raw.u8(offset + 5)
                                if (controlSize in 1..4 && offset + 6 + controlSize <= offset + length) {
                                    val master = raw.u8(offset + 6)
                                    features += Ew300UacFeatureUnit(
                                        unitId = unitId,
                                        sourceId = sourceId,
                                        sourceTerminalType = null,
                                        muteSupported = master and 0x01 != 0,
                                        volumeSupported = master and 0x02 != 0,
                                    )
                                }
                            }
                        }
                    } else if (subclass == AUDIO_STREAMING_SUBCLASS && currentInterface != null) {
                        val stream = streams.getOrPut(currentInterface) {
                            MutableStream(currentInterface, currentAlternate)
                        }
                        when (subtype) {
                            AS_GENERAL -> if (length >= 7) {
                                stream.terminalLink = raw.u8(offset + 3)
                                stream.formatTag = raw.le16(offset + 5)
                            }
                            AS_FORMAT_TYPE -> if (length >= 8) {
                                val sampleRateType = raw.u8(offset + 7)
                                stream.channels = raw.u8(offset + 4)
                                stream.bitResolution = raw.u8(offset + 6)
                                if (sampleRateType > 0) {
                                    val rates = mutableListOf<Int>()
                                    repeat(sampleRateType) { index ->
                                        val at = offset + 8 + index * 3
                                        if (at + 3 <= offset + length) rates += raw.le24(at)
                                    }
                                    stream.sampleRates = rates
                                } else if (length >= 17) {
                                    val minimum = raw.le24(offset + 8)
                                    val maximum = raw.le24(offset + 11)
                                    if (maximum >= minimum) stream.continuousRange = minimum..maximum
                                }
                            }
                        }
                    }
                }
                TYPE_ENDPOINT -> if (length >= 7) {
                    val address = raw.u8(offset + 2)
                    val subclass = currentSubclass
                    if (subclass == AUDIO_STREAMING_SUBCLASS && currentInterface != null) {
                        streams.getOrPut(currentInterface) {
                            MutableStream(currentInterface, currentAlternate)
                        }.endpointAddress = address
                    }
                }
            }
            offset += length
        }

        val resolvedFeatures = features.map { feature ->
            feature.copy(sourceTerminalType = inputTerminals[feature.sourceId])
        }
        val microphone = resolvedFeatures.firstOrNull {
            it.sourceTerminalType == TERMINAL_MICROPHONE && it.muteSupported
        }
        val headset = resolvedFeatures.firstOrNull {
            it.sourceTerminalType == TERMINAL_HEADSET
        }
        return Ew300UacDescriptor(
            uacVersion = uacVersion,
            audioControlInterface = audioControlInterface,
            streams = streams.values.map { it.freeze() },
            featureUnits = resolvedFeatures,
            microphoneFeature = microphone,
            headsetFeature = headset,
        )
    }

    private data class MutableStream(
        val interfaceNumber: Int,
        var alternateSetting: Int,
        var endpointAddress: Int? = null,
        var terminalLink: Int? = null,
        var formatTag: Int? = null,
        var channels: Int? = null,
        var bitResolution: Int? = null,
        var sampleRates: List<Int> = emptyList(),
        var continuousRange: IntRange? = null,
    ) {
        fun freeze(): Ew300UacStream = Ew300UacStream(
            interfaceNumber = interfaceNumber,
            alternateSetting = alternateSetting,
            endpointAddress = endpointAddress,
            terminalLink = terminalLink,
            formatTag = formatTag,
            channels = channels,
            bitResolution = bitResolution,
            sampleRates = sampleRates,
            continuousRange = continuousRange,
        )
    }
}

data class Ew300UacRequest(
    val requestType: Int,
    val request: Int,
    val value: Int,
    val index: Int,
    val length: Int,
    val data: ByteArray? = null,
)

object Ew300UacRequests {
    const val GET_CUR = 0x81
    const val SET_CUR = 0x01
    const val SAMPLING_FREQ_CONTROL = 0x0100
    const val MUTE_CONTROL = 0x0100

    fun currentSampleRateEndpoint(stream: Ew300UacStream): Ew300UacRequest? {
        val endpoint = stream.endpointAddress ?: return null
        return Ew300UacRequest(
            requestType = 0xA2,
            request = GET_CUR,
            value = SAMPLING_FREQ_CONTROL,
            // UAC1 endpoint requests put the endpoint address in the low byte and zero in the
            // high byte. The interface number is not part of this wIndex.
            index = endpoint,
            length = 3,
        )
    }

    fun microphoneMuteGet(unitId: Int, audioControlInterface: Int): Ew300UacRequest = Ew300UacRequest(
        requestType = 0xA1,
        request = GET_CUR,
        value = MUTE_CONTROL,
        index = (unitId shl 8) or audioControlInterface,
        length = 1,
    )

    fun microphoneMuteSet(
        unitId: Int,
        audioControlInterface: Int,
        muted: Boolean,
    ): Ew300UacRequest = Ew300UacRequest(
        requestType = 0x21,
        request = SET_CUR,
        value = MUTE_CONTROL,
        index = (unitId shl 8) or audioControlInterface,
        length = 1,
        data = byteArrayOf(if (muted) 1 else 0),
    )
}

data class Ew300UacStatus(
    val uacVersion: String,
    val currentSampleRateHz: Int?,
    val sampleRatePath: String?,
    val microphoneMuteSupported: Boolean,
    val microphoneMuted: Boolean?,
    val headsetFeaturePresent: Boolean,
    val headsetRemoteSwitchSupported: Boolean = false,
    val uacModeSwitchSupported: Boolean = false,
    val uacModeLabel: String = uacVersion,
    val descriptorSummary: String = "",
    val blockedReasons: List<String> = emptyList(),
)

