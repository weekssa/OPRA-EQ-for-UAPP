package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

/** Hardware-independent eligibility check for the EQ Library direct-Flash action. */
fun OpraEqProfile.isBlackPearlDirectFlashable(): Boolean =
    buildBlackPearlFlashPlan(this, activeSlot = 0x00) is BlackPearlFlashPlan.Ready
