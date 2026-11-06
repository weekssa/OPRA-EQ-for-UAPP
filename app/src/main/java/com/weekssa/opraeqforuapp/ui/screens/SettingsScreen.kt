package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.OutputCategory
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.OutputBehavior
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
    HELP,
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
    onOutputBehaviorChange: (OutputBehavior) -> Unit,
    onExportTargetChange: (ExportDevice, Boolean) -> Unit,
    onActiveExportTargetChange: (ExportDevice) -> Unit,
    onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,
    onDirectFiioJa11FlashEnabledChange: (Boolean) -> Unit,
    onDirectEw300FlashEnabledChange: (Boolean) -> Unit,
    ew300ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    onConnectEw300: () -> Unit = {},
    hiddenCanonicalProfileIds: Set<String>,
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.ROOT.name) }
    var defaultTargetPickerOpen by rememberSaveable { mutableStateOf(false) }
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
        SettingsPage.HELP -> {
            HelpSettingsScreen(
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

    if (defaultTargetPickerOpen) {
        AlertDialog(
            onDismissRequest = { defaultTargetPickerOpen = false },
            title = { Text("Default target") },
            text = {
                Column {
                    ExportDevice.selectableOutputs
                        .filter(appPreferences.manualExportTargets::isSelected)
                        .forEach { device ->
                            ThemeOption(
                                title = device.displayName,
                                selected = device == appPreferences.manualExportTargets.activeTarget,
                                onSelected = {
                                    onActiveExportTargetChange(device)
                                    defaultTargetPickerOpen = false
                                },
                            )
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { defaultTargetPickerOpen = false }) { Text("Done") }
            },
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (BuildConfig.EW300_ENGINEERING_CONTROLS) {
            Ew300ParitySetup(
                appPreferences = appPreferences,
                ew300ConnectionState = ew300ConnectionState,
                onUseEw300Target = {
                    onExportTargetChange(ExportDevice.SIMGOT_EW300, true)
                    onActiveExportTargetChange(ExportDevice.SIMGOT_EW300)
                    onMessage("SIMGOT EW300 DSP is now the active EQ target in this parity build.")
                },
                onEnableDirectFlash = {
                    onDirectEw300FlashEnabledChange(true)
                    onMessage("Direct Flash is enabled for SIMGOT EW300 DSP.")
                },
                onConnect = onConnectEw300,
            )
        }

        SectionTitle("EQ target")
        ThemeOption(
            title = "Automatic",
            description = "Use a connected DAC when available.",
            selected = appPreferences.outputBehavior == OutputBehavior.Automatic,
            onSelected = { onOutputBehaviorChange(OutputBehavior.Automatic) },
        )
        ThemeOption(
            title = "Manual",
            description = "Always use the default target.",
            selected = appPreferences.outputBehavior == OutputBehavior.Manual,
            onSelected = { onOutputBehaviorChange(OutputBehavior.Manual) },
        )
        SettingsValueRow(
            label = "Current target",
            value = appPreferences.exportTargets.activeTarget.displayName,
        )
        SettingsValueRow(
            label = "Default target",
            value = appPreferences.manualExportTargets.activeTarget.displayName,
            actionLabel = "Change",
            onAction = { defaultTargetPickerOpen = true },
        )

        SectionDivider()
        SectionTitle("Outputs")
        OutputCategory.entries.forEach { category ->
            val devices = ExportDevice.selectableOutputs.filter { it.category == category }
            if (devices.isNotEmpty()) {
                Text(
                    text = category.heading,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                devices.forEach { device ->
                    val directFlashChecked = when (device) {
                        ExportDevice.BLACK_PEARL -> appPreferences.directBlackPearlFlashEnabled
                        ExportDevice.FIIO_JA11 -> appPreferences.directFiioJa11FlashEnabled
                        ExportDevice.SIMGOT_EW300 -> appPreferences.directEw300FlashEnabled
                        else -> null
                    }
                    val onDirectFlashChange: ((Boolean) -> Unit)? = when (device) {
                        ExportDevice.BLACK_PEARL -> onDirectBlackPearlFlashEnabledChange
                        ExportDevice.FIIO_JA11 -> onDirectFiioJa11FlashEnabledChange
                        ExportDevice.SIMGOT_EW300 -> onDirectEw300FlashEnabledChange
                        else -> null
                    }
                    OutputOption(
                        device = device,
                        checked = appPreferences.manualExportTargets.isSelected(device),
                        onCheckedChange = { enabled -> onExportTargetChange(device, enabled) },
                        directFlashChecked = directFlashChecked,
                        onDirectFlashChange = onDirectFlashChange,
                    )
                }
            }
        }
        Text(
            text = "Direct Flash writes only when you tap Flash.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionDivider()
        SectionTitle("Library")
        SettingsValueRow(
            label = "Folder",
            value = appPreferences.exportTreeLabel ?: "Not chosen",
            actionLabel = if (appPreferences.exportTreeUri == null) "Choose" else "Change",
            onAction = onChangeExportFolder,
        )
        SettingsValueRow(
            label = "Hidden EQs",
            value = hiddenCanonicalProfileIds.size.toString(),
            actionLabel = "View",
            onAction = { pageName = SettingsPage.HIDDEN_EQS.name },
        )
        when (catalogState) {
            CatalogState.Loading -> SettingsValueRow(label = "Catalog", value = "Loading…")
            is CatalogState.Unavailable -> {
                SettingsValueRow(
                    label = "Catalog",
                    value = "Offline",
                    actionLabel = "Retry",
                    onAction = onRefreshCatalog,
                )
                Text(
                    text = eqLibraryCatalogFailureMessage(catalogState.reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is CatalogState.Ready -> SettingsValueRow(
                label = "Catalog",
                value = if (catalogState.isRefreshing) {
                    "Refreshing…"
                } else {
                    "Updated " + formatCatalogTime(catalogState.lastSuccessfulRefreshMillis)
                },
                actionLabel = "Refresh",
                onAction = onRefreshCatalog,
            )
        }

        SectionDivider()
        SectionTitle("Appearance")
        ThemeOption(
            title = "System",
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
        SectionTitle("About")
        val latestVersion = appPreferences.updates.latestVersion.takeIf { BuildConfig.UPDATE_CHECKS_ENABLED }
        val updateAvailable = latestVersion != null &&
            SemVer.parse(latestVersion)?.let { latest ->
                SemVer.parse(BuildConfig.VERSION_NAME)?.let { installed -> latest > installed }
            } == true
        SettingsValueRow(
            label = "Version",
            value = BuildConfig.VERSION_NAME,
            actionLabel = if (BuildConfig.UPDATE_CHECKS_ENABLED) "Check" else null,
            onAction = if (BuildConfig.UPDATE_CHECKS_ENABLED) onCheckForUpdates else null,
        )
        if (BuildConfig.UPDATE_CHECKS_ENABLED && updateAvailable) {
            SettingsValueRow(
                label = "Update",
                value = latestVersion.orEmpty() + " available",
                actionLabel = "Get",
                onAction = onGetUpdate,
            )
        }
        SettingsLinkRow(
            title = "Help",
            onClick = { pageName = SettingsPage.HELP.name },
        )
        SettingsLinkRow(
            title = "About EQ Library",
            onClick = { pageName = SettingsPage.ABOUT.name },
        )
        SettingsLinkRow(
            title = "Data sources",
            onClick = { pageName = SettingsPage.DATA_SOURCES.name },
        )
        SettingsLinkRow(
            title = "Feedback & EQ submissions",
            onClick = { onOpenUrl(EQ_LIBRARY_FEEDBACK_URL) },
        )

        Spacer(Modifier.height(24.dp))
    }
}

private fun outputSubtitle(device: ExportDevice): String = when (device) {
    ExportDevice.BLACK_PEARL -> "10-band PEQ"
    ExportDevice.FIIO_JA11 -> "5-band PEQ"
    ExportDevice.SIMGOT_EW300 -> "5-band Peak PEQ"
    ExportDevice.UAPP -> "ToneBoosters · 10 bands"
    ExportDevice.EQUALIZER_APO -> "Parametric EQ"
    ExportDevice.POWERAMP -> "Parametric · 64 bands"
    ExportDevice.TOPPING_TUNE -> "Parametric · 10 bands"
    ExportDevice.WAVELET -> "GraphicEQ · 127 points"
    ExportDevice.EASY_EFFECTS -> "Parametric · 32 bands"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Parametric text"
    ExportDevice.UNIVERSAL_GRAPHIC_EQ -> "GraphicEQ · 127 points"
    else -> device.settingsSubtitle
}

@Composable
private fun OutputOption(
    device: ExportDevice,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    directFlashChecked: Boolean?,
    onDirectFlashChange: ((Boolean) -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.displayName)
                Text(
                    text = outputSubtitle(device),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (checked && directFlashChecked != null && onDirectFlashChange != null) {
            Column(modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = directFlashChecked,
                            role = Role.Switch,
                            onValueChange = onDirectFlashChange,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Direct Flash",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = directFlashChecked,
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSettingsScreen(
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
        SettingsSubpageHeader(title = "Help", onBack = onBack)
        SectionTitle("Save and organize")
        Text(
            "My EQs keeps headphone selections, General EQs, and Personal EQ imports on this device. Saving does not export a file or change connected hardware. Favorites are discovery shortcuts in EQ Library.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionDivider()
        SectionTitle("Output target and export")
        Text(
            "Your output target controls how EQ Library prepares an EQ for export or a supported hardware action. It does not route Android audio. Export writes a file to the folder you choose; to use it in USB Audio Player PRO, import that file in UAPP yourself.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionDivider()
        SectionTitle("My DAC")
        Text(
            "My DAC opens when a supported device is recognized during this app session. Device state may be current, stale, or unavailable; check the status shown on that screen before choosing a device action. Flash and Reset always show a review step before a write.",
            style = MaterialTheme.typography.bodyMedium,
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
            text = "EQ Library source code is Apache-2.0. External Black Pearl and FiiO tools are studied only for observable device/protocol behavior; copyleft implementation code is not copied into this project.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "EQ Library is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, TRN, FiiO, EasyEffects, Equalizer APO, Poweramp, Wavelet, or headphone manufacturers.",
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
private fun SettingsValueRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    description: String = "",
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
                            .toggleable(
                                value = row.canonicalProfileId in selectedIds,
                                role = Role.Checkbox,
                            ) { checked ->
                                selectedIds = if (checked) selectedIds + row.canonicalProfileId
                                else selectedIds - row.canonicalProfileId
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = row.canonicalProfileId in selectedIds,
                            onCheckedChange = null,
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
        modifier = Modifier.semantics { heading() }.padding(bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelected)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
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

private fun eqLibraryCatalogFailureMessage(reason: CatalogRefreshFailureReason): String = when (reason) {
    CatalogRefreshFailureReason.Network -> "Couldn’t refresh EQ Library. Check your connection; if you have a saved library, it remains available."
    CatalogRefreshFailureReason.InvalidCatalog -> "The downloaded EQ Library catalog could not be validated. Your last known-good library remains unchanged."
    CatalogRefreshFailureReason.Storage -> "EQ Library couldn’t save the refreshed catalog on this device."
}

@Composable
private fun Ew300ParitySetup(
    appPreferences: AppPreferences,
    ew300ConnectionState: Kt02h20ConnectionState,
    onUseEw300Target: () -> Unit,
    onEnableDirectFlash: () -> Unit,
    onConnect: () -> Unit,
) {
    val targetReady = appPreferences.exportTargets.activeTarget == ExportDevice.SIMGOT_EW300
    val flashReady = appPreferences.directEw300FlashEnabled
    val connected = ew300ConnectionState is Kt02h20ConnectionState.Connected
    val allReady = targetReady && flashReady && connected
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            "Parity setup · Flash to SIMGOT EW300",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "This panel exists only in the ew300parity test build. A library EQ can be flashed only when all three steps show Done; nothing here is enabled silently.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        ParitySetupStep(
            index = 1,
            title = "Active output",
            value = if (targetReady) {
                "SIMGOT EW300 DSP · Done"
            } else {
                "Current: ${appPreferences.exportTargets.activeTarget.displayName} · not the EW300"
            },
            actionLabel = if (targetReady) null else "Use EW300",
            onAction = onUseEw300Target,
        )
        ParitySetupStep(
            index = 2,
            title = "Direct Flash",
            value = if (flashReady) "Enabled · Done" else "Off · Flash stays disabled",
            actionLabel = if (flashReady) null else "Enable Direct Flash",
            onAction = onEnableDirectFlash,
        )
        ParitySetupStep(
            index = 3,
            title = "Connection",
            value = when (ew300ConnectionState) {
                is Kt02h20ConnectionState.Connected -> "Connected · Done"
                is Kt02h20ConnectionState.Connecting -> "Connecting…"
                is Kt02h20ConnectionState.PermissionRequired -> "USB permission required"
                is Kt02h20ConnectionState.Error -> "Error · ${ew300ConnectionState.message}"
                is Kt02h20ConnectionState.Disconnected -> "Not connected"
            },
            actionLabel = when {
                connected -> null
                ew300ConnectionState is Kt02h20ConnectionState.Connecting -> null
                else -> "Connect EW300"
            },
            onAction = onConnect,
        )
        Text(
            if (allReady) {
                "Ready · Flash actions are enabled for saved EQs and managed profiles."
            } else {
                "Flash stays disabled until every step shows Done. The reason is shown next to each Flash action."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (allReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ParitySetupStep(
    index: Int,
    title: String,
    value: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$index. $title", style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

