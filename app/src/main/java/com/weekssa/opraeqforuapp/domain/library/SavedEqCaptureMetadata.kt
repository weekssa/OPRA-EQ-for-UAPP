package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint

data class SavedEqCaptureMetadata(
    val deviceId: DacDeviceId,
    val activeSlot: Int?,
    val verifiedAtEpochMillis: Long,
    val nativeFingerprint: HardwareEqNativeFingerprint,
) {
    init {
        require(activeSlot == null || activeSlot >= 0) { "Captured hardware slot must not be negative" }
        require(verifiedAtEpochMillis >= 0) { "Captured verification timestamp must not be negative" }
        require(nativeFingerprint.deviceId == deviceId) {
            "Captured native fingerprint must belong to the captured device"
        }
    }
}

data class SavedEqHeadphoneAssociation(
    val productId: String,
    val manufacturer: String,
    val model: String,
) {
    init {
        require(productId.isNotBlank()) { "Associated product ID must not be blank" }
        require(manufacturer.isNotBlank()) { "Associated manufacturer must not be blank" }
        require(model.isNotBlank()) { "Associated model must not be blank" }
    }
}
