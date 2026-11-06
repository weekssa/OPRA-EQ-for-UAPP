package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band

/** Fixed, runtime-only A/B states for the owner shelf listening block. Never persisted. */
enum class Ew300ShelfTestTarget {
    FLAT,
    LOW_SHELF,
    HIGH_SHELF,
    ;

    fun bands(): List<Kt02h20Band> {
        val flat = MutableList(Ew300Protocol.BAND_COUNT) {
            Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
        }
        return when (this) {
            FLAT -> flat
            LOW_SHELF -> flat.also {
                it[0] = Kt02h20Band(
                    type = "low_shelf",
                    frequencyHz = Ew300Protocol.EXPERIMENTAL_LOW_SHELF_HZ,
                    gainDb = -6.0,
                    q = Ew300Protocol.EXPERIMENTAL_SHELF_Q,
                )
            }
            HIGH_SHELF -> flat.also {
                it[0] = Kt02h20Band(
                    type = "high_shelf",
                    frequencyHz = Ew300Protocol.EXPERIMENTAL_HIGH_SHELF_HZ,
                    gainDb = -6.0,
                    q = Ew300Protocol.EXPERIMENTAL_SHELF_Q,
                )
            }
        }
    }
}


