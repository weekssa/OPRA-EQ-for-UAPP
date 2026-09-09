package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle

/** Presentation-only lifecycle around a verified native hardware EQ snapshot. */
data class HardwareEqSnapshotUiState(
    val bundle: HardwareEqSnapshotBundle? = null,
    val freshness: DacStateFreshness? = null,
    val isReading: Boolean = false,
    val readFailed: Boolean = false,
) {
    init {
        require(bundle != null || freshness == null) { "A missing snapshot cannot have freshness." }
        require(bundle == null || freshness != null) { "A retained snapshot must declare freshness." }
    }

    fun beginRead(): HardwareEqSnapshotUiState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = true,
        readFailed = false,
    )

    fun publishCurrent(snapshot: HardwareEqSnapshotBundle): HardwareEqSnapshotUiState =
        HardwareEqSnapshotUiState(
            bundle = snapshot,
            freshness = DacStateFreshness.CURRENT,
            isReading = false,
            readFailed = false,
        )

    fun markReadFailed(): HardwareEqSnapshotUiState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = false,
        readFailed = true,
    )

    fun markStale(): HardwareEqSnapshotUiState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = false,
    )
}
