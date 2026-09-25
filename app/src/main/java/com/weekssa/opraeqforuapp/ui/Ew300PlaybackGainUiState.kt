package com.weekssa.opraeqforuapp.ui

/** UI-only state for the verified EW300 DEVICE playback-gain transaction. */
data class Ew300PlaybackGainUiState(
    val isWriting: Boolean = false,
    val lastVerifiedGainDb: Double? = null,
    val error: String? = null,
) {
    fun beginWrite(): Ew300PlaybackGainUiState = copy(
        isWriting = true,
        error = null,
    )

    fun verified(gainDb: Double): Ew300PlaybackGainUiState = copy(
        isWriting = false,
        lastVerifiedGainDb = gainDb,
        error = null,
    )

    fun failure(message: String): Ew300PlaybackGainUiState = copy(
        isWriting = false,
        error = message,
    )

    fun markStale(): Ew300PlaybackGainUiState = copy(
        isWriting = false,
        error = null,
    )
}
