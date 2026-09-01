package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.ui.components.OpraAttribution
import kotlinx.coroutines.launch

private const val EQ_SOURCE_SUBMISSION_URL =
    "https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/new?template=submit_eq_source.yml"

private enum class LibrarySection(val label: String) {
    HEADPHONES("Headphones"),
    GENERAL("General EQs"),
}

private enum class GeneralFilter(val label: String, val category: GeneralEqCategory?) {
    ALL("All", null),
    SOUND("Sound", GeneralEqCategory.SOUND),
    GENRE("Genre", GeneralEqCategory.GENRE),
    UTILITY("Utility", GeneralEqCategory.UTILITY),
}

@Composable
fun BrowseOpraScreen(
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
    exportTargets: ExportTargetPreferences,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    favoriteProfileIds: Set<String>,
    savedGeneralPresetIds: Set<String> = emptySet(),
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
    onToggleGeneralPreset: suspend (GeneralEqPreset) -> Boolean = { false },
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onExportProduct: (String) -> Unit,
    onMessage: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBackFromRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedVendorId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedGeneralFilterIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedSection = LibrarySection.entries[selectedSectionIndex]

    BackHandler {
        when {
            selectedProductId != null -> selectedProductId = null
            selectedVendorId != null -> selectedVendorId = null
            searchQuery.isNotBlank() -> searchQuery = ""
            else -> onBackFromRoot()
        }
    }

    when (catalogState) {
        CatalogState.Loading -> CatalogUnavailableContent(
            title = "Loading EQ Library…",
            showProgress = true,
            modifier = modifier,
        )
        is CatalogState.Unavailable -> CatalogUnavailableContent(
            title = "EQ Library catalog not downloaded yet",
            detail = unavailableCatalogMessage(catalogState.reason),
            onRetry = onRefreshCatalog,
            modifier = modifier,
        )
        is CatalogState.Ready -> {
            val catalog = catalogState.catalog
            val product = if (selectedSection == LibrarySection.HEADPHONES) {
                selectedProductId?.let(catalog::product)
            } else {
                null
            }
            val vendor = if (selectedSection == LibrarySection.HEADPHONES) {
                selectedVendorId?.let(catalog::vendor)
            } else {
                null
            }
            val managedByProduct = remember(managedHeadphones) {
                managedHeadphones.associateBy(ManagedHeadphoneRecord::productId)
            }

            Column(modifier = modifier.fillMaxSize()) {
                if (product == null && vendor == null) {
                    TabRow(selectedTabIndex = selectedSectionIndex) {
                        LibrarySection.entries.forEachIndexed { index, section ->
                            Tab(
                                selected = selectedSectionIndex == index,
                                onClick = {
                                    selectedSectionIndex = index
                                    selectedVendorId = null
                                    selectedProductId = null
                                    searchQuery = ""
                                },
                                text = { Text(section.label) },
                            )
                        }
                    }
                }

                when {
                    product != null -> ProfileSelectionEditor(
                        catalog = catalog,
                        product = product,
                        profileVisibility = profileVisibility,
                        exportTargets = exportTargets,
                        favoriteProfileIds = favoriteProfileIds,
                        onToggleFavorite = onToggleFavorite,
                        onLoadManagedHeadphone = onLoadManagedHeadphone,
                        onSaveSelection = onSaveSelection,
                        onRemoveHeadphone = onRemoveHeadphone,
                        onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
                        onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
                        onExportProduct = onExportProduct,
                        onMessage = onMessage,
                        onOpenUrl = onOpenUrl,
                        onBack = { selectedProductId = null },
                        modifier = Modifier.weight(1f),
                    )
                    vendor != null -> VendorProducts(
                        catalog = catalog,
                        vendorId = vendor.id,
                        managedByProduct = managedByProduct,
                        onProductSelected = { selectedProductId = it.id },
                        onBack = {
                            selectedVendorId = null
                            selectedProductId = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    selectedSection == LibrarySection.HEADPHONES -> HeadphoneBrowseRoot(
                        catalog = catalog,
                        managedByProduct = managedByProduct,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onVendorSelected = { selectedVendorId = it },
                        onProductSelected = { selectedProductId = it.id },
                        onOpenUrl = onOpenUrl,
                        modifier = Modifier.weight(1f),
                    )
                    else -> GeneralEqBrowse(
                        catalog = catalog,
                        searchQuery = searchQuery,
                        selectedFilterIndex = selectedGeneralFilterIndex,
                        savedPresetIds = savedGeneralPresetIds,
                        onSearchQueryChange = { searchQuery = it },
                        onFilterSelected = { selectedGeneralFilterIndex = it },
                        onTogglePreset = onToggleGeneralPreset,
                        onMessage = onMessage,
                        onOpenUrl = onOpenUrl,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadphoneBrowseRoot(
    catalog: OpraCatalog,
    managedByProduct: Map<String, ManagedHeadphoneRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVendorSelected: (String) -> Unit,
    onProductSelected: (OpraProduct) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = "Search headphones…",
        )

        if (searchQuery.isBlank()) {
            val vendors = catalog.vendors.sortedBy { it.name.lowercase() }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "source-attribution") {
                    Text(
                        text = "EQ Library combines supported sources. Selecting an output changes compatibility and export behavior, never which valid library curves are visible.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OpraAttribution(
                        onOpenUrl = onOpenUrl,
                        compact = true,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    TextButton(
                        onClick = { onOpenUrl(EQ_SOURCE_SUBMISSION_URL) },
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text("Submit an EQ source")
                    }
                    HorizontalDivider()
                }
                if (vendors.isEmpty()) {
                    item(key = "no-headphones") {
                        Text(
                            text = "No headphone EQs are available in the saved catalog.",
                            modifier = Modifier.padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                items(vendors, key = { it.id }) { vendor ->
                    val modelCount = catalog.productsForVendor(vendor.id).size
                    ListItem(
                        headlineContent = { Text(vendor.name) },
                        supportingContent = { Text("$modelCount models") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onVendorSelected(vendor.id) },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            val results = catalog.searchProducts(searchQuery)
            if (results.isEmpty()) {
                Text(
                    text = "No headphones found",
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.product.id }) { result ->
                        val managed = managedByProduct[result.product.id]
                        ListItem(
                            headlineContent = { Text(result.product.name) },
                            supportingContent = {
                                Text(
                                    if (managed != null) {
                                        "${result.vendor.name} · ${managed.selectedProfileCount} selected"
                                    } else {
                                        result.vendor.name
                                    },
                                )
                            },
                            trailingContent = { Text("${result.profileCount} profiles") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onProductSelected(result.product) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralEqBrowse(
    catalog: OpraCatalog,
    searchQuery: String,
    selectedFilterIndex: Int,
    savedPresetIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (Int) -> Unit,
    onTogglePreset: suspend (GeneralEqPreset) -> Boolean,
    onMessage: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectedFilter = GeneralFilter.entries[selectedFilterIndex]
    val matching = remember(catalog.generalPresets, searchQuery, selectedFilter) {
        catalog.searchGeneralPresets(searchQuery)
            .filter { preset -> selectedFilter.category == null || preset.category == selectedFilter.category }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = "Search General EQs…",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GeneralFilter.entries.forEachIndexed { index, filter ->
                FilterChip(
                    selected = selectedFilterIndex == index,
                    onClick = { onFilterSelected(index) },
                    label = { Text(filter.label) },
                )
            }
        }
        Text(
            text = "General EQs are standalone presets; v0.3 does not layer them on top of headphone correction EQs.",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (matching.isEmpty()) {
            Text(
                text = if (catalog.generalPresets.isEmpty()) {
                    "No General EQs are published in the current saved catalog yet."
                } else {
                    "No General EQs match this search and filter."
                },
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(matching, key = GeneralEqPreset::id) { preset ->
                    val selected = preset.id in savedPresetIds
                    ListItem(
                        headlineContent = { Text(preset.displayName) },
                        supportingContent = {
                            Column {
                                Text(
                                    listOfNotNull(
                                        preset.creator?.takeIf(String::isNotBlank),
                                        preset.soundImpactSummary?.takeIf(String::isNotBlank),
                                    ).joinToString(" · ").ifBlank { "General parametric EQ" },
                                )
                                if (!preset.isVerified) {
                                    Text(
                                        "Community submission — not independently verified.",
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                preset.sourceUrl?.let { sourceUrl ->
                                    TextButton(onClick = { onOpenUrl(sourceUrl) }) {
                                        Text("Source")
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    scope.launch {
                                        val nowSelected = onTogglePreset(preset)
                                        onMessage(
                                            if (nowSelected) {
                                                "${preset.displayName} added to My EQs for this output."
                                            } else {
                                                "${preset.displayName} removed from My EQs for this output."
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        singleLine = true,
        label = { Text(label) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun VendorProducts(
    catalog: OpraCatalog,
    vendorId: String,
    managedByProduct: Map<String, ManagedHeadphoneRecord>,
    onProductSelected: (OpraProduct) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val vendor = catalog.vendor(vendorId) ?: return
    val products = catalog.productsForVendor(vendorId)
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("Manufacturers", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = vendor.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(products, key = { it.id }) { product ->
                val managed = managedByProduct[product.id]
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = {
                        Text(
                            if (managed != null) {
                                "${catalog.profileCount(product.id)} profiles · ${managed.selectedProfileCount} selected"
                            } else {
                                "${catalog.profileCount(product.id)} profiles"
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onProductSelected(product) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CatalogUnavailableContent(
    title: String,
    detail: String? = null,
    showProgress: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 48.dp, bottom = 20.dp))
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = if (showProgress) 0.dp else 48.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        detail?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        onRetry?.let {
            Button(onClick = it, modifier = Modifier.padding(top = 16.dp)) {
                Text("Try again")
            }
        }
    }
}
