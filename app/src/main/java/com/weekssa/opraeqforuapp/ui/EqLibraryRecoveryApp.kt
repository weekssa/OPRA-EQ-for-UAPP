package com.weekssa.opraeqforuapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqRecord

@Stable
data class UnclaimedEqUiFeature(
    val items: List<UnclaimedEqRecord> = emptyList(),
    val onRecover: suspend (
        documentUri: String,
        manufacturer: String,
        model: String,
        displayName: String,
    ) -> SavedEqRecord = { _, _, _, _ -> error("Unclaimed EQ recovery is unavailable.") },
    val onDelete: suspend (documentUri: String) -> Boolean = { false },
)

/** UI-scoped feature state; repositories and Android storage objects never enter Composition. */
val LocalUnclaimedEqUiFeature = staticCompositionLocalOf { UnclaimedEqUiFeature() }

/**
 * Recovery-aware entry point used by MainActivity. The established three-argument EqLibraryApp
 * remains the routing implementation; this overload only scopes feature state to My EQs.
 */
@Composable
fun EqLibraryApp(
    state: EqLibraryUiState,
    actions: EqLibraryActions,
    unclaimedEqs: List<UnclaimedEqRecord>,
    onRecoverUnclaimedEq: suspend (
        documentUri: String,
        manufacturer: String,
        model: String,
        displayName: String,
    ) -> SavedEqRecord,
    onDeleteUnclaimedEq: suspend (documentUri: String) -> Boolean,
    initialMyDacOpenDeviceId: DacDeviceId? = null,
) {
    CompositionLocalProvider(
        LocalUnclaimedEqUiFeature provides UnclaimedEqUiFeature(
            items = unclaimedEqs,
            onRecover = onRecoverUnclaimedEq,
            onDelete = onDeleteUnclaimedEq,
        ),
    ) {
        EqLibraryApp(
            state = state,
            actions = actions,
            initialMyDacOpenDeviceId = initialMyDacOpenDeviceId,
        )
    }
}
