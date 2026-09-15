package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EqLibraryNavigationTest {
    @Test
    fun baselineDestinationsRemainThreeTabs() {
        assertThat(eqLibraryDestinations(showMyDac = false))
            .containsExactly(
                EqLibraryDestination.MyEqs,
                EqLibraryDestination.EqLibrary,
                EqLibraryDestination.Settings,
            )
            .inOrder()
    }

    @Test
    fun recognizedDacInsertsMyDacImmediatelyAfterMyEqs() {
        assertThat(eqLibraryDestinations(showMyDac = true))
            .containsExactly(
                EqLibraryDestination.MyEqs,
                EqLibraryDestination.MyDac,
                EqLibraryDestination.EqLibrary,
                EqLibraryDestination.Settings,
            )
            .inOrder()
    }

    @Test
    fun savedDestinationRestoresByIdentityNotPosition() {
        val withDac = eqLibraryDestinations(showMyDac = true)

        assertThat(restoreEqLibraryDestination(EqLibraryDestination.EqLibrary.name, withDac))
            .isEqualTo(EqLibraryDestination.EqLibrary)
        assertThat(restoreEqLibraryDestination(EqLibraryDestination.Settings.name, withDac))
            .isEqualTo(EqLibraryDestination.Settings)
    }

    @Test
    fun coldSessionWithoutDacFallsBackFromSavedMyDacToMyEqs() {
        val withoutDac = eqLibraryDestinations(showMyDac = false)

        assertThat(restoreEqLibraryDestination(EqLibraryDestination.MyDac.name, withoutDac))
            .isEqualTo(EqLibraryDestination.MyEqs)
    }

    @Test
    fun unknownSavedDestinationFallsBackSafely() {
        assertThat(restoreEqLibraryDestination("future-destination", eqLibraryDestinations(true)))
            .isEqualTo(EqLibraryDestination.MyEqs)
    }
}
