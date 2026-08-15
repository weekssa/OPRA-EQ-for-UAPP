package com.weekssa.opraeqforuapp.domain.model

enum class ProfileCompatibility(val isSelectable: Boolean, val isExportable: Boolean) {
    FullyCompatible(isSelectable = true, isExportable = true),
    CompatibleWithLimitation(isSelectable = true, isExportable = true),
    NotCompatible(isSelectable = false, isExportable = false),
}
