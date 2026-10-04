package com.weekssa.opraeqforuapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class EqLibraryPalette(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val connected: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val recovery: Color,
    val stale: Color,
    val graphGrid: Color,
    val graphZero: Color,
    val graphCurrent: Color,
    val graphDraft: Color,
    val graphBand: Color,
    val graphBandMuted: Color,
)

internal val LightEqLibraryPalette = EqLibraryPalette(
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFF0F3F7),
    border = Color(0xFFC9D1DC),
    textPrimary = Color(0xFF18202A),
    textSecondary = Color(0xFF5B6573),
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF163A70),
    connected = Color(0xFF006A6A),
    success = Color(0xFF1B6E3A),
    warning = Color(0xFF8A5A00),
    error = Color(0xFFB3261E),
    recovery = Color(0xFF9A3F00),
    stale = Color(0xFF68707B),
    graphGrid = Color(0xFFD7DEE8),
    graphZero = Color(0xFF68707B),
    graphCurrent = Color(0xFF68707B),
    graphDraft = Color(0xFF0B57D0),
    graphBand = Color(0xFF0B57D0),
    graphBandMuted = Color(0xFF557397),
)

internal val DarkEqLibraryPalette = EqLibraryPalette(
    background = Color(0xFF0B0F14),
    surface = Color(0xFF111821),
    surfaceSubtle = Color(0xFF18212C),
    border = Color(0xFF33404E),
    textPrimary = Color(0xFFEEF3F8),
    textSecondary = Color(0xFFB5C0CD),
    primary = Color(0xFF9CC2FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF17355A),
    onPrimaryContainer = Color(0xFFA7CAFF),
    connected = Color(0xFF5DD7D7),
    success = Color(0xFF72D99B),
    warning = Color(0xFFF0C36A),
    error = Color(0xFFFFB4AB),
    recovery = Color(0xFFFFB77A),
    stale = Color(0xFFA7B0BA),
    graphGrid = Color(0xFF2C3744),
    graphZero = Color(0xFFA7B0BA),
    graphCurrent = Color(0xFFA7B0BA),
    graphDraft = Color(0xFF9CC2FF),
    graphBand = Color(0xFF9CC2FF),
    graphBandMuted = Color(0xFF6983A5),
)

internal val LocalEqLibraryPalette = staticCompositionLocalOf { LightEqLibraryPalette }

internal val MaterialThemeEqPalette: EqLibraryPalette
    @Composable get() = LocalEqLibraryPalette.current
