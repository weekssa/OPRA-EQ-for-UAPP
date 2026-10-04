package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EqLibraryNavigationTest {
    @Test
    fun persistentRootDestinationsAreAlwaysThreeAndStable() {
        assertThat(eqLibraryRootDestinations())
            .containsExactly(
                EqLibraryDestination.MyEqs,
                EqLibraryDestination.EqLibrary,
                EqLibraryDestination.Settings,
            )
            .inOrder()
        assertThat(eqLibraryRootDestinations()).doesNotContain(EqLibraryDestination.MyDac)
    }

    @Test
    fun deviceDoesNotChangeRootNavigationButOpensContextualWorkspaceRoute() {
        assertThat(eqLibraryRootDestinations()).hasSize(3)
        assertThat(eqLibraryAvailableDestinations(hasRecognizedDac = false))
            .doesNotContain(EqLibraryDestination.MyDac)
        assertThat(eqLibraryAvailableDestinations(hasRecognizedDac = true))
            .contains(EqLibraryDestination.MyDac)
    }

    @Test
    fun savedRootDestinationRestoresByIdentity() {
        val roots = eqLibraryRootDestinations()

        assertThat(restoreEqLibraryDestination(EqLibraryDestination.EqLibrary.name, roots))
            .isEqualTo(EqLibraryDestination.EqLibrary)
        assertThat(restoreEqLibraryDestination(EqLibraryDestination.Settings.name, roots))
            .isEqualTo(EqLibraryDestination.Settings)
    }

    @Test
    fun coldSessionWithoutRecognizedDeviceFallsBackFromSavedWorkspaceToMyEqs() {
        val withoutDac = eqLibraryAvailableDestinations(hasRecognizedDac = false)

        assertThat(restoreEqLibraryDestination(EqLibraryDestination.MyDac.name, withoutDac))
            .isEqualTo(EqLibraryDestination.MyEqs)
    }

    @Test
    fun persistentRootsLeaveBackToAndroidWhileMyDacUsesInAppBack() {
        eqLibraryRootDestinations().forEach { destination ->
            assertThat(hasDestinationBackHandler(destination)).isFalse()
        }
        assertThat(hasDestinationBackHandler(EqLibraryDestination.MyDac)).isTrue()
    }

    @Test
    fun unknownSavedDestinationFallsBackSafely() {
        assertThat(restoreEqLibraryDestination("future-destination", eqLibraryRootDestinations()))
            .isEqualTo(EqLibraryDestination.MyEqs)
    }
}
