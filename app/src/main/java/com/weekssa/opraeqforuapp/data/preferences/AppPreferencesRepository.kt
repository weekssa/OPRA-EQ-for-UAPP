package com.weekssa.opraeqforuapp.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.weekssa.opraeqforuapp.data.update.AppReleaseInfo
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.domain.settings.UpdatePreferences
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Session overlay for the global output context.
     *
     * DataStore remains the durable source of truth, but a selector tap must affect every composed
     * screen immediately instead of waiting for the asynchronous disk-backed flow to round-trip.
     */
    private val activeTargetOverride = MutableStateFlow<ExportDevice?>(null)

    val preferences: Flow<AppPreferences> = combine(
        dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        },
        activeTargetOverride,
    ) { preferences, sessionActiveTarget ->
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
        val outputPreferences = ExportTargetPreferences.normalize(
            selectedTargets,
            sessionActiveTarget ?: storedActive,
        )

        AppPreferences(
            themeMode = ThemeMode.fromStorageValue(preferences[Keys.ThemeMode]),
            profileVisibility = ProfileVisibilityPreferences(
                showFullyCompatible = preferences[Keys.ShowFullyCompatible] ?: true,
                showCompatibleWithLimitation = preferences[Keys.ShowCompatibleWithLimitation] ?: true,
                showNotCompatible = preferences[Keys.ShowNotCompatible] ?: true,
            ),
            exportTargets = outputPreferences,
            directBlackPearlFlashEnabled = preferences[Keys.DirectBlackPearlFlashEnabled] ?: false,
            directFiioJa11FlashEnabled = preferences[Keys.DirectFiioJa11FlashEnabled] ?: false,
            directJcallyJm12FlashEnabled = preferences[Keys.DirectJcallyJm12FlashEnabled] ?: false,
            hiddenCanonicalProfileIds = preferences[Keys.HiddenCanonicalProfileIds].orEmpty(),
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
    }.flowOn(ioDispatcher)

    suspend fun snapshot(): AppPreferences = preferences.first()

    suspend fun setThemeMode(themeMode: ThemeMode) = updatePreferences { preferences ->
        preferences[Keys.ThemeMode] = themeMode.storageValue
    }

    suspend fun setProfileVisibility(category: ProfileVisibilityCategory, visible: Boolean) =
        updatePreferences { preferences ->
            when (category) {
                ProfileVisibilityCategory.FullyCompatible -> preferences[Keys.ShowFullyCompatible] = visible
                ProfileVisibilityCategory.CompatibleWithLimitation -> preferences[Keys.ShowCompatibleWithLimitation] = visible
                ProfileVisibilityCategory.NotCompatible -> preferences[Keys.ShowNotCompatible] = visible
            }
        }

    suspend fun setExportTargetEnabled(device: ExportDevice, enabled: Boolean) {
        if (!device.selectableInV03) return
        var nextActive: ExportDevice? = null
        updatePreferences { preferences ->
            val current = outputPreferences(
                preferences[Keys.SelectedExportTargets],
                preferences[Keys.ActiveExportTarget],
            )
            val next = current.withTarget(device, enabled)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
            nextActive = next.activeTarget
        }
        nextActive?.let { activeTargetOverride.value = it }
    }

    suspend fun setActiveExportTarget(device: ExportDevice) {
        if (!device.selectableInV03) return
        // Publish first so My EQs, EQ Library, and every callback switch operating context together.
        activeTargetOverride.value = device
        updatePreferences { preferences ->
            val current = outputPreferences(
                preferences[Keys.SelectedExportTargets],
                preferences[Keys.ActiveExportTarget],
            )
            val next = current.withActiveTarget(device)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
        }
    }

    suspend fun setDirectBlackPearlFlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectBlackPearlFlashEnabled] = enabled
    }

    suspend fun setDirectFiioJa11FlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectFiioJa11FlashEnabled] = enabled
    }

    suspend fun setDirectJcallyJm12FlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectJcallyJm12FlashEnabled] = enabled
    }

    suspend fun hideCanonicalProfiles(canonicalProfileIds: Set<String>) {
        val cleanIds = canonicalProfileIds.filterTo(mutableSetOf()) { it.isNotBlank() }
        if (cleanIds.isEmpty()) return
        updatePreferences { preferences ->
            preferences[Keys.HiddenCanonicalProfileIds] =
                preferences[Keys.HiddenCanonicalProfileIds].orEmpty() + cleanIds
        }
    }

    suspend fun unhideCanonicalProfiles(canonicalProfileIds: Set<String>) {
        if (canonicalProfileIds.isEmpty()) return
        updatePreferences { preferences ->
            val remaining = preferences[Keys.HiddenCanonicalProfileIds].orEmpty() - canonicalProfileIds
            if (remaining.isEmpty()) {
                preferences.remove(Keys.HiddenCanonicalProfileIds)
            } else {
                preferences[Keys.HiddenCanonicalProfileIds] = remaining
            }
        }
    }

    suspend fun setExportTree(uri: String, label: String) = updatePreferences { preferences ->
        preferences[Keys.ExportTreeUri] = uri
        preferences[Keys.ExportTreeLabel] = label
    }

    suspend fun initializeInstalledVersion(currentVersion: String) = updatePreferences { preferences ->
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

    suspend fun markUpdateCheckAttempt(atMillis: Long) = updatePreferences { preferences ->
        preferences[Keys.LastUpdateCheckAttemptMillis] = atMillis
    }

    suspend fun storeLatestRelease(release: AppReleaseInfo, checkedAtMillis: Long) =
        updatePreferences { preferences ->
            preferences[Keys.LastUpdateCheckAttemptMillis] = checkedAtMillis
            preferences[Keys.LatestReleaseVersion] = release.version
            preferences[Keys.LatestReleaseUrl] = release.releaseUrl
            preferences[Keys.LatestReleaseNotes] = release.notes
        }

    suspend fun dismissUpdate(version: String) = updatePreferences { preferences ->
        preferences[Keys.DismissedUpdateVersion] = version
    }

    suspend fun dismissPostUpdateCard() = updatePreferences { preferences ->
        preferences.remove(Keys.PostUpdateVersionToShow)
    }

    private suspend fun updatePreferences(block: (MutablePreferences) -> Unit) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences -> block(preferences) }
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
        val DirectFiioJa11FlashEnabled = booleanPreferencesKey("direct_fiio_ja11_flash_enabled")
        val DirectJcallyJm12FlashEnabled = booleanPreferencesKey("direct_jcally_jm12_flash_enabled")
        val HiddenCanonicalProfileIds = stringSetPreferencesKey("hidden_canonical_profile_ids")
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
