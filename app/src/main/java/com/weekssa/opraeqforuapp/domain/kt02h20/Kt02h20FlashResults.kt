package com.weekssa.opraeqforuapp.domain.kt02h20

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
