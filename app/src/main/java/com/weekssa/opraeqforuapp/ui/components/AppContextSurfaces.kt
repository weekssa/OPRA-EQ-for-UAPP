package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.ui.theme.MaterialThemeEqPalette

@Composable
internal fun ConnectedDeviceSurface(
    deviceName: String,
    status: String,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialThemeEqPalette
    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Usb,
                contentDescription = null,
                tint = if (isCurrent) palette.connected else palette.stale,
                modifier = Modifier.size(28.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) palette.connected else palette.stale,
                )
            }
            Text(
                text = "Open My DAC",
                style = MaterialTheme.typography.labelLarge,
                color = palette.primary,
            )
        }
    }
}

@Composable
internal fun ContextualDeviceChoiceSurface(
    title: String,
    status: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialThemeEqPalette
    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(status, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
    }
}
