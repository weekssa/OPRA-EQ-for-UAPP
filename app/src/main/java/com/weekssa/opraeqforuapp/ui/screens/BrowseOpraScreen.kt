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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.visibilityCategory
import com.weekssa.opraeqforuapp.domain.managed.DEFAULT_AUTO_INCLUDE_NEW_PROFILES
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.defaultStagedSelectedProfileIds
import com.weekssa.opraeqforuapp.domain.managed.selectableProfileIds
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import kotlinx.coroutines.launch

@Composable
fun BrowseOpraScreen(
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedVendorId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(
        enabled = selectedProductId != null || selectedVendorId != null || searchQuery.isNotBlank(),
    ) {
        when {
            selectedProductId != null -> selectedProductId = null
            selectedVendorId != null -> selectedVendorId = null
            else -> searchQuery = ""
        }
    }

    when (catalogState) {
        CatalogState.Loading -> CatalogUnavailableContent(
            title = "Loading OPRA catalog…",
            showProgress = true,
            modifier = modifier,
        )
        is CatalogState.Unavailable -> CatalogUnavailableContent(
            title = "OPRA catalog not downloaded yet",
            detail = unavailableCatalogMessage(catalogState.reason),
            onRetry = onRefreshCatalog,
            modifier = modifier,
        )
        is CatalogState.Ready -> CatalogBrowser(
            catalog = catalogState.catalog,
            profileVisibility = profileVisibility,
            managedHeadphones = managedHeadphones,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedVendorId = selectedVendorId,
            onVendorSelected = {
                selectedVendorId = it
                selectedProductId = null
            },
            selectedProductId = selectedProductId,
            onProductSelected = { product -> selectedProductId = product.id },
            onBackToVendors = {
                selectedVendorId = null
                selectedProductId = null
            },
            onBackFromProduct = { selectedProductId = null },
            onLoadManagedHeadphone = onLoadManagedHeadphone,
            onSaveSelection = onSaveSelection,
            onRemoveHeadphone = onRemoveHeadphone,
            modifier = modifier,
        )
    }
}

