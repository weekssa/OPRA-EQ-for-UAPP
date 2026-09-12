package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot

data class BlackPearlQualificationUiState(
    val isReading: Boolean = false,
    val snapshot: BlackPearlDeviceQualificationSnapshot? = null,
    val isCurrentSession: Boolean = false,
    val error: String? = null,
) {
    init {
        require(!isCurrentSession || snapshot != null) {
            "A current qualification state requires a snapshot."
        }
    }

    fun beginRead(): BlackPearlQualificationUiState = copy(
        isReading = true,
        isCurrentSession = false,
        error = null,
    )

    fun success(snapshot: BlackPearlDeviceQualificationSnapshot): BlackPearlQualificationUiState =
        BlackPearlQualificationUiState(
            isReading = false,
            snapshot = snapshot,
            isCurrentSession = true,
        )

    fun failure(message: String): BlackPearlQualificationUiState = copy(
        isReading = false,
        isCurrentSession = false,
        error = message,
    )

    fun withSessionCurrent(current: Boolean): BlackPearlQualificationUiState = copy(
        isCurrentSession = snapshot != null && current,
    )
}
