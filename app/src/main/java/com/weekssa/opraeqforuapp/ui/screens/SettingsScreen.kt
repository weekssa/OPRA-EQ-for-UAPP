package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.domain.update.SemVer
import com.weekssa.opraeqforuapp.ui.components.OPRA_DATA_LICENSE_URL
import com.weekssa.opraeqforuapp.ui.components.OPRA_PROJECT_URL
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private const val EQ_LIBRARY_PROJECT_URL = "https://github.com/weekssa/OPRA-EQ-for-UAPP"
private const val EQ_LIBRARY_PRIVACY_URL = "https://github.com/weekssa/OPRA-EQ-for-UAPP/blob/main/PRIVACY.md"
private const val EQ_LIBRARY_FEEDBACK_URL = "https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/new/choose"
private const val PARAEQ_PROJECT_URL = "https://github.com/wabsto1/ParaEQ"

private enum class SettingsPage {
    ROOT,
    HIDDEN_EQS,
    ABOUT,
    DATA_SOURCES,
}

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
    hiddenCanonicalProfileIds: Set<String>,
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.ROOT.name) }
    val page = runCatching { SettingsPage.valueOf(pageName) }.getOrDefault(SettingsPage.ROOT)
    val returnToRoot = { pageName = SettingsPage.ROOT.name }

    when (page) {
        SettingsPage.HIDDEN_EQS -> {
            HiddenEqSettingsScreen(
                catalogState = catalogState,
                hiddenCanonicalProfileIds = hiddenCanonicalProfileIds,
                onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,
                onMessage = onMessage,
                onBack = returnToRoot,
                modifier = modifier,
            )
            return
        }
        SettingsPage.ABOUT -> {
            AboutEqLibraryScreen(
                onOpenUrl = onOpenUrl,
                onBack = returnToRoot,
                modifier = modifier,
            )
            return
        }
        SettingsPage.DATA_SOURCES -> {
            DataSourcesSettingsScreen(
                onOpenUrl = onOpenUrl,
                onBack = returnToRoot,
                modifier = modifier,
            )
            return
        }
        SettingsPage.ROOT -> Unit
    }

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
        TextButton(onClick = { pageName = SettingsPage.HIDDEN_EQS.name }) {
            Text("Hidden EQs · ${hiddenCanonicalProfileIds.size}")
        }
        Text(
            text = "Hidden EQs remain in the living archive and in any existing My EQs collection; this setting changes ordinary library visibility only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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
        SectionTitle("Help & contribute")
        SettingsLinkRow(
            title = "Feedback & EQ submissions",
            description = "Report a problem, suggest an improvement, or submit an EQ source.",
            onClick = { onOpenUrl(EQ_LIBRARY_FEEDBACK_URL) },
        )

        SectionDivider()
        SectionTitle("About")
        SettingsLinkRow(
            title = "About EQ Library",
            description = "Version, privacy, source code, licensing, and independence.",
            onClick = { pageName = SettingsPage.ABOUT.name },
        )
        SettingsLinkRow(
            title = "Data sources & attribution",
            description = "OPRA, AutoEq, ParaEQ, creators, repositories, and public communities.",
            onClick = { pageName = SettingsPage.DATA_SOURCES.name },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AboutEqLibraryScreen(
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        SettingsSubpageHeader(title = "About EQ Library", onBack = onBack)
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Headphone selections, app settings, generated preset state, and conversion stay on this device. No account is required, and EQ Library contains no analytics or telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { onOpenUrl(EQ_LIBRARY_PROJECT_URL) }) { Text("Source code") }
            TextButton(onClick = { onOpenUrl(EQ_LIBRARY_PRIVACY_URL) }) { Text("Privacy") }
        }
        SectionDivider()
        SectionTitle("Open source & independence")
        Text(
            text = "EQ Library source code is Apache-2.0. Black Pearl protocol references are studied for observable device behavior only; GPL implementation code is not copied into this project.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "EQ Library is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, TRN, Poweramp, Wavelet, or headphone manufacturers.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DataSourcesSettingsScreen(
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        SettingsSubpageHeader(title = "Data sources & attribution", onBack = onBack)
        Text(
            text = "EQ Library combines attributed EQ data from OPRA and other qualified public sources. Creator and original-source information is preserved with each EQ whenever available.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        SectionDivider()
        SectionTitle("OPRA")
        Text(
            text = "OPRA is an open community-maintained headphone database and one source used by EQ Library. OPRA-sourced profile creators and source details remain attributed with their EQs where provided.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { onOpenUrl(OPRA_PROJECT_URL) }) { Text("Open OPRA project") }
            TextButton(onClick = { onOpenUrl(OPRA_DATA_LICENSE_URL) }) { Text("Data license") }
        }

        SectionDivider()
        SectionTitle("ParaEQ")
        Text(
            text = "The initial General EQ presets are sourced from the MIT-licensed ParaEQ built-in preset definitions. EQ Library preserves the source-authored preset labels and coefficients and keeps generated safety headroom separate from source preamp metadata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onOpenUrl(PARAEQ_PROJECT_URL) }) { Text("Open ParaEQ project") }

        SectionDivider()
        SectionTitle("Other attributed sources")
        Text(
            text = "The library can also contain attributed AutoEq, creator, repository, and public community EQ profiles. Each profile carries its available creator and source provenance so source-specific details remain inspectable without making any one source the app's identity.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSubpageHeader(
    title: String,
    onBack: () -> Unit,
) {
    TextButton(onClick = onBack) {
        androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
        Text("Settings", modifier = Modifier.padding(start = 4.dp))
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SettingsLinkRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private data class HiddenEqRow(
    val canonicalProfileId: String,
    val title: String,
    val subtitle: String,
)

@Composable
private fun HiddenEqSettingsScreen(
    catalogState: CatalogState,
    hiddenCanonicalProfileIds: Set<String>,
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var selectedIds by remember(hiddenCanonicalProfileIds) { mutableStateOf<Set<String>>(emptySet()) }
    val rows = remember(catalogState, hiddenCanonicalProfileIds) {
        hiddenEqRows(catalogState, hiddenCanonicalProfileIds)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("Settings", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            "Hidden EQs",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Hidden items are still archived. No rows are selected by default; choose the EQs you want to make visible again.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            TextButton(onClick = { selectedIds = rows.mapTo(mutableSetOf(), HiddenEqRow::canonicalProfileId) }) {
                Text("Select all")
            }
            TextButton(onClick = { selectedIds = emptySet() }) { Text("Select none") }
        }
        Button(
            onClick = {
                val toUnhide = selectedIds
                scope.launch {
                    onUnhideCanonicalProfiles(toUnhide)
                    selectedIds = emptySet()
                    onMessage("${toUnhide.size} ${if (toUnhide.size == 1) "EQ" else "EQs"} unhidden.")
                }
            },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) { Text("Unhide selected (${selectedIds.size})") }

        if (rows.isEmpty()) {
            Text(
                "No EQs are hidden.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = HiddenEqRow::canonicalProfileId) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (row.canonicalProfileId in selectedIds) {
                                    selectedIds - row.canonicalProfileId
                                } else {
                                    selectedIds + row.canonicalProfileId
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = row.canonicalProfileId in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + row.canonicalProfileId
                                else selectedIds - row.canonicalProfileId
                            },
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(row.title)
                            Text(
                                row.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun hiddenEqRows(
    catalogState: CatalogState,
    hiddenCanonicalProfileIds: Set<String>,
): List<HiddenEqRow> {
    if (hiddenCanonicalProfileIds.isEmpty()) return emptyList()
    val catalog = (catalogState as? CatalogState.Ready)?.catalog
    if (catalog == null) {
        return hiddenCanonicalProfileIds.sorted().map { id ->
            HiddenEqRow(id, "Archived EQ", "Catalog details unavailable · $id")
        }
    }

    val rows = mutableListOf<HiddenEqRow>()
    val found = mutableSetOf<String>()
    catalog.profiles
        .filter { it.canonicalProfileId in hiddenCanonicalProfileIds }
        .groupBy(OpraEqProfile::canonicalProfileId)
        .forEach { (canonicalId, revisions) ->
            val profile = revisions.firstOrNull { !it.isHistoricalRevision() } ?: revisions.first()
            val product = catalog.product(profile.productId)
            val vendor = product?.let { catalog.vendor(it.vendorId) }
            rows += HiddenEqRow(
                canonicalProfileId = canonicalId,
                title = product?.name ?: "Headphone EQ",
                subtitle = listOfNotNull(
                    vendor?.name,
                    profile.author?.takeIf(String::isNotBlank),
                    profile.details?.takeIf(String::isNotBlank),
                ).joinToString(" · ").ifBlank { canonicalId },
            )
            found += canonicalId
        }
    catalog.generalPresets
        .filter { it.canonicalProfileId in hiddenCanonicalProfileIds }
        .groupBy { it.canonicalProfileId }
        .forEach { (canonicalId, revisions) ->
            val preset = revisions.firstOrNull { it.isLatestRevision } ?: revisions.first()
            rows += HiddenEqRow(
                canonicalProfileId = canonicalId,
                title = preset.displayName,
                subtitle = listOfNotNull(
                    "General EQ",
                    preset.creator?.takeIf(String::isNotBlank),
                    preset.category.name.lowercase().replaceFirstChar(Char::titlecase),
                ).joinToString(" · "),
            )
            found += canonicalId
        }
    (hiddenCanonicalProfileIds - found).forEach { id ->
        rows += HiddenEqRow(id, "Archived EQ", "Catalog details unavailable · $id")
    }
    return rows.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
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