package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoundImpactSummaryTest {
    @Test
    fun summarizesBroadFilterActionsWithoutStockClaims() {
        val summary = SoundImpactSummary.fromFilters(
            listOf(
                EqFilter(EqFilterType.LOW_SHELF, 70.0, 3.0, 0.7),
                EqFilter(EqFilterType.PEAK, 3_800.0, -2.0, 1.0),
            ),
        )

        assertThat(summary).isEqualTo("Adds bass and reduces upper-mid energy.")
    }

    @Test
    fun ignoresTinyAndVeryNarrowCorrections() {
        val summary = SoundImpactSummary.fromFilters(
            listOf(
                EqFilter(EqFilterType.PEAK, 1_000.0, 0.3, 1.0),
                EqFilter(EqFilterType.PEAK, 8_000.0, -1.0, 8.0),
            ),
        )

        assertThat(summary).isNull()
    }

    @Test
    fun limitsSummaryToTwoOrderedRegions() {
        val summary = SoundImpactSummary.fromFilters(
            listOf(
                EqFilter(EqFilterType.LOW_SHELF, 50.0, 4.0, 0.7),
                EqFilter(EqFilterType.PEAK, 500.0, 2.0, 1.0),
                EqFilter(EqFilterType.HIGH_SHELF, 8_000.0, -3.0, 0.7),
            ),
        )

        assertThat(summary).isEqualTo("Adds sub-bass and adds slightly lower-mid energy.")
    }
}
