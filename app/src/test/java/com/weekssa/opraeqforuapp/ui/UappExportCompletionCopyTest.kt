package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.export.PresetExportItemResult
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.PresetExportCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class UappExportCompletionCopyTest {
    @Test
    fun createdOrUpdatedUappFilesRequestManualImportGuidance() {
        val summary = PresetExportSummary(
            listOf(PresetExportItemResult.Created(candidate)),
        )

        assertEquals(UappExportCompletionCopy.EXPORTED, uappExportCompletionCopy(ExportDevice.UAPP, summary))
    }

    @Test
    fun alreadyCurrentUappFilesStillExplainManualImport() {
        val summary = PresetExportSummary(
            listOf(PresetExportItemResult.Current(candidate)),
        )

        assertEquals(UappExportCompletionCopy.ALREADY_CURRENT, uappExportCompletionCopy(ExportDevice.UAPP, summary))
    }

    @Test
    fun failedNonUappAndLostAccessResultsDoNotClaimUappFilesAreReady() {
        val failed = PresetExportSummary(
            listOf(PresetExportItemResult.Failed(candidate, "fixture failure")),
        )
        val nonUapp = PresetExportSummary(
            listOf(PresetExportItemResult.Created(candidate)),
        )
        val accessLost = PresetExportSummary(
            results = listOf(PresetExportItemResult.Created(candidate)),
            accessLost = true,
        )

        assertEquals(UappExportCompletionCopy.NONE, uappExportCompletionCopy(ExportDevice.UAPP, failed))
        assertEquals(UappExportCompletionCopy.NONE, uappExportCompletionCopy(ExportDevice.BLACK_PEARL, nonUapp))
        assertEquals(UappExportCompletionCopy.NONE, uappExportCompletionCopy(ExportDevice.UAPP, accessLost))
    }

    private companion object {
        val candidate = PresetExportCandidate(
            profileId = "profile",
            productId = "product",
            manufacturerName = "Fixture",
            modelName = "Headphone",
            relativeDirectory = "Fixture/Headphone",
            fileName = "Fixture_Headphone.xml",
            xml = "<preset />",
            generatedFingerprint = "fingerprint",
            contentHash = "content-hash",
            deviceName = "UAPP",
            fidelity = DevicePresetFidelity.EXACT,
        )
    }
}
