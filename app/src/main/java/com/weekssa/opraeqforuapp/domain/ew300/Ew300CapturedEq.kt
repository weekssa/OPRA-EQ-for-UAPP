package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqCaptureMetadata
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation

data class Ew300CapturedEqDraft(
    val productId: String,
    val manufacturer: String,
    val model: String,
    val profile: OpraEqProfile,
    val captureMetadata: SavedEqCaptureMetadata,
)

/** Canonicalizes one complete, verified EW300 snapshot into a local Personal EQ. */
fun buildEw300CapturedEqDraft(
    captureId: String,
    snapshotBundle: HardwareEqSnapshotBundle,
    association: SavedEqHeadphoneAssociation?,
): Ew300CapturedEqDraft {
    require(captureId.isNotBlank()) { "Capture ID is required." }
    val snapshot = snapshotBundle.snapshot
    require(snapshot.deviceId == DacDeviceId.SIMGOT_EW300) {
        "Only a verified SIMGOT EW300 snapshot can be captured by this path."
    }
    require(snapshot.filters.size == Ew300Protocol.BAND_COUNT) {
        "The verified EW300 snapshot must contain all ${Ew300Protocol.BAND_COUNT} bands."
    }
    require(snapshot.filters.map { it.index }.sorted() == (0 until Ew300Protocol.BAND_COUNT).toList()) {
        "The verified EW300 snapshot has an incomplete band layout."
    }
    require(snapshot.filters.all { it.enabled }) {
        "This EW300 EQ contains disabled bands that cannot yet be represented faithfully."
    }
    require(snapshot.filters.all { it.type in SUPPORTED_CAPTURE_TYPES }) {
        "This EW300 EQ contains an unsupported filter type."
    }
    require(snapshotBundle.fingerprint.deviceId == snapshot.deviceId) {
        "The verified snapshot and native fingerprint belong to different devices."
    }

    val productId = association?.productId ?: "personal-product:$captureId"
    val profile = OpraEqProfile(
        id = "personal-eq:$captureId",
        productId = productId,
        author = "Personal",
        details = buildString {
            append("Captured from SIMGOT EW300 DSP")
            snapshot.activeSlot?.let { slot -> append(" · Slot $slot") }
        },
        link = null,
        profileType = "parametric_eq",
        // EW300 register 0x66 is playback gain. Like Black Pearl playback gain, it is device state
        // rather than a dedicated PEQ preamp and must not be baked into a captured canonical EQ.
        preampGainDb = null,
        bands = snapshot.filters.sortedBy { it.index }.map { filter ->
            OpraBand(
                type = when (filter.type) {
                    EqFilterType.PEAK -> "peak_dip"
                    EqFilterType.LOW_SHELF -> "low_shelf"
                    EqFilterType.HIGH_SHELF -> "high_shelf"
                    else -> error("unsupported captured EW300 EQ filter")
                },
                frequency = filter.frequencyHz,
                gainDb = filter.gainDb,
                q = filter.q,
                slope = null,
            )
        },
    )
    val metadata = SavedEqCaptureMetadata(
        deviceId = snapshot.deviceId,
        activeSlot = snapshot.activeSlot,
        verifiedAtEpochMillis = snapshot.verifiedAtEpochMillis,
        nativeFingerprint = snapshotBundle.fingerprint,
    )
    return Ew300CapturedEqDraft(
        productId = productId,
        manufacturer = association?.manufacturer.orEmpty(),
        model = association?.model.orEmpty(),
        profile = profile,
        captureMetadata = metadata,
    )
}

private val SUPPORTED_CAPTURE_TYPES = setOf(
    EqFilterType.PEAK,
)
