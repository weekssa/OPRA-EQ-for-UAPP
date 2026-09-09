package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
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
import com.weekssa.opraeqforuapp.ui.StringSetSaver
import kotlinx.coroutines.launch

private enum class LibrarySection(@param:StringRes val labelResId: Int) {
    HEADPHONES(R.string.browse_headphones),
    GENERAL(R.string.browse_general_eqs),
}

private enum class GeneralFilter(
    @param:StringRes val labelResId: Int,
    val category: GeneralEqCategory?,
) {
    ALL(R.string.filter_all, null),
    SOUND(R.string.filter_sound, GeneralEqCategory.SOUND),
    GENRE(R.string.filter_genre, GeneralEqCategory.GENRE),
    UTILITY(R.string.filter_utility, GeneralEqCategory.UTILITY),
}

@Composable
fun BrowseOpraScreen(
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
    exportTargets: ExportTargetPreferences,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    favoriteProfileIds: Set<String>,
    savedGeneralPresetIds: Set<String> = emptySet(),
    hiddenCanonicalProfileIds: Set<String> = emptySet(),
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
    onSaveGeneralPresets: suspend (List<GeneralEqPreset>) -> Int = { 0 },
    onHideCanonicalProfiles: suspend (Set<String>) -> Unit = {},
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
            title = stringResource(R.string.catalog_loading),
            showProgress = true,
            modifier = modifier,
        )
        is CatalogState.Unavailable -> CatalogUnavailableContent(
            title = stringResource(R.string.catalog_not_downloaded),
            detail = unavailableCatalogMessage(catalogState.reason),
            onRetry = onRefreshCatalog,
            modifier = modifier,
        )
        is CatalogState.Ready -> {
            val fullCatalog = catalogState.catalog
            val catalog = remember(fullCatalog, hiddenCanonicalProfileIds) {
                fullCatalog.excludingHiddenCanonicalProfiles(hiddenCanonicalProfileIds)
            }
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
                    PrimaryTabRow(selectedTabIndex = selectedSectionIndex) {
                        LibrarySection.entries.forEachIndexed { index, section ->
                            Tab(
                                selected = selectedSectionIndex == index,
                                onClick = {
                                    selectedSectionIndex = index
                                    selectedVendorId = null
                                    selectedProductId = null
                                    searchQuery = ""
                                },
                                text = { Text(stringResource(section.labelResId)) },
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
                        onHideCanonicalProfile = { canonicalProfileId ->
                            onHideCanonicalProfiles(setOf(canonicalProfileId))
                        },
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
                        modifier = Modifier.weight(1f),
                    )
                    else -> GeneralEqBrowse(
                        catalog = catalog,
                        searchQuery = searchQuery,
                        selectedFilterIndex = selectedGeneralFilterIndex,
                        savedPresetIds = savedGeneralPresetIds,
                        onSearchQueryChange = { searchQuery = it },
                        onFilterSelected = { selectedGeneralFilterIndex = it },
                        onSavePresets = onSaveGeneralPresets,
                        onHideCanonicalProfiles = onHideCanonicalProfiles,
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
            labelResId = R.string.search_headphones,
        )

        if (searchQuery.isBlank()) {
            val vendors = catalog.vendors.sortedBy { it.name.lowercase() }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (vendors.isEmpty()) {
                    item(key = "no-headphones") {
                        Text(
                            text = stringResource(R.string.no_headphone_eqs),
                            modifier = Modifier.padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                items(vendors, key = { it.id }) { vendor ->
                    val modelCount = catalog.productsForVendor(vendor.id).size
                    ListItem(
                        headlineContent = { Text(vendor.name) },
                        supportingContent = {
                            Text(pluralStringResource(R.plurals.model_count, modelCount, modelCount))
                        },
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
                    text = stringResource(R.string.no_headphones_found),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.product.id }) { result ->
                        val managed = managedByProduct[result.product.id]
                        val selectedText = managed?.let {
                            pluralStringResource(R.plurals.selected_count, it.selectedProfileCount, it.selectedProfileCount)
                        }
                        val profileText = pluralStringResource(
                            R.plurals.profile_count,
                            result.profileCount,
                            result.profileCount,
                        )
                        ListItem(
                            headlineContent = { Text(result.product.name) },
                            supportingContent = {
                                Text(
                                    if (selectedText != null) {
                                        stringResource(R.string.count_pair, result.vendor.name, selectedText)
                                    } else {
                                        result.vendor.name
                                    },
                                )
                            },
                            trailingContent = { Text(profileText) },
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
    onSavePresets: suspend (List<GeneralEqPreset>) -> Int,
    onHideCanonicalProfiles: suspend (Set<String>) -> Unit,
    onMessage: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectedFilter = GeneralFilter.entries[selectedFilterIndex]
    var batchSelectedIds by rememberSaveable(
        catalog,
        stateSaver = StringSetSaver,
    ) { mutableStateOf(emptySet<String>()) }
    val matching = remember(catalog.generalPresets, searchQuery, selectedFilter) {
        catalog.searchGeneralPresets(searchQuery)
            .filter { preset -> selectedFilter.category == null || preset.category == selectedFilter.category }
    }
    val selectedPresets = catalog.generalPresets.filter { it.id in batchSelectedIds }
    val selectedCanonicalIds = selectedPresets.mapTo(mutableSetOf(), GeneralEqPreset::canonicalProfileId)
    val savedMessage = pluralStringResource(
        R.plurals.general_eq_saved_message,
        selectedPresets.size,
        selectedPresets.size,
    )
    val hiddenMessage = pluralStringResource(
        R.plurals.eq_hidden_message,
        selectedCanonicalIds.size,
        selectedCanonicalIds.size,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            labelResId = R.string.search_general_eqs,
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
                    label = { Text(stringResource(filter.labelResId)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.general_eq_standalone_note),
            modifier = Modifier.padding(bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { batchSelectedIds = batchSelectedIds + matching.map(GeneralEqPreset::id) },
                enabled = matching.isNotEmpty(),
            ) { Text(stringResource(R.string.action_select_all)) }
            TextButton(
                onClick = { batchSelectedIds = batchSelectedIds - matching.map(GeneralEqPreset::id).toSet() },
                enabled = matching.isNotEmpty(),
            ) { Text(stringResource(R.string.action_select_none)) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val toSave = selectedPresets.toList()
                    scope.launch {
                        onSavePresets(toSave)
                        batchSelectedIds = emptySet()
                        onMessage(savedMessage)
                    }
                },
                enabled = selectedPresets.isNotEmpty(),
            ) { Text(stringResource(R.string.save_selected_count, selectedPresets.size)) }
            OutlinedButton(
                onClick = {
                    val canonicalIds = selectedCanonicalIds.toSet()
                    scope.launch {
                        onHideCanonicalProfiles(canonicalIds)
                        batchSelectedIds = emptySet()
                        onMessage(hiddenMessage)
                    }
                },
                enabled = selectedPresets.isNotEmpty(),
            ) { Text(stringResource(R.string.action_hide_selected)) }
        }

        if (matching.isEmpty()) {
            Text(
                text = if (catalog.generalPresets.isEmpty()) {
                    stringResource(R.string.general_eq_none_visible)
                } else {
                    stringResource(R.string.general_eq_none_match)
                },
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(matching, key = GeneralEqPreset::id) { preset ->
                    ListItem(
                        headlineContent = { Text(preset.displayName) },
                        supportingContent = {
                            Column {
                                Text(
                                    listOfNotNull(
                                        preset.creator?.takeIf(String::isNotBlank),
                                        preset.soundImpactSummary?.takeIf(String::isNotBlank),
                                    ).joinToString(" · ").ifBlank {
                                        stringResource(R.string.general_parametric_eq)
                                    },
                                )
                                if (preset.id in savedPresetIds) {
                                    Text(
                                        stringResource(R.string.saved_in_my_eqs_output),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (!preset.isVerified) {
                                    Text(
                                        stringResource(R.string.community_submission_unverified),
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                preset.sourceUrl?.let { sourceUrl ->
                                    TextButton(onClick = { onOpenUrl(sourceUrl) }) {
                                        Text(stringResource(R.string.action_source))
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Checkbox(
                                checked = preset.id in batchSelectedIds,
                                onCheckedChange = { checked ->
                                    batchSelectedIds = if (checked) {
                                        batchSelectedIds + preset.id
                                    } else {
                                        batchSelectedIds - preset.id
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
    @StringRes labelResId: Int,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        singleLine = true,
        label = { Text(stringResource(labelResId)) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_search_content_description),
                    )
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
            Text(stringResource(R.string.manufacturers), modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = vendor.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(products, key = { it.id }) { product ->
                val managed = managedByProduct[product.id]
                val profileCount = catalog.profileCount(product.id)
                val profileText = pluralStringResource(R.plurals.profile_count, profileCount, profileCount)
                val selectedText = managed?.let {
                    pluralStringResource(R.plurals.selected_count, it.selectedProfileCount, it.selectedProfileCount)
                }
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = {
                        Text(
                            if (selectedText == null) {
                                profileText
                            } else {
                                stringResource(R.string.count_pair, profileText, selectedText)
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
        onRetry?.let { retry ->
            Button(onClick = retry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.action_try_again))
            }
        }
    }
}
