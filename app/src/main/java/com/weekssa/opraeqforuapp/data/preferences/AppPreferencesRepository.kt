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
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.EffectiveOutputResolver
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.OutputBehavior
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Process-lifetime action target chosen from My EQs / EQ Library. Never persisted. */
internal object SessionExportTarget {
    val activeTarget = MutableStateFlow<ExportDevice?>(null)

    fun select(device: ExportDevice) {
        if (device.selectableInV03) activeTarget.value = device
    }

    fun clear() {
        activeTarget.value = null
    }
}

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val presentSupportedDacs: Flow<Set<DacDeviceId>> = flowOf(emptySet()),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val preferences: Flow<AppPreferences> = combine(
        dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        },
        SessionExportTarget.activeTarget,
        presentSupportedDacs,
    ) { preferences, sessionActiveTarget, presentDeviceIds ->
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
        val manualOutputPreferences = ExportTargetPreferences.normalize(
            selectedTargets,
            storedActive,
        )
        val outputBehavior = OutputBehavior.fromStorageValue(preferences[Keys.OutputBehavior])
        val effective = EffectiveOutputResolver.resolve(
            behavior = outputBehavior,
            manualFallback = manualOutputPreferences.activeTarget,
            presentDeviceIds = presentDeviceIds,
            sessionOverride = sessionActiveTarget,
        )
        val effectiveOutputPreferences = ExportTargetPreferences.normalize(
            selectedTargets = manualOutputPreferences.selectedTargets + effective.output,
            activeTarget = effective.output,
        )

        AppPreferences(
            themeMode = ThemeMode.fromStorageValue(preferences[Keys.ThemeMode]),
            profileVisibility = ProfileVisibilityPreferences(
                showFullyCompatible = preferences[Keys.ShowFullyCompatible] ?: true,
                showCompatibleWithLimitation = preferences[Keys.ShowCompatibleWithLimitation] ?: true,
                showNotCompatible = preferences[Keys.ShowNotCompatible] ?: true,
            ),
            exportTargets = effectiveOutputPreferences,
            manualExportTargets = manualOutputPreferences,
            outputBehavior = outputBehavior,
            directBlackPearlFlashEnabled = preferences[Keys.DirectBlackPearlFlashEnabled] ?: false,
            directFiioJa11FlashEnabled = preferences[Keys.DirectFiioJa11FlashEnabled] ?: false,
            directEw300FlashEnabled = preferences[Keys.DirectEw300FlashEnabled] ?: false,
            // Legacy migration state only. JCALLY is no longer a current product output.
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

    suspend fun setOutputBehavior(outputBehavior: OutputBehavior) {
        SessionExportTarget.clear()
        updatePreferences { preferences ->
            preferences[Keys.OutputBehavior] = outputBehavior.storageValue
        }
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
        updatePreferences { preferences ->
            val current = outputPreferences(
                preferences[Keys.SelectedExportTargets],
                preferences[Keys.ActiveExportTarget],
            )
            val next = current.withTarget(device, enabled)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
        }
    }

    /** Persistent Settings choice for Default EQ target; this retains the approved Manual-mode action. */
    suspend fun setActiveExportTarget(device: ExportDevice) {
        if (!device.selectableInV03) return
        SessionExportTarget.clear()
        updatePreferences { preferences ->
            val current = outputPreferences(
                preferences[Keys.SelectedExportTargets],
                preferences[Keys.ActiveExportTarget],
            )
            val next = current.withActiveTarget(device)
            preferences[Keys.SelectedExportTargets] = next.selectedTargets.mapTo(mutableSetOf()) { it.name }
            preferences[Keys.ActiveExportTarget] = next.activeTarget.name
            preferences[Keys.OutputBehavior] = OutputBehavior.Manual.storageValue
        }
    }

    suspend fun setDirectBlackPearlFlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectBlackPearlFlashEnabled] = enabled
    }

    suspend fun setDirectFiioJa11FlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectFiioJa11FlashEnabled] = enabled
    }

    suspend fun setDirectEw300FlashEnabled(enabled: Boolean) = updatePreferences { preferences ->
        preferences[Keys.DirectEw300FlashEnabled] = enabled
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
        val OutputBehavior = stringPreferencesKey("output_behavior")
        val ShowFullyCompatible = booleanPreferencesKey("show_fully_compatible")
        val ShowCompatibleWithLimitation = booleanPreferencesKey("show_compatible_with_limitation")
        val ShowNotCompatible = booleanPreferencesKey("show_not_compatible")
        val SelectedExportTargets = stringSetPreferencesKey("selected_export_targets")
        val ActiveExportTarget = stringPreferencesKey("active_export_target")
        val DirectBlackPearlFlashEnabled = booleanPreferencesKey("direct_black_pearl_flash_enabled")
        val DirectFiioJa11FlashEnabled = booleanPreferencesKey("direct_fiio_ja11_flash_enabled")
        val DirectEw300FlashEnabled = booleanPreferencesKey("direct_ew300_flash_enabled")
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
