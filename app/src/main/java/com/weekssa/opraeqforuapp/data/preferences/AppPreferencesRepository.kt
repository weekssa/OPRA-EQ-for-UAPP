package com.weekssa.opraeqforuapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext

    val preferences: Flow<AppPreferences> = appContext.appPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                themeMode = ThemeMode.fromStorageValue(preferences[Keys.ThemeMode]),
                profileVisibility = ProfileVisibilityPreferences(
                    showFullyCompatible = preferences[Keys.ShowFullyCompatible] ?: true,
                    showCompatibleWithLimitation = preferences[Keys.ShowCompatibleWithLimitation] ?: true,
                    showNotCompatible = preferences[Keys.ShowNotCompatible] ?: true,
                ),
                exportTreeUri = preferences[Keys.ExportTreeUri],
                exportTreeLabel = preferences[Keys.ExportTreeLabel],
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = themeMode.storageValue
        }
    }

    suspend fun setProfileVisibility(category: ProfileVisibilityCategory, visible: Boolean) {
        appContext.appPreferencesDataStore.edit { preferences ->
            when (category) {
                ProfileVisibilityCategory.FullyCompatible -> preferences[Keys.ShowFullyCompatible] = visible
                ProfileVisibilityCategory.CompatibleWithLimitation -> preferences[Keys.ShowCompatibleWithLimitation] = visible
                ProfileVisibilityCategory.NotCompatible -> preferences[Keys.ShowNotCompatible] = visible
            }
        }
    }

    suspend fun setExportTree(uri: String, label: String) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.ExportTreeUri] = uri
            preferences[Keys.ExportTreeLabel] = label
        }
    }

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val ShowFullyCompatible = booleanPreferencesKey("show_fully_compatible")
        val ShowCompatibleWithLimitation = booleanPreferencesKey("show_compatible_with_limitation")
        val ShowNotCompatible = booleanPreferencesKey("show_not_compatible")
        val ExportTreeUri = stringPreferencesKey("export_tree_uri")
        val ExportTreeLabel = stringPreferencesKey("export_tree_label")
    }
}
