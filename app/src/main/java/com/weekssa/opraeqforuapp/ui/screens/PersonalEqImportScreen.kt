package com.weekssa.opraeqforuapp.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.ParametricEqTextParser
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PersonalEqImportScreen(
    onBack: () -> Unit,
    onSave: suspend (String, String, String, String?, String) -> SavedEqRecord,
    onSaved: (SavedEqRecord) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var peqText by remember { mutableStateOf("") }
    var loadedFileName by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val parsed = remember(peqText) {
        peqText.takeIf(String::isNotBlank)?.let(ParametricEqTextParser::parseStrictPersonal)
    }
    val canSave = !saving && manufacturer.isNotBlank() && model.isNotBlank() &&
        displayName.isNotBlank() && parsed?.isValid == true

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) { readTextDocument(context, uri) }
            }
            loaded.onSuccess { (name, text) ->
                loadedFileName = name
                peqText = text
            }.onFailure { error ->
                onMessage(error.message ?: "Couldn't read that file.")
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "import-header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Text("My EQs", modifier = Modifier.padding(start = 4.dp))
                }
                Text("Import personal EQ", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Supported input: Equalizer APO / AutoEq parametric text. The file extension does not choose the converter; EQ Library inspects the contents and normalizes a valid PEQ into its canonical model first.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "import-actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            .orEmpty()
                        if (text.isBlank()) {
                            onMessage("Clipboard doesn't contain PEQ text.")
                        } else {
                            loadedFileName = null
                            peqText = text
                        }
                    },
                ) { Text("Paste PEQ text") }
                OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                    Text("Choose file")
                }
            }
            loadedFileName?.let { name ->
                Text(
                    "Loaded: $name",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "metadata") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Headphone model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("EQ name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target / note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = peqText,
                    onValueChange = {
                        loadedFileName = null
                        peqText = it
                    },
                    label = { Text("Equalizer APO / AutoEq text") },
                    minLines = 7,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (parsed != null) {
            item(key = "preview-heading") {
                HorizontalDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Parsed EQ", style = MaterialTheme.typography.titleMedium)
                    Text(
                        parsed.parsedEq.preampGainDb?.let { "Preamp: ${formatDb(it)} dB" }
                            ?: "No preamp supplied",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${parsed.parsedEq.filters.size} active filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    parsed.errors.forEach { error ->
                        Text(
                            error,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            itemsIndexed(parsed.parsedEq.filters, key = { index, _ -> "preview-filter:$index" }) { index, filter ->
                Text(
                    text = "${index + 1}. ${filterTypeLabel(filter.type)} · ${formatFrequency(filter.frequencyHz)} Hz · ${formatDb(requireNotNull(filter.gainDb))} dB · Q ${formatQ(requireNotNull(filter.q))}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (parsed.isValid) {
                item(key = "ready") {
                    Text(
                        "Ready to import. The preview above is the canonical EQ that will be saved.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item(key = "save") {
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        runCatching {
                            onSave(
                                manufacturer,
                                model,
                                displayName,
                                target.takeIf(String::isNotBlank),
                                peqText,
                            )
                        }.onSuccess(onSaved)
                            .onFailure { error ->
                                onMessage(error.message ?: "Couldn't import that PEQ.")
                            }
                        saving = false
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(if (saving) "Saving…" else "Save & export")
            }
            Text(
                "Import saves the complete canonical PEQ first. Output-specific band/range limits are applied only during export. Import never flashes connected hardware.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun readTextDocument(context: Context, uri: Uri): Pair<String, String> {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        ?.takeIf(String::isNotBlank)
        ?: "Selected file"
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
        it.readText().removePrefix("\uFEFF")
    } ?: error("Couldn't open that file.")
    require(text.length <= MAX_IMPORT_CHARACTERS) { "That file is too large to be a PEQ text preset." }
    return name to text
}

private fun filterTypeLabel(type: EqFilterType): String = when (type) {
    EqFilterType.PEAK -> "Peak"
    EqFilterType.LOW_SHELF -> "Low shelf"
    EqFilterType.HIGH_SHELF -> "High shelf"
    EqFilterType.LOW_PASS -> "Low pass"
    EqFilterType.HIGH_PASS -> "High pass"
    EqFilterType.OTHER -> "Other"
}

private fun formatDb(value: Double): String = String.format(Locale.US, "%+.2f", value)
private fun formatQ(value: Double): String = String.format(Locale.US, "%.3f", value)
private fun formatFrequency(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.2f", value)

private const val MAX_IMPORT_CHARACTERS = 500_000
