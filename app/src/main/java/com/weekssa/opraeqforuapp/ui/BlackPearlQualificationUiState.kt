package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.dac.DacControlId

data class BlackPearlQualificationUiState(
    val isReading: Boolean = false,
    val isWriting: Boolean = false,
    val activeWriteControlId: DacControlId? = null,
    val lastVerifiedWriteControlId: DacControlId? = null,
    val snapshot: BlackPearlDeviceQualificationSnapshot? = null,
    val isCurrentSession: Boolean = false,
    val error: String? = null,
) {
    init {
        require(!isCurrentSession || snapshot != null) {
            "A current Black Pearl DEVICE state requires a snapshot."
        }
        require(isWriting == (activeWriteControlId != null)) {
            "A Black Pearl DEVICE write must identify exactly one active control."
        }
    }

    val isBusy: Boolean
        get() = isReading || isWriting

    fun beginRead(): BlackPearlQualificationUiState = copy(
        isReading = true,
        isWriting = false,
        activeWriteControlId = null,
        lastVerifiedWriteControlId = null,
        isCurrentSession = false,
        error = null,
    )

    fun success(snapshot: BlackPearlDeviceQualificationSnapshot): BlackPearlQualificationUiState =
        BlackPearlQualificationUiState(
            snapshot = snapshot,
            isCurrentSession = true,
        )

    fun failure(message: String): BlackPearlQualificationUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        lastVerifiedWriteControlId = null,
        isCurrentSession = false,
        error = message,
    )

    fun beginWrite(controlId: DacControlId): BlackPearlQualificationUiState = copy(
        isReading = false,
        isWriting = true,
        activeWriteControlId = controlId,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun writeVerified(
        controlId: DacControlId,
        snapshot: BlackPearlDeviceQualificationSnapshot,
    ): BlackPearlQualificationUiState = BlackPearlQualificationUiState(
        lastVerifiedWriteControlId = controlId,
        snapshot = snapshot,
        isCurrentSession = true,
    )

    fun writeFailure(
        message: String,
        actualSnapshot: BlackPearlDeviceQualificationSnapshot? = snapshot,
        actualIsCurrent: Boolean = false,
    ): BlackPearlQualificationUiState = BlackPearlQualificationUiState(
        snapshot = actualSnapshot,
        isCurrentSession = actualSnapshot != null && actualIsCurrent,
        error = message,
    )

    fun withSessionCurrent(current: Boolean): BlackPearlQualificationUiState {
        val retainedSnapshotWentStale = snapshot != null && !current
        return when {
            retainedSnapshotWentStale -> copy(
                isReading = false,
                isWriting = false,
                activeWriteControlId = null,
                isCurrentSession = false,
            )
            isReading -> copy(isCurrentSession = false)
            else -> copy(isCurrentSession = snapshot != null && current)
        }
    }
}
