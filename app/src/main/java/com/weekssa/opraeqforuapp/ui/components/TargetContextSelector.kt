package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.export.ExportDevice

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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Box {
            TextButton(
                onClick = { expanded = true },
                enabled = enabledTargets.isNotEmpty(),
            ) {
                Text(
                    text = stringResource(
                        R.string.target_selector_format,
                        activeTarget.displayName,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
