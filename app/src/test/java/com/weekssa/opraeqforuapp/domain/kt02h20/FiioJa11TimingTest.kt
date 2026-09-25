package com.weekssa.opraeqforuapp.domain.kt02h20

import org.junit.Assert.assertEquals
import org.junit.Test

class FiioJa11TimingTest {
    @Test
    fun everyJa11MutationUsesTheDocumentedCommandInterval() {
        val reports = listOf(
            FiioJa11Protocol.writeOutputVolumeReport(42),
            FiioJa11Protocol.writeHeadsetControlReport(true),
            FiioJa11Protocol.writeBandReport(
                index = 0,
                band = FiioJa11Protocol.Band("peak_dip", 1_000.0, 0.0, 0.7),
            ),
            FiioJa11Protocol.writeGlobalGainReport(-4.0),
            FiioJa11Protocol.writeEqProgramReport(FiioJa11Protocol.EqProgram.USER_1),
            FiioJa11Protocol.applyReport(),
            FiioJa11Protocol.saveToFlashReport(),
            FiioJa11Protocol.writeUacModeReport(FiioJa11Protocol.UacMode.UAC_2),
        )

        assertEquals(200L, FiioJa11Timing.MUTATION_SETTLE_MILLIS)
        reports.forEach { report ->
            assertEquals(
                FiioJa11Timing.MUTATION_SETTLE_MILLIS,
                FiioJa11Timing.settleMillisForMutation(report),
            )
        }
    }
}
