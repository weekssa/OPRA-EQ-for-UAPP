package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle

/** Immutable, coherent JA11 editor baseline captured from one verified User 1 read. */
data class FiioJa11EditorBaseline(
    val deviceFingerprintKey: String,
    val usbProductId: Int,
    val snapshotBundle: HardwareEqSnapshotBundle,
    val detachGeneration: Long = 0L,
) {
    init {
        require(deviceFingerprintKey.isNotBlank()) { "JA11 editor baseline requires an exact identity key." }
        require(usbProductId in FiioJa11Protocol.SUPPORTED_PRODUCT_IDS) {
            "JA11 editor baseline requires a supported USB product identity."
        }
        require(detachGeneration >= 0L)
        require(snapshotBundle.snapshot.deviceId == DacDeviceId.FIIO_JA11)
        require(snapshotBundle.snapshot.activeProgram == FiioJa11Protocol.EqProgram.USER_1)
        require(snapshotBundle.snapshot.filters.size == FiioJa11Protocol.BAND_COUNT)
        require(snapshotBundle.snapshot.dedicatedEqPreampDb != null)
        require(snapshotBundle.fingerprint.eqEnabled)
        require(snapshotBundle.snapshot.verifiedAtEpochMillis >= 0L)
    }

    val sessionGeneration: Long
        get() = snapshotBundle.snapshot.sessionGeneration

    fun sessionToken(): FiioJa11SessionToken = FiioJa11SessionToken(
        deviceFingerprintKey = deviceFingerprintKey,
        usbProductId = usbProductId,
        sessionGeneration = sessionGeneration,
        detachGeneration = detachGeneration,
    )
}

sealed interface Kt02h20FlashResult {
    data class Success(
        val representation: FiveBandRepresentation,
        /** True only when the run-mode protocol has an explicit persist/save command used by us. */
        val explicitPersistenceCommandUsed: Boolean,
    ) : Kt02h20FlashResult

    data class NotSuitable(val reason: String) : Kt02h20FlashResult
    data class DeviceUnavailable(val reason: String) : Kt02h20FlashResult
    data class TransferFailed(val reason: String) : Kt02h20FlashResult
    data class VerificationFailed(val reason: String) : Kt02h20FlashResult
}

sealed interface Kt02h20FlatResetResult {
    data class Success(
        val restoredPlaybackGainDb: Double,
        val explicitPersistenceCommandUsed: Boolean,
    ) : Kt02h20FlatResetResult

    data class DeviceUnavailable(val reason: String) : Kt02h20FlatResetResult
    data class NotSuitable(val reason: String) : Kt02h20FlatResetResult
    data class TransferFailed(val reason: String) : Kt02h20FlatResetResult
    data class VerificationFailed(val reason: String) : Kt02h20FlatResetResult
}

sealed interface FiioJa11EditorApplyResult {
    data object Verified : FiioJa11EditorApplyResult
    data class InvalidPlan(val reason: String) : FiioJa11EditorApplyResult
    data class StaleBaseline(val reason: String) : FiioJa11EditorApplyResult
    data class DeviceUnavailable(val reason: String) : FiioJa11EditorApplyResult
    data class TransferFailed(val reason: String) : FiioJa11EditorApplyResult
    data class VerificationFailed(val reason: String) : FiioJa11EditorApplyResult
}
