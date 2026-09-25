package com.weekssa.opraeqforuapp.domain.ew300

/** Result of one absolute EW300 playback-gain transaction. */
sealed interface Ew300PlaybackGainResult {
    data class Success(val playbackGainDb: Double) : Ew300PlaybackGainResult
    data class NotSuitable(val reason: String) : Ew300PlaybackGainResult
    data class DeviceUnavailable(val reason: String) : Ew300PlaybackGainResult
    data class TransferFailed(val reason: String) : Ew300PlaybackGainResult
    data class VerificationFailed(val reason: String) : Ew300PlaybackGainResult
}
