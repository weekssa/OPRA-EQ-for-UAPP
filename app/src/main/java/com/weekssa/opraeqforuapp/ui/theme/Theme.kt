package com.weekssa.opraeqforuapp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

@Composable
fun OpraEqTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (darkTheme) DarkEqLibraryPalette else LightEqLibraryPalette

    ApplySystemBarAppearance(darkTheme = darkTheme)

    CompositionLocalProvider(LocalEqLibraryPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = EqLibraryTypography,
            content = content,
        )
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF163A70),
    secondary = Color(0xFF0B57D0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF163A70),
    tertiary = Color(0xFF006A6A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4ECEB),
    onTertiaryContainer = Color(0xFF003D3D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF18202A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18202A),
    surfaceVariant = Color(0xFFF0F3F7),
    onSurfaceVariant = Color(0xFF5B6573),
    outline = Color(0xFF68707B),
    outlineVariant = Color(0xFFC9D1DC),
    scrim = Color.Black,
    inverseSurface = Color(0xFF18202A),
    inverseOnSurface = Color(0xFFF7F9FC),
    inversePrimary = Color(0xFF9CC2FF),
    surfaceTint = Color(0xFF0B57D0),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CC2FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF17355A),
    onPrimaryContainer = Color(0xFFA7CAFF),
    secondary = Color(0xFF9CC2FF),
    onSecondary = Color(0xFF002F65),
    secondaryContainer = Color(0xFF17355A),
    onSecondaryContainer = Color(0xFFA7CAFF),
    tertiary = Color(0xFF5DD7D7),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF004F50),
    onTertiaryContainer = Color(0xFFB9F0EF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFEEF3F8),
    surface = Color(0xFF111821),
    onSurface = Color(0xFFEEF3F8),
    surfaceVariant = Color(0xFF18212C),
    onSurfaceVariant = Color(0xFFB5C0CD),
    outline = Color(0xFFA7B0BA),
    outlineVariant = Color(0xFF33404E),
    scrim = Color.Black,
    inverseSurface = Color(0xFFEEF3F8),
    inverseOnSurface = Color(0xFF18212C),
    inversePrimary = Color(0xFF0B57D0),
    surfaceTint = Color(0xFF9CC2FF),
)

private val EqLibraryTypography = Typography().copy(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
private fun ApplySystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
