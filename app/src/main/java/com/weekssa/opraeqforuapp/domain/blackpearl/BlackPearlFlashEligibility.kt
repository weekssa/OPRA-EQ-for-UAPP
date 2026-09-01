package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

/** Hardware-independent eligibility check for the EQ Library direct-Flash action. */
fun OpraEqProfile.isBlackPearlDirectFlashable(): Boolean =
    buildBlackPearlFlashPlan(this, activeSlot = 0x00) is BlackPearlFlashPlan.Ready

/** Required Black Pearl global playback-gain adjustment, if the PEQ source itself is flashable. */
fun OpraEqProfile.blackPearlRequiredPlaybackGainDb(): Double? =
    (buildBlackPearlFlashPlan(this, activeSlot = 0x00) as? BlackPearlFlashPlan.Ready)
        ?.requiredPlaybackGainDb

/** Caution shown before Flash when the plan is representable but outside a validated device range. */
fun OpraEqProfile.blackPearlFlashWarning(): String? =
    (buildBlackPearlFlashPlan(this, activeSlot = 0x00) as? BlackPearlFlashPlan.Ready)
        ?.warning
