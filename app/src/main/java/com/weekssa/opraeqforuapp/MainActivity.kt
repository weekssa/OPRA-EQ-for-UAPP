package com.weekssa.opraeqforuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.ui.OpraEqApp
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appPreferencesRepository by lazy {
        AppPreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appPreferences = appPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            ).value

            OpraEqTheme(themeMode = appPreferences.themeMode) {
                OpraEqApp(
                    appPreferences = appPreferences,
                    onThemeModeChange = { themeMode ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setThemeMode(themeMode)
                        }
                    },
                    onProfileVisibilityChange = { category, visible ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setProfileVisibility(category, visible)
                        }
                    },
                )
            }
        }
    }
}
