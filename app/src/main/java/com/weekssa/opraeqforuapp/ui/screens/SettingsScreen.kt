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
import com.weekssa.opraeqforuapp.domain.update.SemVer
import com.weekssa.opraeqforuapp.ui.components.OPRA_DATA_LICENSE_URL
import com.weekssa.opraeqforuapp.ui.components.OpraAttribution
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    catalogState: CatalogState,
    onRefreshCatalog: () -> Unit,
    onChangeExportFolder: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onWhatsNew: () -> Unit,
    onGetUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
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
        SectionTitle("Exports")
        Text("EQ Library root folder")
        Text(
            text = appPreferences.exportTreeLabel ?: "Not chosen yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Suggested location: Documents/EQ Library. Each export creates device-first folders for UAPP, TRN Black Pearl, Topping DX5 II, and Topping DX1 II.",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onChangeExportFolder) {
            Text(if (appPreferences.exportTreeUri == null) "Choose root folder" else "Change root folder")
        }
        if (appPreferences.exportTreeUri != null) {
            Text(
                text = "Changing the root affects future exports only. Existing files are not moved or deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionDivider()
        SectionTitle("OPRA source")
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
                    text = if (catalogState.isRefreshing) "Refreshing…" else "Saved OPRA source available",
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
                    text = "OPRA is the first live source in this beta. Additional canonical/community sources are being added behind the same library model.",
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
        SectionTitle("About & updates")
        Text("EQ Library")
        Text(
            text = "Installed version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val latestVersion = appPreferences.updates.latestVersion
        val updateAvailable = latestVersion != null &&
            SemVer.parse(latestVersion)?.let { latest ->
                SemVer.parse(BuildConfig.VERSION_NAME)?.let { installed -> latest > installed }
            } == true
        when {
            updateAvailable -> Text(
                text = "Version $latestVersion is available.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            latestVersion != null -> Text(
                text = "You’re up to date.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Text(
                text = "Update status not available yet.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCheckForUpdates) { Text("Check for update") }
            if (latestVersion != null && !appPreferences.updates.releaseNotes.isNullOrBlank()) {
                TextButton(onClick = onWhatsNew) { Text("What’s new") }
            }
            if (updateAvailable && appPreferences.updates.releaseUrl != null) {
                TextButton(onClick = onGetUpdate) { Text("Get update") }
            }
        }
        Text(
            text = "Updates are downloaded manually from the public GitHub Release page. EQ Library does not silently download or install APKs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionDivider()
        SectionTitle("Privacy")
        Text(
            text = "Headphone selections, app settings, generated preset state, and conversion stay on this device. No account is required, and EQ Library contains no analytics or telemetry. This beta uses network access for the OPRA source catalog and public app-release metadata.",
            style = MaterialTheme.typography.bodyMedium,
        )

        SectionDivider()
        SectionTitle("Credits & licenses")
        OpraAttribution(onOpenUrl = onOpenUrl)
        Text(
            text = "OPRA manufacturer, product, and EQ data is provided under CC BY-SA 4.0. EQ Library preserves individual EQ creator/source information from OPRA where provided.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onOpenUrl(OPRA_DATA_LICENSE_URL) }) {
            Text("CC BY-SA 4.0 license")
        }
        Text(
            text = "EQ Library source code is Apache-2.0. The ToneBoosters/UAPP conversion mapping is based in part on SiliconExarch/EqConverter (Apache-2.0); provenance is documented in the project NOTICE. AndroidX, Kotlin, and kotlinx libraries retain their respective open-source licenses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "EQ Library is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, TRN, TOPPING, or headphone manufacturers.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
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

private fun unavailableCatalogMessage(reason: com.weekssa.opraeqforuapp.data.catalog.CatalogUnavailableReason): String =
    when (reason) {
        com.weekssa.opraeqforuapp.data.catalog.CatalogUnavailableReason.NoSavedCatalog ->
            "No saved OPRA catalog is available yet. Connect to the internet and try refresh."
        com.weekssa.opraeqforuapp.data.catalog.CatalogUnavailableReason.SavedCatalogInvalid ->
            "The saved OPRA catalog could not be used. Try refresh to download a clean copy."
    }
