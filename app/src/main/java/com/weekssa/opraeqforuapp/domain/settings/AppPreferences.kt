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

data class ProfileVisibilityPreferences(
    val showFullyCompatible: Boolean = true,
    val showCompatibleWithLimitation: Boolean = true,
    val showNotCompatible: Boolean = true,
) {
    fun isVisible(category: ProfileVisibilityCategory): Boolean = when (category) {
        ProfileVisibilityCategory.FullyCompatible -> showFullyCompatible
        ProfileVisibilityCategory.CompatibleWithLimitation -> showCompatibleWithLimitation
        ProfileVisibilityCategory.NotCompatible -> showNotCompatible
    }

    fun withVisibility(category: ProfileVisibilityCategory, visible: Boolean): ProfileVisibilityPreferences =
        when (category) {
            ProfileVisibilityCategory.FullyCompatible -> copy(showFullyCompatible = visible)
            ProfileVisibilityCategory.CompatibleWithLimitation -> copy(showCompatibleWithLimitation = visible)
            ProfileVisibilityCategory.NotCompatible -> copy(showNotCompatible = visible)
        }
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val profileVisibility: ProfileVisibilityPreferences = ProfileVisibilityPreferences(),
)
