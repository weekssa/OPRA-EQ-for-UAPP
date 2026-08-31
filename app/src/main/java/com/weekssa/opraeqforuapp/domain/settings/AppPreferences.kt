package com.weekssa.opraeqforuapp.domain.settings

enum class ThemeMode(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

enum class ProfileVisibilityCategory {
    FullyCompatible,
    CompatibleWithLimitation,
    NotCompatible,
}

/**
 * Legacy v0.2 compatibility-filter preference retained only for storage/API migration.
 *
 * v0.3 output context never hides canonical library curves, so persisted compatibility toggles no
 * longer affect presentation. Device-specific capability is shown as information at export time.
 */
data class ProfileVisibilityPreferences(
    val showFullyCompatible: Boolean = true,
    val showCompatibleWithLimitation: Boolean = true,
    val showNotCompatible: Boolean = true,
) {
    @Suppress("UNUSED_PARAMETER")
    fun isVisible(category: ProfileVisibilityCategory): Boolean = true

    fun withVisibility(category: ProfileVisibilityCategory, visible: Boolean): ProfileVisibilityPreferences =
        when (category) {
            ProfileVisibilityCategory.FullyCompatible -> copy(showFullyCompatible = visible)
            ProfileVisibilityCategory.CompatibleWithLimitation -> copy(showCompatibleWithLimitation = visible)
            ProfileVisibilityCategory.NotCompatible -> copy(showNotCompatible = visible)
        }
}

data class UpdatePreferences(
    val latestVersion: String? = null,
    val releaseUrl: String? = null,
    val releaseNotes: String? = null,
    val lastCheckAttemptMillis: Long? = null,
    val dismissedVersion: String? = null,
    val lastSeenInstalledVersion: String? = null,
    val postUpdateVersionToShow: String? = null,
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val profileVisibility: ProfileVisibilityPreferences = ProfileVisibilityPreferences(),
    val exportTargets: ExportTargetPreferences = ExportTargetPreferences(),
    val directBlackPearlFlashEnabled: Boolean = false,
    val exportTreeUri: String? = null,
    val exportTreeLabel: String? = null,
    val updates: UpdatePreferences = UpdatePreferences(),
)
