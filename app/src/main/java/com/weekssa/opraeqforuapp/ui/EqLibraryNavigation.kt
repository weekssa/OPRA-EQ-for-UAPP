package com.weekssa.opraeqforuapp.ui

import androidx.annotation.StringRes
import com.weekssa.opraeqforuapp.R

/** Stable top-level destinations. Persist the enum name, never a positional index. */
internal enum class EqLibraryDestination(@param:StringRes val labelResId: Int) {
    MyEqs(R.string.nav_my_eqs),
    MyDac(R.string.nav_my_dac),
    EqLibrary(R.string.nav_eq_library),
    Settings(R.string.nav_settings),
}

internal fun eqLibraryRootDestinations(): List<EqLibraryDestination> = listOf(
    EqLibraryDestination.MyEqs,
    EqLibraryDestination.EqLibrary,
    EqLibraryDestination.Settings,
)

/** My DAC is an in-session workspace reached from device context, never a persistent root tab. */
internal fun eqLibraryAvailableDestinations(hasRecognizedDac: Boolean): List<EqLibraryDestination> =
    eqLibraryRootDestinations() + if (hasRecognizedDac) listOf(EqLibraryDestination.MyDac) else emptyList()

/** Persistent root destinations leave Back to Android; only the nested My DAC workspace intercepts it. */
internal fun hasDestinationBackHandler(destination: EqLibraryDestination): Boolean =
    destination == EqLibraryDestination.MyDac

/**
 * Restores by stable identity. A destination that is unavailable in the current cold session falls
 * back to My EQs instead of interpreting an old numeric index against a different destination list.
 */
internal fun restoreEqLibraryDestination(
    savedName: String?,
    available: List<EqLibraryDestination>,
): EqLibraryDestination {
    val saved = savedName?.let { name ->
        EqLibraryDestination.entries.firstOrNull { it.name == name }
    }
    return saved?.takeIf(available::contains) ?: EqLibraryDestination.MyEqs
}
