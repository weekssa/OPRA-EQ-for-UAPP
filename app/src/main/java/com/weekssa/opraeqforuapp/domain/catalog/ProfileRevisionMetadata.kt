package com.weekssa.opraeqforuapp.domain.catalog

fun OpraEqProfile.isHistoricalRevision(): Boolean = details
    ?.split(" · ")
    ?.any { it.trim().equals("Previous revision", ignoreCase = true) }
    ?: false
