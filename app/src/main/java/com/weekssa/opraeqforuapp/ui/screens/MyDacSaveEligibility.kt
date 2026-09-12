package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch

internal fun shouldOfferSaveDacEq(
    match: HardwareEqMatch?,
    freshness: DacStateFreshness?,
    connected: Boolean,
): Boolean {
    if (!connected || freshness != DacStateFreshness.CURRENT) return false
    return when (match) {
        HardwareEqMatch.Unknown,
        is HardwareEqMatch.ModifiedKnown,
        -> true
        HardwareEqMatch.Flat,
        is HardwareEqMatch.Exact,
        is AmbiguousExactHardwareEqMatch,
        null,
        -> false
    }
}
