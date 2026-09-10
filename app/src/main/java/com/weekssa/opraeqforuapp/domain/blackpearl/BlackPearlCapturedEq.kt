package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqCaptureMetadata
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation

data class BlackPearlCapturedEqDraft(
    val productId: String,
    val manufacturer: String,
    val model: String,
    val profile: OpraEqProfile,
    val captureMetadata: SavedEqCaptureMetadata,
)

/**
 * Pure canonicalization of one verified Black Pearl native snapshot into a local Personal EQ.
 * Ordinary Black Pearl playback/global gain is deliberately excluded from source preamp.
 */
fun buildBlackPearlCapturedEqDraft(
    captureId: String,
    snapshotBundle: HardwareEqSnapshotBundle,
    association: SavedEqHeadphoneAssociation?,
): BlackPearlCapturedEqDraft {
    require(captureId.isNotBlank()) { "Capture ID is required." }
    val snapshot = snapshotBundle.snapshot
    require(snapshot.deviceId == DacDeviceId.TRN_BLACK_PEARL) {
        "Only a verified TRN Black Pearl snapshot can be captured by this path."
    }
    require(snapshot.filters.size == BlackPearlProtocol.BAND_COUNT) {
        "The verified Black Pearl snapshot must contain all ${BlackPearlProtocol.BAND_COUNT} bands."
    }
    require(snapshot.filters.map { it.index }.sorted() == (0 until BlackPearlProtocol.BAND_COUNT).toList()) {
        "The verified Black Pearl snapshot has an incomplete or inconsistent band layout."
    }
    require(snapshot.filters.all { it.enabled }) {
        "This hardware EQ contains disabled bands that cannot yet be represented faithfully as a Personal EQ."
    }
    require(snapshot.filters.all { it.type in SUPPORTED_CAPTURE_TYPES }) {
        "This hardware EQ contains a filter type that cannot yet be represented faithfully as a Personal EQ."
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
            append("Captured from TRN Black Pearl")
            snapshot.activeSlot?.let { slot -> append(" · Slot $slot") }
        },
        link = null,
        profileType = "parametric_eq",
        // Black Pearl 0x03 is ordinary playback/global gain. Only a dedicated EQ preamp, if one
        // exists in a future qualified model, may populate source preamp here.
        preampGainDb = snapshot.dedicatedEqPreampDb,
        bands = snapshot.filters.sortedBy { it.index }.map { filter ->
            OpraBand(
                type = when (filter.type) {
                    EqFilterType.PEAK -> "peak_dip"
                    EqFilterType.LOW_SHELF -> "low_shelf"
                    EqFilterType.HIGH_SHELF -> "high_shelf"
                    else -> error("unsupported captured EQ filter")
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
    return BlackPearlCapturedEqDraft(
        productId = productId,
        manufacturer = association?.manufacturer.orEmpty(),
        model = association?.model.orEmpty(),
        profile = profile,
        captureMetadata = metadata,
    )
}

private val SUPPORTED_CAPTURE_TYPES = setOf(
    EqFilterType.PEAK,
    EqFilterType.LOW_SHELF,
    EqFilterType.HIGH_SHELF,
)
