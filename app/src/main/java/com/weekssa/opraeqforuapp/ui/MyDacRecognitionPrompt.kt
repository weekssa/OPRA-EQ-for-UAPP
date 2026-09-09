package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId

/**
 * Returns the first supported DAC that became physically present since the previous observation.
 *
 * The caller owns presentation and records the current set before showing any prompt so a Snackbar
 * cancellation/recomposition cannot manufacture a repeated detection event.
 */
internal fun newlyPresentDac(
    previousPresentDeviceNames: Collection<String>,
    currentPresentDeviceIds: Set<DacDeviceId>,
): DacDeviceId? = currentPresentDeviceIds
    .asSequence()
    .filterNot { deviceId -> deviceId.name in previousPresentDeviceNames }
    .minByOrNull(DacDeviceId::ordinal)
