package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.dac.FiioJa11PendingRestartWrite
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsOperationReport
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsEqReadback
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot

data class FiioJa11DeviceUiState(
    val isReading: Boolean = false,
    val isWriting: Boolean = false,
    val isSavingDefaults: Boolean = false,
    val activeWriteControlId: DacControlId? = null,
    val snapshot: FiioJa11DeviceSnapshot? = null,
    val isCurrentSession: Boolean = false,
    val pendingRestartWrite: FiioJa11PendingRestartWrite? = null,
    val lastVerifiedWriteControlId: DacControlId? = null,
    val error: String? = null,
    val defaultsReport: FiioJa11DefaultsOperationReport? = null,
) {
    init {
        require(isWriting == (activeWriteControlId != null))
        require(!isCurrentSession || snapshot != null)
    }

    /**
     * A reconnect-verification transaction is intentionally busy even when no USB transfer is
     * currently in flight. Treating that gap as idle would let a manual refresh overwrite the
     * pending intent before the replacement JA11 session can be verified.
     */
    val isBusy: Boolean
        get() = isReading || isWriting || isSavingDefaults || pendingRestartWrite != null

    fun beginDefaultsSave(): FiioJa11DeviceUiState = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = true, lastVerifiedWriteControlId = null, error = null,
        defaultsReport = null,
    )

    fun defaultsVerified(snapshot: FiioJa11DeviceSnapshot, report: FiioJa11DefaultsOperationReport) = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = false, snapshot = snapshot, isCurrentSession = true,
        pendingRestartWrite = null, lastVerifiedWriteControlId = null, error = null,
        defaultsReport = report,
    )

    fun defaultsFailure(
        message: String,
        report: FiioJa11DefaultsOperationReport? = null,
        lastVerifiedSnapshot: FiioJa11DeviceSnapshot? = snapshot,
    ) = copy(
        isReading = false, isWriting = false, activeWriteControlId = null,
        isSavingDefaults = false, snapshot = lastVerifiedSnapshot, isCurrentSession = false, pendingRestartWrite = null,
        lastVerifiedWriteControlId = null, error = message, defaultsReport = report,
    )

    fun beginRead(): FiioJa11DeviceUiState = copy(
        isReading = true,
        isWriting = false,
        activeWriteControlId = null,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun readSuccess(snapshot: FiioJa11DeviceSnapshot): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        snapshot = snapshot,
        isCurrentSession = true,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun beginWrite(controlId: DacControlId): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = true,
        activeWriteControlId = controlId,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun verified(
        controlId: DacControlId,
        snapshot: FiioJa11DeviceSnapshot,
    ): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        snapshot = snapshot,
        isCurrentSession = true,
        pendingRestartWrite = null,
        lastVerifiedWriteControlId = controlId,
        error = null,
    )

    fun reconnectRequired(pending: FiioJa11PendingRestartWrite): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
        pendingRestartWrite = pending,
        lastVerifiedWriteControlId = null,
        error = null,
    )

    fun failure(message: String): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
        pendingRestartWrite = null,
        lastVerifiedWriteControlId = null,
        error = message,
    )

    fun markStale(): FiioJa11DeviceUiState = copy(
        isReading = false,
        isWriting = false,
        activeWriteControlId = null,
        isCurrentSession = false,
    )

    fun withSessionCurrent(current: Boolean): FiioJa11DeviceUiState = copy(
        isCurrentSession = snapshot != null && current,
    )
}
