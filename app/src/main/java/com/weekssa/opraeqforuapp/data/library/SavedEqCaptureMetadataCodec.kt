package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqCaptureMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SavedEqCaptureMetadataCodec(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
    },
) {
    fun encode(metadata: SavedEqCaptureMetadata): String =
        json.encodeToString(metadata.toStored())

    fun decode(encoded: String): SavedEqCaptureMetadata =
        json.decodeFromString<StoredCaptureMetadata>(encoded).toDomain()
}

@Serializable
private data class StoredCaptureMetadata(
    val deviceId: String,
    val activeSlot: Int?,
    val verifiedAtEpochMillis: Long,
    val eqEnabled: Boolean,
    val dedicatedEqPreampUnits: Long?,
    val bands: List<StoredNativeBand>,
)

@Serializable
private data class StoredNativeBand(
    val index: Int,
    val enabled: Boolean,
    val type: String,
    val frequencyUnits: Long,
    val gainUnits: Long,
    val qUnits: Long,
)

private fun SavedEqCaptureMetadata.toStored() = StoredCaptureMetadata(
    deviceId = deviceId.name,
    activeSlot = activeSlot,
    verifiedAtEpochMillis = verifiedAtEpochMillis,
    eqEnabled = nativeFingerprint.eqEnabled,
    dedicatedEqPreampUnits = nativeFingerprint.dedicatedEqPreampUnits,
    bands = nativeFingerprint.bands.map { band ->
        StoredNativeBand(
            index = band.index,
            enabled = band.enabled,
            type = band.type.name,
            frequencyUnits = band.frequencyUnits,
            gainUnits = band.gainUnits,
            qUnits = band.qUnits,
        )
    },
)

private fun StoredCaptureMetadata.toDomain(): SavedEqCaptureMetadata {
    val parsedDeviceId = DacDeviceId.valueOf(deviceId)
    val fingerprint = HardwareEqNativeFingerprint(
        deviceId = parsedDeviceId,
        eqEnabled = eqEnabled,
        bands = bands.map { band ->
            HardwareEqNativeBandFingerprint(
                index = band.index,
                enabled = band.enabled,
                type = EqFilterType.valueOf(band.type),
                frequencyUnits = band.frequencyUnits,
                gainUnits = band.gainUnits,
                qUnits = band.qUnits,
            )
        },
        dedicatedEqPreampUnits = dedicatedEqPreampUnits,
    )
    return SavedEqCaptureMetadata(
        deviceId = parsedDeviceId,
        activeSlot = activeSlot,
        verifiedAtEpochMillis = verifiedAtEpochMillis,
        nativeFingerprint = fingerprint,
    )
}
