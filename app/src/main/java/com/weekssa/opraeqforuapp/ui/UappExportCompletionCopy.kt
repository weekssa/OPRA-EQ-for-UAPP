package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.export.PresetExportItemResult
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.domain.export.ExportDevice

internal enum class UappExportCompletionCopy {
    EXPORTED,
    ALREADY_CURRENT,
    NONE,
}

internal fun uappExportCompletionCopy(
    exportDevice: ExportDevice,
    summary: PresetExportSummary,
): UappExportCompletionCopy = when {
    exportDevice != ExportDevice.UAPP || summary.accessLost || summary.successfulCount == 0 ->
        UappExportCompletionCopy.NONE
    summary.createdCount + summary.updatedCount > 0 -> UappExportCompletionCopy.EXPORTED
    summary.results.any { it is PresetExportItemResult.Current } -> UappExportCompletionCopy.ALREADY_CURRENT
    else -> UappExportCompletionCopy.NONE
}
