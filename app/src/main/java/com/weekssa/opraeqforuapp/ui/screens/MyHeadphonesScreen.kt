package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogState

@Composable
fun MyHeadphonesScreen(
    catalogState: CatalogState,
    onBrowseOpra: () -> Unit,
    onRefreshCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No headphones yet",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Choose OPRA profiles to build your local preset library.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Your selections stay on this device.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBrowseOpra,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Browse OPRA")
        }

        when (catalogState) {
            CatalogState.Loading -> Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Downloading OPRA catalog…",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is CatalogState.Unavailable -> {
                Text(
                    text = unavailableCatalogMessage(catalogState.reason),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRefreshCatalog) {
                    Text("Try again")
                }
            }
            is CatalogState.Ready -> Unit
        }
    }
}

internal fun unavailableCatalogMessage(reason: CatalogRefreshFailureReason): String = when (reason) {
    CatalogRefreshFailureReason.Network -> "The OPRA catalog couldn’t be downloaded. Check your connection and try again."
    CatalogRefreshFailureReason.InvalidCatalog -> "The downloaded OPRA catalog couldn’t be processed."
    CatalogRefreshFailureReason.Storage -> "The OPRA catalog couldn’t be saved on this device."
}
