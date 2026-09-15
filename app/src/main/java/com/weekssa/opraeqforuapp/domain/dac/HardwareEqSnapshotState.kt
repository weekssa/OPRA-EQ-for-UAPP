package com.weekssa.opraeqforuapp.domain.dac

/** Lifecycle state around the most recently verified native hardware EQ snapshot. */
data class HardwareEqSnapshotState(
    val bundle: HardwareEqSnapshotBundle? = null,
    val freshness: DacStateFreshness? = null,
    val isReading: Boolean = false,
    val readFailed: Boolean = false,
) {
    init {
        require(bundle != null || freshness == null) { "A missing snapshot cannot have freshness." }
        require(bundle == null || freshness != null) { "A retained snapshot must declare freshness." }
    }

    fun beginRead(): HardwareEqSnapshotState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = true,
        readFailed = false,
    )

    fun publishCurrent(snapshot: HardwareEqSnapshotBundle): HardwareEqSnapshotState =
        HardwareEqSnapshotState(
            bundle = snapshot,
            freshness = DacStateFreshness.CURRENT,
            isReading = false,
            readFailed = false,
        )

    fun markReadFailed(): HardwareEqSnapshotState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = false,
        readFailed = true,
    )

    fun markStale(): HardwareEqSnapshotState = copy(
        freshness = bundle?.let { DacStateFreshness.LAST_READ_STALE },
        isReading = false,
    )
}
