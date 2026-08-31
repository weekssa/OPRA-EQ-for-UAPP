package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences

data class CatalogExportVisibility(
    val catalog: OpraCatalog,
    val hiddenProfileCount: Int,
)

/**
 * Legacy presentation bridge retained for migration/test compatibility.
 *
 * v0.3 output context never filters the canonical EQ Library. Device capability is informational
 * and affects export/Flash actions only, so this function always returns the original catalog.
 */
@Suppress("UNUSED_PARAMETER")
fun OpraCatalog.forExportTargetVisibility(
    preferences: ExportTargetPreferences,
): CatalogExportVisibility = CatalogExportVisibility(
    catalog = this,
    hiddenProfileCount = 0,
)
