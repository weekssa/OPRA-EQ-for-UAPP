package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProfileVisibilityChange: (ProfileVisibilityCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SectionTitle("Profile visibility")
        VisibilityOption(
            title = "Fully compatible",
            checked = appPreferences.profileVisibility.showFullyCompatible,
            onCheckedChange = {
                onProfileVisibilityChange(ProfileVisibilityCategory.FullyCompatible, it)
            },
        )
        VisibilityOption(
            title = "Compatible with limitation",
            checked = appPreferences.profileVisibility.showCompatibleWithLimitation,
            onCheckedChange = {
                onProfileVisibilityChange(ProfileVisibilityCategory.CompatibleWithLimitation, it)
            },
        )
        VisibilityOption(
            title = "Not compatible",
            checked = appPreferences.profileVisibility.showNotCompatible,
            onCheckedChange = {
                onProfileVisibilityChange(ProfileVisibilityCategory.NotCompatible, it)
            },
        )

        SectionDivider()
        SectionTitle("Appearance")
        ThemeOption(
            title = "System default",
            description = "Follow your Android device appearance.",
            selected = appPreferences.themeMode == ThemeMode.System,
            onSelected = { onThemeModeChange(ThemeMode.System) },
        )
        ThemeOption(
            title = "Light",
            selected = appPreferences.themeMode == ThemeMode.Light,
            onSelected = { onThemeModeChange(ThemeMode.Light) },
        )
        ThemeOption(
            title = "Dark",
            selected = appPreferences.themeMode == ThemeMode.Dark,
            onSelected = { onThemeModeChange(ThemeMode.Dark) },
        )

        SectionDivider()
        SectionTitle("Presets")
        Text("Export folder")
        Text(
            text = "Not chosen yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionDivider()
        SectionTitle("OPRA catalog")
        Text("Catalog status")
        Text(
            text = "Not downloaded yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionDivider()
        SectionTitle("About")
        Text("OPRA EQ for UAPP")
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
}

@Composable
private fun VisibilityOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(Modifier.width(12.dp))
        Text(title)
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onSelected: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelected,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
