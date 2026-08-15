package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.visibilityCategory
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences

@Composable
fun BrowseOpraScreen(
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
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
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedVendorId = selectedVendorId,
            onVendorSelected = {
                selectedVendorId = it
                selectedProductId = null
            },
            selectedProductId = selectedProductId,
            onProductSelected = { product ->
                selectedProductId = product.id
            },
            onBackToVendors = {
                selectedVendorId = null
                selectedProductId = null
            },
            onBackFromProduct = { selectedProductId = null },
            modifier = modifier,
        )
    }
}

@Composable
private fun CatalogBrowser(
    catalog: OpraCatalog,
    profileVisibility: ProfileVisibilityPreferences,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedVendorId: String?,
    onVendorSelected: (String) -> Unit,
    selectedProductId: String?,
    onProductSelected: (OpraProduct) -> Unit,
    onBackToVendors: () -> Unit,
    onBackFromProduct: () -> Unit,
    modifier: Modifier,
) {
    val product = selectedProductId?.let(catalog::product)
    val vendor = selectedVendorId?.let(catalog::vendor)

    when {
        product != null -> ProductProfiles(
            catalog = catalog,
            product = product,
            profileVisibility = profileVisibility,
            onBack = onBackFromProduct,
            modifier = modifier,
        )
        vendor != null -> VendorProducts(
            catalog = catalog,
            vendorId = vendor.id,
            onProductSelected = onProductSelected,
            onBack = onBackToVendors,
            modifier = modifier,
        )
        else -> BrowseRoot(
            catalog = catalog,
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
                        ListItem(
                            headlineContent = { Text(result.product.name) },
                            supportingContent = { Text(result.vendor.name) },
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
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = { Text("${catalog.profileCount(product.id)} profiles") },
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

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
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
        if (hiddenCount > 0) {
            Text(
                text = "$hiddenCount OPRA profiles hidden by your compatibility filter.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                ProfileRow(profile)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: OpraEqProfile) {
    val compatibility = profile.assessCompatibility()
    ListItem(
        headlineContent = {
            Text(profile.author ?: "Creator information missing")
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
                            text = "Not compatible",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        compatibility.reason?.let { Text(it) }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
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
