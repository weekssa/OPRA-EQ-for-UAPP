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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.ParametricEqTextParser
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PersonalEqImportStage(val label: String) {
    INPUT("Input"),
    PARSE("Parse"),
    DESCRIBE("Describe"),
    REVIEW("Review"),
    SAVE("Save"),
}

@Composable
internal fun PersonalEqImportScreen(
    onBack: () -> Unit,
    onSave: suspend (String, String, String, String?, String) -> SavedEqRecord,
    onSaved: (SavedEqRecord) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var manufacturer by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    val draftStore = remember(context.filesDir) { PersonalEqInputDraftStore(context.filesDir) }
    val draftId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    val peqTextSaver = remember(draftStore, draftId) { draftStore.saver(draftId) }
    var peqText by rememberSaveable(draftId, stateSaver = peqTextSaver) { mutableStateOf("") }
    var loadedFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var stageName by rememberSaveable { mutableStateOf(PersonalEqImportStage.INPUT.name) }
    var saving by remember { mutableStateOf(false) }
    val stage = runCatching { PersonalEqImportStage.valueOf(stageName) }
        .getOrDefault(PersonalEqImportStage.INPUT)
    val parsed = remember(peqText) {
        peqText.takeIf(String::isNotBlank)?.let(ParametricEqTextParser::parseStrictPersonal)
    }
    val canDescribe = parsed?.isValid == true
    val canReview = manufacturer.isNotBlank() && model.isNotBlank() && displayName.isNotBlank()
    val canSave = !saving && canReview && canDescribe
    val previewFilters = parsed?.parsedEq?.filters.orEmpty().mapIndexedNotNull { index, filter ->
        val gain = filter.gainDb ?: return@mapIndexedNotNull null
        val q = filter.q ?: return@mapIndexedNotNull null
        com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter(
            index = index,
            enabled = true,
            type = filter.type,
            frequencyHz = filter.frequencyHz,
            gainDb = gain,
            q = q,
        )
    }
    val responseCurve = remember(previewFilters) {
        runCatching {
            com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator.evaluate(previewFilters)
        }.getOrNull()
    }
    val importActionButtonColors = ButtonDefaults.buttonColors(
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    fun setInputText(value: String, fileName: String? = null) {
        if (value.length > MAX_IMPORT_CHARACTERS) {
            onMessage("PEQ text is limited to $MAX_IMPORT_CHARACTERS characters.")
            return
        }
        loadedFileName = fileName
        peqText = value
    }

    LaunchedEffect(peqText) {
        if (peqText == PersonalEqInputDraftStore.RESTORE_FAILURE_MARKER) {
            peqText = ""
            onMessage("Your unsaved text could not be restored. Choose the file or paste the text again.")
        }
    }

    fun goBack() {
        if (saving) return
        stageName = when (stage) {
            PersonalEqImportStage.INPUT -> {
                draftStore.delete(draftId)
                onBack()
                PersonalEqImportStage.INPUT.name
            }
            PersonalEqImportStage.PARSE -> PersonalEqImportStage.INPUT.name
            PersonalEqImportStage.DESCRIBE -> PersonalEqImportStage.PARSE.name
            PersonalEqImportStage.REVIEW -> PersonalEqImportStage.DESCRIBE.name
            PersonalEqImportStage.SAVE -> PersonalEqImportStage.REVIEW.name
        }
    }

    BackHandler(onBack = ::goBack)

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onMessage("No file selected. Your draft is unchanged. Choose a file or paste EQ text to continue.")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) { readTextDocument(context, uri) }
            }
            loaded.onSuccess { (name, text) ->
                setInputText(text, name)
            }.onFailure { error ->
                onMessage(error.message ?: "Couldn't read that file.")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
        item(key = "import-header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = ::goBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Text(if (stage == PersonalEqImportStage.INPUT) "My EQs" else "Previous step", modifier = Modifier.padding(start = 4.dp))
                }
                Text("Import Personal EQ", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Step ${stage.ordinal + 1} of ${PersonalEqImportStage.entries.size} · ${stage.label}",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Import stays on this device. It does not connect to or change hardware.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (stage) {
            PersonalEqImportStage.INPUT -> {
                item(key = "import-input-help") {
                    Text(
                        "Paste or choose Equalizer APO / AutoEq parametric text. EQ Library checks the contents; the file extension does not decide whether it is supported.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item(key = "import-actions") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                    setInputText(text)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Paste PEQ text") }
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Choose file") }
                        loadedFileName?.let { name ->
                            Text("Selected file: $name", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item(key = "import-text") {
                    OutlinedTextField(
                        value = peqText,
                        onValueChange = { setInputText(it) },
                        label = { Text("Equalizer APO / AutoEq text") },
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                } else {
                                    false
                                }
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            PersonalEqImportStage.PARSE -> {
                item(key = "parse-result") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Parse result", style = MaterialTheme.typography.titleMedium)
                        if (parsed == null) {
                            Text("Add EQ text in the previous step, then parse it.")
                        } else {
                            Text("${parsed.parsedEq.filters.size} active filters")
                            Text(
                                parsed.parsedEq.preampGainDb?.let { "Source preamp: ${formatDb(it)} dB" }
                                    ?: "No source preamp supplied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            parsed.errors.forEach { error ->
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                            if (!parsed.isValid) {
                                Text(
                                    "Go back and correct the text. No EQ is saved until the review is accepted.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                item(key = "parse-next") {
                    Button(
                        onClick = { stageName = PersonalEqImportStage.DESCRIBE.name },
                        enabled = canDescribe,
                        colors = importActionButtonColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
                    ) { Text("Continue to description") }
                }
            }

            PersonalEqImportStage.DESCRIBE -> {
                item(key = "description") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Describe this EQ", style = MaterialTheme.typography.titleMedium)
                        Text("The parsed filter values remain unchanged.", style = MaterialTheme.typography.bodySmall)
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
                    }
                }
                item(key = "description-next") {
                    Button(
                        onClick = { stageName = PersonalEqImportStage.REVIEW.name },
                        enabled = canReview && canDescribe,
                        colors = importActionButtonColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
                    ) { Text("Review EQ") }
                }
            }

            PersonalEqImportStage.REVIEW -> {
                item(key = "review-details") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        Text("$manufacturer · $model")
                        target.takeIf(String::isNotBlank)?.let { Text("Target / note: $it") }
                        Text(
                            loadedFileName?.let { "Source file: $it" } ?: "Source: pasted by you",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        parsed?.parsedEq?.preampGainDb?.let { Text("Source preamp: ${formatDb(it)} dB") }
                        Text(
                            "Filter response preview. This view does not adapt the EQ to an output.",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (responseCurve != null && parsed != null) {
                    item(key = "review-graph") {
                        com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph(
                            curve = responseCurve,
                            filters = previewFilters,
                            accessibilityDescription = "Filter response preview for $displayName. Source preamp is shown separately.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    item(key = "review-graph-unavailable") {
                        Text(
                            "A response plot is unavailable for these filter values. The source values below remain available for review.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                parsed?.let { result ->
                    itemsIndexed(result.parsedEq.filters, key = { index, _ -> "review-filter:$index" }) { index, filter ->
                        Text(
                            text = "${index + 1}. ${filterTypeLabel(filter.type)} · ${formatFrequency(filter.frequencyHz)} Hz · ${formatDb(requireNotNull(filter.gainDb))} dB · Q ${formatQ(requireNotNull(filter.q))}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item(key = "review-next") {
                    Button(
                        onClick = { stageName = PersonalEqImportStage.SAVE.name },
                        enabled = canReview && canDescribe,
                        colors = importActionButtonColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Continue to save") }
                }
            }

            PersonalEqImportStage.SAVE -> {
                item(key = "save-summary") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Ready to save", style = MaterialTheme.typography.titleMedium)
                        Text("$displayName · $manufacturer $model")
                        Text("${parsed?.parsedEq?.filters?.size ?: 0} filters will be saved as your Personal EQ.")
                        Text(
                            "Saving keeps the complete canonical EQ on this device. Export or hardware actions are separate and happen only when you choose them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item(key = "save-action") {
                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val record = onSave(
                                        manufacturer,
                                        model,
                                        displayName,
                                        target.takeIf(String::isNotBlank),
                                        peqText,
                                    )
                                    draftStore.delete(draftId)
                                    onSaved(record)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    onMessage(error.message ?: "Couldn't import that PEQ.")
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = canSave,
                        colors = importActionButtonColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(if (saving) "Saving…" else "Save to My EQs")
                    }
                }
            }
        }
        }
        if (stage == PersonalEqImportStage.INPUT) {
            Button(
                onClick = { stageName = PersonalEqImportStage.PARSE.name },
                enabled = peqText.isNotBlank(),
                colors = importActionButtonColors,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Parse EQ text") }
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
