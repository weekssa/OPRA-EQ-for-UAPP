package com.weekssa.opraeqforuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.weekssa.opraeqforuapp.data.catalog.HttpOpraCatalogSource
import com.weekssa.opraeqforuapp.data.catalog.OpraCatalogRepository
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.ui.OpraEqApp
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appPreferencesRepository by lazy {
        AppPreferencesRepository(applicationContext)
    }

    private val catalogRepository by lazy {
        OpraCatalogRepository(
            filesDir = filesDir,
            source = HttpOpraCatalogSource(
                userAgent = "OPRA EQ for UAPP/${BuildConfig.VERSION_NAME}",
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            catalogRepository.initialize()
        }

        setContent {
            val appPreferences = appPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            ).value
            val catalogState = catalogRepository.state.collectAsStateWithLifecycle().value

            OpraEqTheme(themeMode = appPreferences.themeMode) {
                OpraEqApp(
                    appPreferences = appPreferences,
                    catalogState = catalogState,
                    onRefreshCatalog = catalogRepository::refresh,
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
