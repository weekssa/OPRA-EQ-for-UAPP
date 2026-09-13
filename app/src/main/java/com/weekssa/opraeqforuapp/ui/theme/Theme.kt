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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

/**
 * EQ Library's product accent, sampled from the approved TOPPING reference UI.
 *
 * Keep semantic error/warning/success colors separate. This token is for brand/interactive emphasis:
 * navigation selection, primary actions, selected controls, links, sliders, focus, and graphs.
 */
internal val ToppingBlue = Color(0xFF257BFF)
private val ToppingBlueStrong = Color(0xFF1F6FE8)
private val ToppingBlueLightContainer = Color(0xFFD9E7FF)
private val ToppingBlueLightSecondaryContainer = Color(0xFFD5E5FF)
private val ToppingBlueDarkContainer = Color(0xFF0B3C86)
private val ToppingBlueDarkSecondaryContainer = Color(0xFF133B76)
private val OnToppingBlue = Color(0xFF001A3A)
private val OnToppingBlueDarkContainer = Color(0xFFD9E7FF)

private val LightColorScheme = lightColorScheme(
    primary = ToppingBlue,
    onPrimary = OnToppingBlue,
    primaryContainer = ToppingBlueLightContainer,
    onPrimaryContainer = OnToppingBlue,
    secondary = ToppingBlueStrong,
    onSecondary = Color.White,
    secondaryContainer = ToppingBlueLightSecondaryContainer,
    onSecondaryContainer = OnToppingBlue,
    tertiary = ToppingBlueStrong,
    onTertiary = Color.White,
    tertiaryContainer = ToppingBlueLightContainer,
    onTertiaryContainer = OnToppingBlue,
)

private val DarkColorScheme = darkColorScheme(
    primary = ToppingBlue,
    onPrimary = OnToppingBlue,
    primaryContainer = ToppingBlueDarkContainer,
    onPrimaryContainer = OnToppingBlueDarkContainer,
    secondary = ToppingBlue,
    onSecondary = OnToppingBlue,
    secondaryContainer = ToppingBlueDarkSecondaryContainer,
    onSecondaryContainer = OnToppingBlueDarkContainer,
    tertiary = ToppingBlue,
    onTertiary = OnToppingBlue,
    tertiaryContainer = ToppingBlueDarkContainer,
    onTertiaryContainer = OnToppingBlueDarkContainer,
)

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

    ApplySystemBarAppearance(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content,
    )
}

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
