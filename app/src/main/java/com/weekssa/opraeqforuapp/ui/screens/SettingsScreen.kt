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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    catalogState: CatalogState,
    onRefreshCatalog: () -> Unit,
    onChangeExportFolder: () -> Unit,
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
            text = appPreferences.exportTreeLabel ?: "Not chosen yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Suggested location: Documents/OPRA EQ for UAPP/Presets. You can choose any folder offered by Android.",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onChangeExportFolder) {
            Text(if (appPreferences.exportTreeUri == null) "Choose folder" else "Change folder")
        }
        if (appPreferences.exportTreeUri != null) {
            Text(
                text = "Changing the folder affects future exports only. Files in the previous folder are not moved or deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionDivider()
        SectionTitle("OPRA catalog")
        when (catalogState) {
            CatalogState.Loading -> Text(
                text = "Downloading OPRA catalog…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is CatalogState.Unavailable -> {
                Text(
                    text = unavailableCatalogMessage(catalogState.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRefreshCatalog) {
                    Text("Try refresh")
                }
            }
            is CatalogState.Ready -> {
                Text(
                    text = if (catalogState.isRefreshing) "Refreshing…" else "Saved catalog available",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${catalogState.catalog.vendors.size} manufacturers · ${catalogState.catalog.products.size} headphones · ${catalogState.catalog.profiles.size} EQ profiles",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Last refreshed ${formatCatalogTime(catalogState.lastSuccessfulRefreshMillis)}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "The app checks for OPRA updates approximately daily.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onRefreshCatalog,
                    enabled = !catalogState.isRefreshing,
                ) {
                    Text("Refresh now")
                }
            }
        }

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

private fun formatCatalogTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
