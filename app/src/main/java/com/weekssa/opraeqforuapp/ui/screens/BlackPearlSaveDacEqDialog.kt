package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import kotlinx.coroutines.launch

@Composable
internal fun BlackPearlSaveDacEqDialog(
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    onDismiss: () -> Unit,
    onSave: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var selectedAssociation by remember { mutableStateOf<SavedEqHeadphoneAssociation?>(null) }
    var librarySearch by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val savedAssociations = remember(managedHeadphones, savedEqs) {
        buildList {
            managedHeadphones.forEach { headphone ->
                add(
                    SavedEqHeadphoneAssociation(
                        productId = headphone.productId,
                        manufacturer = headphone.vendorName,
                        model = headphone.productName,
                    ),
                )
            }
            savedEqs
                .filter(SavedEqRecord::hasHeadphoneAssociation)
                .forEach { record ->
                    add(
                        SavedEqHeadphoneAssociation(
                            productId = record.productId,
                            manufacturer = record.manufacturer,
                            model = record.model,
                        ),
                    )
                }
        }.distinctBy { association ->
            Triple(association.productId, association.manufacturer, association.model)
        }.sortedWith(
            compareBy<SavedEqHeadphoneAssociation>(String.CASE_INSENSITIVE_ORDER) { it.manufacturer }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.model },
        )
    }

    val libraryAssociations = remember(catalogState, librarySearch) {
        val ready = catalogState as? CatalogState.Ready
        if (ready == null || librarySearch.isBlank()) {
            emptyList()
        } else {
            ready.catalog.searchProducts(librarySearch)
                .take(MAX_LIBRARY_RESULTS)
                .map { result ->
                    SavedEqHeadphoneAssociation(
                        productId = result.product.id,
                        manufacturer = result.vendor.name,
                        model = result.product.name,
                    )
                }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.my_dac_save_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.my_dac_save_provenance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.my_dac_save_name)) },
                    placeholder = { Text(stringResource(R.string.my_dac_save_name_placeholder)) },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                Text(
                    text = stringResource(R.string.my_dac_save_association_heading),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.my_dac_save_association_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                AssociationChoiceRow(
                    label = stringResource(R.string.my_dac_save_leave_unassociated),
                    selected = selectedAssociation == null,
                    enabled = !saving,
                    onClick = { selectedAssociation = null },
                )

                if (savedAssociations.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.my_dac_save_saved_headphones),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                    savedAssociations.forEach { association ->
                        AssociationChoiceRow(
                            label = "${association.manufacturer} · ${association.model}",
                            selected = selectedAssociation == association,
                            enabled = !saving,
                            onClick = { selectedAssociation = association },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.my_dac_save_search_library),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = librarySearch,
                    onValueChange = { librarySearch = it },
                    label = { Text(stringResource(R.string.my_dac_save_search_hint)) },
                    singleLine = true,
                    enabled = !saving && catalogState is CatalogState.Ready,
                    supportingText = if (catalogState !is CatalogState.Ready) {
                        { Text(stringResource(R.string.my_dac_save_catalog_unavailable)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                libraryAssociations.forEach { association ->
                    AssociationChoiceRow(
                        label = "${association.manufacturer} · ${association.model}",
                        selected = selectedAssociation == association,
                        enabled = !saving,
                        onClick = { selectedAssociation = association },
                    )
                }
                if (
                    catalogState is CatalogState.Ready &&
                    librarySearch.isNotBlank() &&
                    libraryAssociations.isEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.my_dac_save_no_library_matches),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && displayName.isNotBlank(),
                onClick = {
                    scope.launch {
                        saving = true
                        val message = runCatching {
                            onSave(displayName, selectedAssociation)
                        }.getOrElse { error ->
                            error.message ?: "Could not save the current Black Pearl EQ."
                        }
                        saving = false
                        onDismiss()
                        onMessage(message)
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (saving) R.string.my_dac_save_saving else R.string.my_dac_save_action,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun AssociationChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private const val MAX_LIBRARY_RESULTS = 8
