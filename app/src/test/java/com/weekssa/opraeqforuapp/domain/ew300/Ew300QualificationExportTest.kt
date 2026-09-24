package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300QualificationExportTest {
    @Test
    fun exportPinsCandidateAndPersistenceOutcomeInBothFormats() {
        val readOnly = Ew300CapabilityReport(
            planVersion = "test-plan",
            deviceFingerprintKey = "exact-device",
            cases = listOf(
                Ew300CapabilityCaseResult("snapshot", Ew300CapabilityCaseResult.Status.PASS, "passed"),
            ),
            stateKnown = true,
            stoppedAfterFailure = false,
        )
        val export = Ew300QualificationExport(
            candidateSourceSha = "0123456789abcdef0123456789abcdef01234567",
            readOnlyReport = readOnly,
            persistenceResult = Ew300PersistenceQualificationResult.Verified("restored"),
        )

        assertTrue(export.toReadableText().contains("0123456789abcdef"))
        assertTrue(export.toReadableText().contains("Persistence qualification: VERIFIED"))
        assertTrue(export.toJson().contains("\"status\":\"VERIFIED\""))
        assertTrue(export.toJson().contains("\"candidateSourceSha\""))
    }
}
