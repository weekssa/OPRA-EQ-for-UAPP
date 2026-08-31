package com.weekssa.opraeqforuapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weekssa.opraeqforuapp.data.update.AppReleaseInfo
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.domain.settings.UpdatePreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
            val storedTargets = preferences[Keys.SelectedExportTargets]
            val selectedTargets = if (storedTargets == null) {
                setOf(ExportDevice.UAPP)
            } else {
                storedTargets.mapNotNullTo(mutableSetOf()) { storedName ->
                    ExportDevice.entries.firstOrNull { it.name == storedName }
                }
            }
            val storedActive = preferences[Keys.ActiveExportTarget]
                ?.let { storedName -> ExportDevice.entries.firstOrNull { it.name == storedName } }
            val outputPreferences = ExportTargetPreferences.normalize(selectedTargets, storedActive)

            AppPreferences(
                themeMode = ThemeMode.fromStorageValue(preferences[Keys.ThemeMode]),
                profileVisibility = ProfileVisibilityPreferences(
                    showFullyCompatible = preferences[Keys.ShowFullyCompatible] ?: true,
                    showCompatibleWithLimitation = preferences[Keys.ShowCompatibleWithLimitation] ?: true,
                    showNotCompatible = preferences[Keys.ShowNotCompatible] ?: true,
                ),
                exportTargets = outputPreferences,
                directBlackPearlFlashEnabled = preferences[Keys.DirectBlackPearlFlashEnabled] ?: false,
                exportTreeUri = preferences[Keys.ExportTreeUri],
                exportTreeLabel = preferences[Keys.ExportTreeLabel],
                updates = UpdatePreferences(
                    latestVersion = preferences[Keys.LatestReleaseVersion],
                    releaseUrl = preferences[Keys.LatestReleaseUrl],
                    releaseNotes = preferences[Keys.LatestReleaseNotes],
                    lastCheckAttemptMillis = preferences[Keys.LastUpdateCheckAttemptMillis],
                    dismissedVersion = preferences[Keys.DismissedUpdateVersion],
                    lastSeenInstalledVersion = preferences[Keys.LastSeenInstalledVersion],
                    postUpdateVersionToShow = preferences[Keys.PostUpdateVersionToShow],
                ),
            )
        }

    suspend fun snapshot(): AppPreferences = preferences.first()

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

    suspend fun setExportTargetEnabled(device: ExportDevice, enabled: Boolean) {
        if (!device.selectableInV03) return
        appContext.appPreferencesDataStore.edit { preferences ->
            val current = outputPreferences(preferences[Keys.SelectedExportTargets], preferences[Keys.ActiveExportTarget])
            val next = current.withTarget(device, enabled)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
        }
    }

    suspend fun setActiveExportTarget(device: ExportDevice) {
        if (!device.selectableInV03) return
        appContext.appPreferencesDataStore.edit { preferences ->
            val current = outputPreferences(preferences[Keys.SelectedExportTargets], preferences[Keys.ActiveExportTarget])
            val next = current.withActiveTarget(device)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
        }
    }

    suspend fun setDirectBlackPearlFlashEnabled(enabled: Boolean) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.DirectBlackPearlFlashEnabled] = enabled
        }
    }

    suspend fun setExportTree(uri: String, label: String) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.ExportTreeUri] = uri
            preferences[Keys.ExportTreeLabel] = label
        }
    }

    suspend fun initializeInstalledVersion(currentVersion: String) {
        appContext.appPreferencesDataStore.edit { preferences ->
            val previous = preferences[Keys.LastSeenInstalledVersion]
            when {
                previous == null -> {
                    preferences[Keys.LastSeenInstalledVersion] = currentVersion
                    preferences.remove(Keys.PostUpdateVersionToShow)
                }
                previous != currentVersion -> {
                    preferences[Keys.LastSeenInstalledVersion] = currentVersion
                    preferences[Keys.PostUpdateVersionToShow] = currentVersion
                }
            }
        }
    }

    suspend fun markUpdateCheckAttempt(atMillis: Long) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.LastUpdateCheckAttemptMillis] = atMillis
        }
    }

    suspend fun storeLatestRelease(release: AppReleaseInfo, checkedAtMillis: Long) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.LastUpdateCheckAttemptMillis] = checkedAtMillis
            preferences[Keys.LatestReleaseVersion] = release.version
            preferences[Keys.LatestReleaseUrl] = release.releaseUrl
            preferences[Keys.LatestReleaseNotes] = release.notes
        }
    }

    suspend fun dismissUpdate(version: String) {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.DismissedUpdateVersion] = version
        }
    }

    suspend fun dismissPostUpdateCard() {
        appContext.appPreferencesDataStore.edit { preferences ->
            preferences.remove(Keys.PostUpdateVersionToShow)
        }
    }

    private fun outputPreferences(
        storedTargets: Set<String>?,
        storedActive: String?,
    ): ExportTargetPreferences {
        val selected = if (storedTargets == null) {
            setOf(ExportDevice.UAPP)
        } else {
            storedTargets.mapNotNullTo(mutableSetOf()) { storedName ->
                ExportDevice.entries.firstOrNull { it.name == storedName }
            }
        }
        val active = storedActive?.let { name -> ExportDevice.entries.firstOrNull { it.name == name } }
        return ExportTargetPreferences.normalize(selected, active)
    }

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val ShowFullyCompatible = booleanPreferencesKey("show_fully_compatible")
        val ShowCompatibleWithLimitation = booleanPreferencesKey("show_compatible_with_limitation")
        val ShowNotCompatible = booleanPreferencesKey("show_not_compatible")
        val SelectedExportTargets = stringSetPreferencesKey("selected_export_targets")
        val ActiveExportTarget = stringPreferencesKey("active_export_target")
        val DirectBlackPearlFlashEnabled = booleanPreferencesKey("direct_black_pearl_flash_enabled")
        // Legacy v0.3 preview key intentionally left unread. Output selection no longer hides library curves.
        @Suppress("unused")
        val ShowUnexportablePresets = booleanPreferencesKey("show_unexportable_presets")
        val ExportTreeUri = stringPreferencesKey("export_tree_uri")
        val ExportTreeLabel = stringPreferencesKey("export_tree_label")
        val LatestReleaseVersion = stringPreferencesKey("latest_release_version")
        val LatestReleaseUrl = stringPreferencesKey("latest_release_url")
        val LatestReleaseNotes = stringPreferencesKey("latest_release_notes")
        val LastUpdateCheckAttemptMillis = longPreferencesKey("last_update_check_attempt_millis")
        val DismissedUpdateVersion = stringPreferencesKey("dismissed_update_version")
        val LastSeenInstalledVersion = stringPreferencesKey("last_seen_installed_version")
        val PostUpdateVersionToShow = stringPreferencesKey("post_update_version_to_show")
    }
}