@Composable
private fun CatalogBrowser(
    catalog: OpraCatalog,
    profileVisibility: ProfileVisibilityPreferences,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedVendorId: String?,
    onVendorSelected: (String) -> Unit,
    selectedProductId: String?,
    onProductSelected: (OpraProduct) -> Unit,
    onBackToVendors: () -> Unit,
    onBackFromProduct: () -> Unit,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    modifier: Modifier,
) {
    val product = selectedProductId?.let(catalog::product)
    val vendor = selectedVendorId?.let(catalog::vendor)
    val managedByProduct = remember(managedHeadphones) { managedHeadphones.associateBy { it.productId } }

    when {
        product != null -> ProductProfiles(
            catalog = catalog,
            product = product,
            profileVisibility = profileVisibility,
            onLoadManagedHeadphone = onLoadManagedHeadphone,
            onSaveSelection = onSaveSelection,
            onRemoveHeadphone = onRemoveHeadphone,
            onBack = onBackFromProduct,
            modifier = modifier,
        )
        vendor != null -> VendorProducts(
            catalog = catalog,
            vendorId = vendor.id,
            managedByProduct = managedByProduct,
            onProductSelected = onProductSelected,
            onBack = onBackToVendors,
            modifier = modifier,
        )
        else -> BrowseRoot(
            catalog = catalog,
            managedByProduct = managedByProduct,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onVendorSelected = onVendorSelected,
            onProductSelected = onProductSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun BrowseRoot(
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true,
            label = { Text("Search headphones…") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
        )

        if (searchQuery.isBlank()) {
            val vendors = catalog.vendors.sortedBy { it.name.lowercase() }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
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
private fun ProductProfiles(
    catalog: OpraCatalog,
    product: OpraProduct,
    profileVisibility: ProfileVisibilityPreferences,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val vendor = catalog.vendor(product.vendorId)
    val profiles = catalog.profilesForProduct(product.id)
    val visibleProfiles = profiles.filter { profile ->
        val category = profile.assessCompatibility().category.visibilityCategory()
        profileVisibility.isVisible(category)
    }
    val hiddenCount = profiles.size - visibleProfiles.size
    val scope = rememberCoroutineScope()

    var initialized by remember(product.id) { mutableStateOf(false) }
    var wasManaged by remember(product.id) { mutableStateOf(false) }
    var stagedSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var baselineSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var autoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var baselineAutoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var showDiscardDialog by remember(product.id) { mutableStateOf(false) }
    var showRemovalDialog by remember(product.id) { mutableStateOf(false) }
    var showZeroSelectionDialog by remember(product.id) { mutableStateOf(false) }
    var incompatibilityExplanation by remember(product.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(product.id) {
        val managed = onLoadManagedHeadphone(product.id)
        val selected = if (managed == null) {
            defaultStagedSelectedProfileIds(profiles)
        } else {
            val selectionState = managed.toSelectionState()
            profiles.filter(selectionState::isSelected).mapTo(mutableSetOf(), OpraEqProfile::id)
        }
        wasManaged = managed != null
        stagedSelectedIds = selected
        baselineSelectedIds = selected
        autoInclude = managed?.autoIncludeNewProfiles ?: DEFAULT_AUTO_INCLUDE_NEW_PROFILES
        baselineAutoInclude = autoInclude
        initialized = true
    }

    val dirty = initialized &&
        (stagedSelectedIds != baselineSelectedIds || autoInclude != baselineAutoInclude)
    val removedProfileCount = baselineSelectedIds.count { it !in stagedSelectedIds }

    fun performSave() {
        scope.launch {
            if (stagedSelectedIds.isEmpty()) {
                onRemoveHeadphone(product.id)
                wasManaged = false
            } else {
                onSaveSelection(product.id, stagedSelectedIds, autoInclude)
                wasManaged = true
            }
            baselineSelectedIds = stagedSelectedIds
            baselineAutoInclude = autoInclude
        }
    }

    fun requestSave() {
        when {
            stagedSelectedIds.isEmpty() -> showZeroSelectionDialog = true
            wasManaged && removedProfileCount > 0 -> showRemovalDialog = true
            else -> performSave()
        }
    }

    fun requestBack() {
        if (dirty) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = dirty) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved profile-selection changes will be discarded.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    if (showRemovalDialog) {
        AlertDialog(
            onDismissRequest = { showRemovalDialog = false },
            title = { Text("Remove $removedProfileCount profiles?") },
            text = { Text("Those profiles will no longer be selected for this headphone. Existing saved preset files are not changed by this action.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemovalDialog = false
                    performSave()
                }) { Text("Remove profiles") }
            },
            dismissButton = {
                TextButton(onClick = { showRemovalDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showZeroSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showZeroSelectionDialog = false },
            title = { Text(if (wasManaged) "Remove headphone?" else "Don’t add headphone?") },
            text = {
                Text(
                    if (wasManaged) {
                        "No profiles are selected. Saving will remove this headphone from My Headphones. Existing saved preset files are not changed."
                    } else {
                        "No profiles are selected, so this headphone will not be added to My Headphones."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showZeroSelectionDialog = false
                    performSave()
                }) { Text(if (wasManaged) "Remove headphone" else "Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showZeroSelectionDialog = false }) { Text("Cancel") }
            },
        )
    }

    incompatibilityExplanation?.let { explanation ->
        AlertDialog(
            onDismissRequest = { incompatibilityExplanation = null },
            title = { Text("Not compatible") },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { incompatibilityExplanation = null }) { Text("OK") }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = ::requestBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            Text(vendor?.name ?: "Models", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = product.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (vendor != null) {
            Text(
                text = vendor.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!initialized) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Loading saved selections…")
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { stagedSelectedIds = selectableProfileIds(profiles) }) {
                Text("Select all")
            }
            TextButton(onClick = { stagedSelectedIds = emptySet() }) {
                Text("Select none")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Automatically include new OPRA profiles for this headphone",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = autoInclude,
                onCheckedChange = { autoInclude = it },
            )
        }

        if (hiddenCount > 0) {
            Text(
                text = "$hiddenCount OPRA profiles hidden by your compatibility filter.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = ::requestSave,
            enabled = dirty,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Save changes")
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                val assessment = profile.assessCompatibility()
                ProfileRow(
                    profile = profile,
                    selected = profile.id in stagedSelectedIds,
                    onSelectionChange = { selected ->
                        stagedSelectedIds = if (selected) {
                            stagedSelectedIds + profile.id
                        } else {
                            stagedSelectedIds - profile.id
                        }
                    },
                    onExplainIncompatibility = {
                        incompatibilityExplanation = assessment.reason
                            ?: "This OPRA profile cannot be converted safely by the established UAPP/ToneBoosters mapping."
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: OpraEqProfile,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onExplainIncompatibility: () -> Unit,
) {
    val compatibility = profile.assessCompatibility()
    val selectable = compatibility.category.isSelectable
    val rowModifier = if (selectable) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = onSelectionChange,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onExplainIncompatibility)
    }

    ListItem(
        leadingContent = {
            Checkbox(
                checked = selected && selectable,
                onCheckedChange = null,
                enabled = selectable,
            )
        },
        headlineContent = {
            Text(profile.author?.takeIf { it.isNotBlank() } ?: "Creator information missing")
        },
        supportingContent = {
            Column {
                profile.details?.let { Text(it) }
                when (compatibility.category) {
                    ProfileCompatibility.FullyCompatible -> Unit
                    ProfileCompatibility.CompatibleWithLimitation -> {
                        Text(
                            text = "Compatible with limitation",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        compatibility.reason?.let { Text(it) }
                    }
                    ProfileCompatibility.NotCompatible -> {
                        Text(
                            text = "Not compatible · unavailable for selection",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        compatibility.reason?.let { Text(it) }
                    }
                }
            }
        },
        modifier = rowModifier,
    )
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
