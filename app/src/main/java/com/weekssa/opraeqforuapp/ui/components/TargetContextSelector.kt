package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.ui.theme.MaterialThemeEqPalette

/**
 * Secondary action context for My EQs / EQ Library.
 *
 * Target changes affect compatibility and Export/Flash actions only. They never represent library
 * ownership and never navigate away from the current EQ/headphone surface.
 */
@Composable
internal fun TargetContextSelector(
    activeTarget: ExportDevice,
    enabledTargets: List<ExportDevice>,
    onTargetChange: (ExportDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val palette = MaterialThemeEqPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                onClick = { expanded = true },
                enabled = enabledTargets.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { role = Role.Button },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceSubtle),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                    ) {
                        Text(
                            text = stringResource(R.string.target_selector_format, activeTarget.displayName),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                        )
                        Text("Change", style = MaterialTheme.typography.labelLarge, color = palette.primary, maxLines = 1, softWrap = false)
                    }
                    Text(
                        text = "Prepares EQ files or actions; it does not route Android audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                enabledTargets.forEach { target ->
                    DropdownMenuItem(
                        text = { Text(target.displayName) },
                        onClick = {
                            expanded = false
                            onTargetChange(target)
                        },
                    )
                }
            }
        }
    }
}
