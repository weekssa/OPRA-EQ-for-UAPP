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
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
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
    onExportTargetChange: (ExportDevice, Boolean) -> Unit,
    onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SectionTitle("Outputs")
        Text(
            text = "Choose which devices and apps appear in the output selector. The active output changes conversion, export, and My EQs context; it never hides curves from EQ Library.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ExportDevice.selectableOutputs.forEach { device ->
            CheckboxOption(
                title = outputTitle(device),
                description = outputDescription(device),
                checked = appPreferences.exportTargets.isSelected(device),
                onCheckedChange = { enabled -> onExportTargetChange(device, enabled) },
            )
        }

        if (appPreferences.exportTargets.isSelected(ExportDevice.BLACK_PEARL)) {
            SectionDivider()
            SectionTitle("Black Pearl")
            CheckboxOption(
                title = "Enable direct Flash",
                description = "Allow EQ Library to connect to the TRN Black Pearl and write EQ presets from My EQs. Flash may adjust global playback gain when required by a preset's preamp/headroom; the confirmation shows the exact adjustment. Other DAC controls are not managed.",
                checked = appPreferences.directBlackPearlFlashEnabled,
                onCheckedChange = onDirectBlackPearlFlashEnabledChange,
            )
        }

        SectionDivider()
        SectionTitle("Library")
        when (catalogState) {
            CatalogState.Loading -> Text(
                text = "Loading your saved EQ Library and checking available catalog state…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is CatalogState.Unavailable -> {
                Text(
                    text = eqLibraryCatalogFailureMessage(catalogState.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRefreshCatalog) { Text("Try refresh") }
            }
            is CatalogState.Ready -> {
                Text(
                    text = if (catalogState.isRefreshing) "Refreshing EQ Library…" else "Saved EQ Library available",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${catalogState.catalog.vendors.size} manufacturers · ${catalogState.catalog.products.size} headphones · ${catalogState.catalog.profiles.size} EQ profiles",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Last successful refresh ${formatCatalogTime(catalogState.lastSuccessfulRefreshMillis)}",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "EQ Library uses saved data immediately and opportunistically refreshes when the last successful refresh is about a day old. If you're offline, the saved library remains available.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onRefreshCatalog,
                    enabled = !catalogState.isRefreshing,
                ) { Text("Refresh now") }
            }
        }

        SectionDivider()
        SectionTitle("Export folder")
        Text("EQ Library root folder")
        Text(
            text = appPreferences.exportTreeLabel ?: "Not chosen yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Selected headphone presets are exported automatically when you Add or Save them. If a managed file is later missing or stale, My EQs exposes recovery Export actions. Suggested location: Documents/EQ Library.",
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
        SectionTitle("Updates")
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
        SectionTitle("About")
        Text(
            text = "Headphone selections, app settings, generated preset state, and conversion stay on this device. No account is required, and EQ Library contains no analytics or telemetry.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OpraAttribution(onOpenUrl = onOpenUrl, modifier = Modifier.padding(top = 12.dp))
        Text(
            text = "OPRA is one source inside EQ Library. The library can also contain attributed AutoEq, creator, repository, and public community EQ profiles. Individual source and creator information is preserved with each profile when available.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onOpenUrl(OPRA_DATA_LICENSE_URL) }) { Text("OPRA data license") }
        Text(
            text = "EQ Library source code is Apache-2.0. Black Pearl protocol references are studied for observable device behavior only; GPL implementation code is not copied into this project.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "EQ Library is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, TRN, Poweramp, Wavelet, or headphone manufacturers.",
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
private fun CheckboxOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
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
        RadioButton(selected = selected, onClick = onSelected)
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

private fun outputTitle(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "USB Audio Player PRO / ToneBoosters"
    ExportDevice.BLACK_PEARL -> "TRN Black Pearl"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Universal Parametric EQ"
    ExportDevice.POWERAMP -> "Poweramp / Poweramp Equalizer"
    ExportDevice.WAVELET -> "Wavelet"
    ExportDevice.TOPPING_DX5_II -> "TOPPING DX5 II"
    ExportDevice.TOPPING_DX1_II -> "TOPPING DX1 II"
}

private fun outputDescription(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "ToneBoosters XML for USB Audio Player PRO"
    ExportDevice.BLACK_PEARL -> "Preset file export plus optional direct Flash from My EQs"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Portable AutoEq / Equalizer APO-style parametric text"
    ExportDevice.POWERAMP -> "AutoEq parametric text supported by Poweramp and Poweramp Equalizer"
    ExportDevice.WAVELET -> "Wavelet 127-point GraphicEQ import"
    ExportDevice.TOPPING_DX5_II,
    ExportDevice.TOPPING_DX1_II -> "Hardware validation pending"
}

private fun formatCatalogTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private fun eqLibraryCatalogFailureMessage(reason: CatalogRefreshFailureReason): String = when (reason) {
    CatalogRefreshFailureReason.Network -> "Couldn’t refresh EQ Library. Check your connection; if you have a saved library, it remains available."
    CatalogRefreshFailureReason.InvalidCatalog -> "The downloaded EQ Library catalog could not be validated. Your last known-good library remains unchanged."
    CatalogRefreshFailureReason.Storage -> "EQ Library couldn’t save the refreshed catalog on this device."
}
