#!/usr/bin/env python3
"""One-shot connected-GitHub patch helper for the approved v0.3 PR #4 implementation.

This exists only because the connected GitHub API exposes whole-file writes but not
patch application. A temporary workflow runs this script once, commits the resulting
implementation, and the helper/workflow hook are then removed before final validation.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one replacement target, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_slice(path: str, start_marker: str, end_marker: str, replacement: str) -> None:
    text = read(path)
    start = text.find(start_marker)
    if start < 0:
        raise SystemExit(f"{path}: start marker not found: {start_marker!r}")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"{path}: end marker not found: {end_marker!r}")
    write(path, text[:start] + replacement + text[end:])


# ---- Stable canonical identity + local visibility projection -----------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/catalog/OpraCatalog.kt",
    """    val isVerified: Boolean = true,\n) {\n""",
    """    /** Stable canonical tuning lineage identity used by local Hide/Unhide. */\n    val canonicalProfileId: String = id,\n    val isVerified: Boolean = true,\n) {\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/catalog/OpraCatalog.kt",
    """    val eqLibrarySafetyHeadroomDb: Double? = null,\n    val isVerified: Boolean = true,\n    val isLatestRevision: Boolean = true,\n)\n""",
    """    val eqLibrarySafetyHeadroomDb: Double? = null,\n    /** Stable canonical tuning lineage identity used by local Hide/Unhide. */\n    val canonicalProfileId: String = id,\n    val isVerified: Boolean = true,\n    val isLatestRevision: Boolean = true,\n)\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/catalog/OpraCatalog.kt",
    """    private fun resolveProductId(productId: String): String {\n""",
    """    /**\n     * Returns the ordinary browse/search projection after applying the user's global local Hide\n     * preferences. Canonical data is never removed; callers that manage My EQs keep the original\n     * unfiltered catalog. Hiding one lineage hides all of its genuine revisions.\n     */\n    fun excludingHiddenCanonicalProfiles(hiddenCanonicalProfileIds: Set<String>): OpraCatalog {\n        if (hiddenCanonicalProfileIds.isEmpty()) return this\n\n        val visibleProfiles = profiles.filterNot { it.canonicalProfileId in hiddenCanonicalProfileIds }\n        val visibleGeneralPresets = generalPresets.filterNot {\n            it.canonicalProfileId in hiddenCanonicalProfileIds\n        }\n        val visibleProductIds = visibleProfiles\n            .mapTo(mutableSetOf()) { canonicalProductId(it.productId) }\n        val visibleProducts = products.filter { canonicalProductId(it.id) in visibleProductIds }\n        val visibleVendorIds = visibleProducts.mapTo(mutableSetOf(), OpraProduct::vendorId)\n\n        return copy(\n            vendors = vendors.filter { it.id in visibleVendorIds },\n            products = visibleProducts,\n            profiles = visibleProfiles,\n            generalPresets = visibleGeneralPresets,\n        )\n    }\n\n    private fun resolveProductId(productId: String): String {\n""",
)

replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/library/CanonicalLegacyCatalogAdapter.kt",
    """                GeneralEqPreset(\n                    id = \"eq-library-general:${profile.canonicalProfileId}@${revision.revisionId}\",\n                    displayName = displayName,\n""",
    """                GeneralEqPreset(\n                    id = \"eq-library-general:${profile.canonicalProfileId}@${revision.revisionId}\",\n                    displayName = displayName,\n                    canonicalProfileId = profile.canonicalProfileId,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/library/CanonicalLegacyCatalogAdapter.kt",
    """        return OpraEqProfile(\n            id = legacyProfileId,\n            productId = productId,\n""",
    """        return OpraEqProfile(\n            id = legacyProfileId,\n            productId = productId,\n            canonicalProfileId = profile.canonicalProfileId,\n""",
)

# Preserve canonical lineage in durable saved snapshots without changing acoustic fingerprints.
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/managed/ManagedProfileSnapshotCodec.kt",
    """private data class StoredProfileSnapshot(\n    val id: String,\n    val productId: String,\n""",
    """private data class StoredProfileSnapshot(\n    val id: String,\n    val productId: String,\n    val canonicalProfileId: String? = null,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/managed/ManagedProfileSnapshotCodec.kt",
    """private fun OpraEqProfile.toStoredSnapshot() = StoredProfileSnapshot(\n    id = id,\n    productId = productId,\n""",
    """private fun OpraEqProfile.toStoredSnapshot() = StoredProfileSnapshot(\n    id = id,\n    productId = productId,\n    canonicalProfileId = canonicalProfileId,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/managed/ManagedProfileSnapshotCodec.kt",
    """private fun StoredProfileSnapshot.toDomain() = OpraEqProfile(\n    id = id,\n    productId = productId,\n""",
    """private fun StoredProfileSnapshot.toDomain() = OpraEqProfile(\n    id = id,\n    productId = productId,\n    canonicalProfileId = canonicalProfileId ?: id,\n""",
)

# ---- Preferences DataStore: global reversible Hide/Unhide -------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/settings/AppPreferences.kt",
    """    val directBlackPearlFlashEnabled: Boolean = false,\n    val exportTreeUri: String? = null,\n""",
    """    val directBlackPearlFlashEnabled: Boolean = false,\n    val hiddenCanonicalProfileIds: Set<String> = emptySet(),\n    val exportTreeUri: String? = null,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/preferences/AppPreferencesRepository.kt",
    """            directBlackPearlFlashEnabled = preferences[Keys.DirectBlackPearlFlashEnabled] ?: false,\n            exportTreeUri = preferences[Keys.ExportTreeUri],\n""",
    """            directBlackPearlFlashEnabled = preferences[Keys.DirectBlackPearlFlashEnabled] ?: false,\n            hiddenCanonicalProfileIds = preferences[Keys.HiddenCanonicalProfileIds].orEmpty(),\n            exportTreeUri = preferences[Keys.ExportTreeUri],\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/preferences/AppPreferencesRepository.kt",
    """    suspend fun setExportTree(uri: String, label: String) {\n""",
    """    suspend fun hideCanonicalProfiles(canonicalProfileIds: Set<String>) {\n        val cleanIds = canonicalProfileIds.filterTo(mutableSetOf()) { it.isNotBlank() }\n        if (cleanIds.isEmpty()) return\n        appContext.appPreferencesDataStore.edit { preferences ->\n            preferences[Keys.HiddenCanonicalProfileIds] =\n                preferences[Keys.HiddenCanonicalProfileIds].orEmpty() + cleanIds\n        }\n    }\n\n    suspend fun unhideCanonicalProfiles(canonicalProfileIds: Set<String>) {\n        if (canonicalProfileIds.isEmpty()) return\n        appContext.appPreferencesDataStore.edit { preferences ->\n            val remaining = preferences[Keys.HiddenCanonicalProfileIds].orEmpty() - canonicalProfileIds\n            if (remaining.isEmpty()) {\n                preferences.remove(Keys.HiddenCanonicalProfileIds)\n            } else {\n                preferences[Keys.HiddenCanonicalProfileIds] = remaining\n            }\n        }\n    }\n\n    suspend fun setExportTree(uri: String, label: String) {\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/preferences/AppPreferencesRepository.kt",
    """        val DirectBlackPearlFlashEnabled = booleanPreferencesKey(\"direct_black_pearl_flash_enabled\")\n        // Legacy v0.3 preview key intentionally left unread. Output selection no longer hides library curves.\n""",
    """        val DirectBlackPearlFlashEnabled = booleanPreferencesKey(\"direct_black_pearl_flash_enabled\")\n        val HiddenCanonicalProfileIds = stringSetPreferencesKey(\"hidden_canonical_profile_ids\")\n        // Legacy v0.3 preview key intentionally left unread. Output selection no longer hides library curves.\n""",
)

# ---- Strict Equalizer APO / AutoEq personal-import validation ---------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/library/ParametricEqTextParser.kt",
    """    private fun parseType(raw: String): EqFilterType = when (raw.uppercase(Locale.ROOT)) {\n""",
    """    data class StrictParseResult(\n        val parsedEq: ParsedEq,\n        val errors: List<String>,\n    ) {\n        val isValid: Boolean get() = errors.isEmpty() && parsedEq.filters.isNotEmpty()\n    }\n\n    /**\n     * User-facing personal import is intentionally stricter than source discovery. Unknown prose\n     * may be ignored, but malformed Filter/Preamp lines and unsupported enabled filter types are\n     * blocking so an intended 10-band EQ can never silently become a 9-band import.\n     */\n    fun parseStrictPersonal(text: String): StrictParseResult {\n        var preamp: Double? = null\n        val filters = mutableListOf<EqFilter>()\n        val errors = mutableListOf<String>()\n\n        text.lineSequence().forEachIndexed { index, rawLine ->\n            val lineNumber = index + 1\n            val line = rawLine.trim()\n            if (line.isEmpty() || line.startsWith(\"#\")) return@forEachIndexed\n\n            if (line.startsWith(\"Preamp\", ignoreCase = true)) {\n                val match = preampRegex.matchEntire(line)\n                if (match == null) {\n                    errors += \"Line $lineNumber: malformed Preamp line.\"\n                } else {\n                    preamp = match.groupValues[1].toDoubleOrNull()\n                    if (preamp == null) errors += \"Line $lineNumber: invalid Preamp value.\"\n                }\n                return@forEachIndexed\n            }\n\n            if (!line.startsWith(\"Filter\", ignoreCase = true)) return@forEachIndexed\n            val filterMatch = filterPrefixRegex.matchEntire(line)\n            if (filterMatch == null) {\n                errors += \"Line $lineNumber: malformed Filter line.\"\n                return@forEachIndexed\n            }\n            if (filterMatch.groupValues[1].equals(\"OFF\", ignoreCase = true)) {\n                return@forEachIndexed\n            }\n\n            val rawType = filterMatch.groupValues[2]\n            val type = parseType(rawType)\n            if (type !in STRICT_PERSONAL_TYPES) {\n                errors += \"Line $lineNumber: unsupported active filter type $rawType.\"\n                return@forEachIndexed\n            }\n\n            val body = filterMatch.groupValues[3]\n            val frequency = frequencyRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()\n            if (frequency == null || frequency <= 0.0) {\n                errors += \"Line $lineNumber: filter frequency must be a positive number.\"\n                return@forEachIndexed\n            }\n            val gain = gainRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()\n            if (gain == null) {\n                errors += \"Line $lineNumber: filter Gain is required.\"\n                return@forEachIndexed\n            }\n            val q = qRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()\n            if (q == null || q <= 0.0) {\n                errors += \"Line $lineNumber: filter Q must be a positive number.\"\n                return@forEachIndexed\n            }\n\n            filters += EqFilter(\n                type = type,\n                frequencyHz = frequency,\n                gainDb = gain,\n                q = q,\n            )\n        }\n\n        if (filters.isEmpty() && errors.isEmpty()) {\n            errors += \"This EQ format isn't supported yet. Use Equalizer APO / AutoEq parametric text.\"\n        }\n        return StrictParseResult(\n            parsedEq = ParsedEq(preampGainDb = preamp, filters = filters),\n            errors = errors,\n        )\n    }\n\n    private fun parseType(raw: String): EqFilterType = when (raw.uppercase(Locale.ROOT)) {\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/domain/library/ParametricEqTextParser.kt",
    """        else -> EqFilterType.OTHER\n    }\n}\n""",
    """        else -> EqFilterType.OTHER\n    }\n\n    private val STRICT_PERSONAL_TYPES = setOf(\n        EqFilterType.PEAK,\n        EqFilterType.LOW_SHELF,\n        EqFilterType.HIGH_SHELF,\n    )\n}\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/library/SavedEqRepository.kt",
    """        val parsed = ParametricEqTextParser.parse(peqText)\n        require(parsed.filters.isNotEmpty()) { \"No supported enabled PEQ filters were found.\" }\n""",
    """        val strict = ParametricEqTextParser.parseStrictPersonal(peqText)\n        require(strict.errors.isEmpty()) { strict.errors.joinToString(\" \") }\n        val parsed = strict.parsedEq\n        require(parsed.filters.isNotEmpty()) { \"No supported enabled PEQ filters were found.\" }\n""",
)

# ---- General EQ Save selected must be idempotent, not a toggle --------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/library/SavedGeneralEqRepository.kt",
    """    suspend fun toggleForOutput(outputId: String, preset: GeneralEqPreset): Boolean =\n""",
    """    suspend fun saveForOutput(outputId: String, preset: GeneralEqPreset): Boolean =\n        database.withTransaction {\n            val existingSelection = dao.getSelection(outputId, preset.id)\n            val now = nowMillis()\n            val existing = dao.get(preset.id)\n            dao.upsert(\n                SavedGeneralEqEntity(\n                    presetId = preset.id,\n                    displayName = preset.displayName,\n                    category = preset.category.name,\n                    profileJson = snapshotCodec.encode(preset.toExportProfile()),\n                    createdAtMillis = existing?.createdAtMillis ?: now,\n                    updatedAtMillis = now,\n                ),\n            )\n            if (existingSelection == null) {\n                dao.upsertSelection(\n                    OutputGeneralEqEntity(\n                        outputId = outputId,\n                        presetId = preset.id,\n                        selectedAtMillis = now,\n                    ),\n                )\n            }\n            existingSelection == null\n        }\n\n    suspend fun toggleForOutput(outputId: String, preset: GeneralEqPreset): Boolean =\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/data/library/SavedGeneralEqRepository.kt",
    """    private fun GeneralEqPreset.toExportProfile(): OpraEqProfile = OpraEqProfile(\n        id = id,\n        productId = INTERNAL_GENERAL_PRODUCT_ID,\n""",
    """    private fun GeneralEqPreset.toExportProfile(): OpraEqProfile = OpraEqProfile(\n        id = id,\n        productId = INTERNAL_GENERAL_PRODUCT_ID,\n        canonicalProfileId = canonicalProfileId,\n""",
)

# ---- Dedicated personal-import screen ---------------------------------------
write(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/PersonalEqImportScreen.kt",
    r'''package com.weekssa.opraeqforuapp.ui.screens

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
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("Couldn't open that file.")
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
''',
)

# ---- My EQs: compact + Import, dedicated importer, initial export ------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """    ) -> String?,\n    onDeleteSavedEq: suspend (String) -> Unit,\n""",
    """    ) -> SavedEqRecord,\n    onDeleteSavedEq: suspend (String) -> Unit,\n""",
)
replace_slice(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """    if (importOpen) {\n        PersonalEqDialog(\n""",
    """    pendingFlash?.let { pending ->\n""",
    """    if (importOpen) {\n        PersonalEqImportScreen(\n            onBack = { importOpen = false },\n            onSave = onImportPersonal,\n            onSaved = { record ->\n                importOpen = false\n                onMessage(\"Personal EQ saved to My EQs.\")\n                onExportSavedEq(record.entryId)\n            },\n            onMessage = onMessage,\n            modifier = modifier,\n        )\n        return\n    }\n\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                    if (exportCurrentness.hasPendingExport) {\n                        Button(onClick = onExportAll) {\n                            Icon(Icons.Outlined.FileUpload, contentDescription = null)\n                            Text(\"Export all\", modifier = Modifier.padding(start = 6.dp))\n                        }\n                    }\n                    OutlinedButton(onClick = { importOpen = true }) {\n                        Icon(Icons.Outlined.Add, contentDescription = null)\n                        Text(\"Import PEQ\", modifier = Modifier.padding(start = 6.dp))\n                    }\n                }\n""",
    """                if (exportCurrentness.hasPendingExport) {\n                    Button(onClick = onExportAll) {\n                        Icon(Icons.Outlined.FileUpload, contentDescription = null)\n                        Text(\"Export all\", modifier = Modifier.padding(start = 6.dp))\n                    }\n                }\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """                item(key = \"saved-headphone-heading\") {\n                    Text(\n                        text = \"Saved snapshots & personal imports\",\n                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),\n                        style = MaterialTheme.typography.labelLarge,\n                        color = MaterialTheme.colorScheme.onSurfaceVariant,\n                    )\n                }\n""",
    """                item(key = \"saved-headphone-heading\") {\n                    SavedImportsHeading(onImport = { importOpen = true })\n                }\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """        item(key = \"general-heading\") {\n            SectionHeading(\"General EQs\")\n        }\n""",
    """        if (headphoneSavedEqs.isEmpty()) {\n            item(key = \"saved-headphone-heading-empty\") {\n                SavedImportsHeading(onImport = { importOpen = true })\n            }\n        }\n\n        item(key = \"general-heading\") {\n            SectionHeading(\"General EQs\")\n        }\n""",
)
replace_slice(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt",
    """@Composable\nprivate fun PersonalEqDialog(\n""",
    """private fun exportStatusText(exportCurrentness: ExportCurrentness): String {\n""",
    """@Composable\nprivate fun SavedImportsHeading(onImport: () -> Unit) {\n    Row(\n        modifier = Modifier\n            .fillMaxWidth()\n            .padding(horizontal = 16.dp, vertical = 6.dp),\n        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,\n    ) {\n        Text(\n            text = \"Saved snapshots & personal imports\",\n            modifier = Modifier.weight(1f),\n            style = MaterialTheme.typography.labelLarge,\n            color = MaterialTheme.colorScheme.onSurfaceVariant,\n        )\n        TextButton(onClick = onImport) {\n            Icon(Icons.Outlined.Add, contentDescription = null)\n            Text(\"Import\", modifier = Modifier.padding(start = 4.dp))\n        }\n    }\n}\n\n""",
)

# ---- Headphone ProfileSelectionEditor: per-lineage Hide ---------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    """import androidx.compose.material.icons.outlined.StarBorder\n""",
    """import androidx.compose.material.icons.outlined.StarBorder\nimport androidx.compose.material.icons.outlined.VisibilityOff\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    """    onToggleFavorite: (suspend (OpraEqProfile, String, String) -> Boolean)?,\n    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,\n""",
    """    onToggleFavorite: (suspend (OpraEqProfile, String, String) -> Boolean)?,\n    onHideCanonicalProfile: suspend (String) -> Unit,\n    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    """                    onToggleFavorite = onToggleFavorite?.let { toggle ->\n                        {\n                            scope.launch {\n                                val favorited = toggle(\n                                    profile,\n                                    vendor?.name ?: \"Unknown manufacturer\",\n                                    product.name,\n                                )\n                                onMessage(\n                                    if (favorited) {\n                                        \"Saved to My EQs favorites.\"\n                                    } else {\n                                        \"Removed from My EQs favorites.\"\n                                    },\n                                )\n                            }\n                        }\n                    },\n                    onOpenSource = profile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },\n""",
    """                    onToggleFavorite = onToggleFavorite?.let { toggle ->\n                        {\n                            scope.launch {\n                                val favorited = toggle(\n                                    profile,\n                                    vendor?.name ?: \"Unknown manufacturer\",\n                                    product.name,\n                                )\n                                onMessage(\n                                    if (favorited) {\n                                        \"Saved to My EQs favorites.\"\n                                    } else {\n                                        \"Removed from My EQs favorites.\"\n                                    },\n                                )\n                            }\n                        }\n                    },\n                    onHide = {\n                        scope.launch {\n                            if (profile.id !in baselineSelectedIds) {\n                                stagedSelectedIds = stagedSelectedIds - profile.id\n                            }\n                            onHideCanonicalProfile(profile.canonicalProfileId)\n                            onMessage(\"EQ hidden from EQ Library. Restore it in Settings → Hidden EQs.\")\n                        }\n                    },\n                    onOpenSource = profile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    """    onToggleFavorite: (() -> Unit)?,\n    onOpenSource: (() -> Unit)?,\n""",
    """    onToggleFavorite: (() -> Unit)?,\n    onHide: () -> Unit,\n    onOpenSource: (() -> Unit)?,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt",
    """        trailingContent = onToggleFavorite?.let { action ->\n            {\n                IconButton(onClick = action) {\n                    Icon(\n                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,\n                        contentDescription = if (isFavorite) \"Remove favorite\" else \"Add favorite\",\n                    )\n                }\n            }\n        },\n""",
    """        trailingContent = {\n            Row {\n                IconButton(onClick = onHide) {\n                    Icon(Icons.Outlined.VisibilityOff, contentDescription = \"Hide from EQ Library\")\n                }\n                onToggleFavorite?.let { action ->\n                    IconButton(onClick = action) {\n                        Icon(\n                            imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,\n                            contentDescription = if (isFavorite) \"Remove favorite\" else \"Add favorite\",\n                        )\n                    }\n                }\n            }\n        },\n""",
)

# ---- Browse: globally filtered catalog + General batch Save/Hide ------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.OutlinedTextField\n""",
    """import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.OutlinedButton\nimport androidx.compose.material3.OutlinedTextField\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """    favoriteProfileIds: Set<String>,\n    savedGeneralPresetIds: Set<String> = emptySet(),\n    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onToggleGeneralPreset: suspend (GeneralEqPreset) -> Boolean = { false },\n""",
    """    favoriteProfileIds: Set<String>,\n    savedGeneralPresetIds: Set<String> = emptySet(),\n    hiddenCanonicalProfileIds: Set<String> = emptySet(),\n    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onSaveGeneralPresets: suspend (List<GeneralEqPreset>) -> Int = { 0 },\n    onHideCanonicalProfiles: suspend (Set<String>) -> Unit = {},\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """            val catalog = catalogState.catalog\n            val product = if (selectedSection == LibrarySection.HEADPHONES) {\n""",
    """            val fullCatalog = catalogState.catalog\n            val catalog = remember(fullCatalog, hiddenCanonicalProfileIds) {\n                fullCatalog.excludingHiddenCanonicalProfiles(hiddenCanonicalProfileIds)\n            }\n            val product = if (selectedSection == LibrarySection.HEADPHONES) {\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """                        onToggleFavorite = onToggleFavorite,\n                        onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
    """                        onToggleFavorite = onToggleFavorite,\n                        onHideCanonicalProfile = { canonicalProfileId ->\n                            onHideCanonicalProfiles(setOf(canonicalProfileId))\n                        },\n                        onLoadManagedHeadphone = onLoadManagedHeadphone,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """                        savedPresetIds = savedGeneralPresetIds,\n                        onSearchQueryChange = { searchQuery = it },\n                        onFilterSelected = { selectedGeneralFilterIndex = it },\n                        onTogglePreset = onToggleGeneralPreset,\n                        onMessage = onMessage,\n""",
    """                        savedPresetIds = savedGeneralPresetIds,\n                        onSearchQueryChange = { searchQuery = it },\n                        onFilterSelected = { selectedGeneralFilterIndex = it },\n                        onSavePresets = onSaveGeneralPresets,\n                        onHideCanonicalProfiles = onHideCanonicalProfiles,\n                        onMessage = onMessage,\n""",
)
replace_slice(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/BrowseOpraScreen.kt",
    """@Composable\nprivate fun GeneralEqBrowse(\n""",
    """@Composable\nprivate fun SearchField(\n""",
    r'''@Composable
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
    var batchSelectedIds by remember(catalog) { mutableStateOf<Set<String>>(emptySet()) }
    val matching = remember(catalog.generalPresets, searchQuery, selectedFilter) {
        catalog.searchGeneralPresets(searchQuery)
            .filter { preset -> selectedFilter.category == null || preset.category == selectedFilter.category }
    }
    val selectedPresets = catalog.generalPresets.filter { it.id in batchSelectedIds }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = "Search General EQs…",
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
                    label = { Text(filter.label) },
                )
            }
        }
        Text(
            text = "General EQs are standalone presets; v0.3 does not layer them on top of headphone correction EQs.",
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
            ) { Text("Select all") }
            TextButton(
                onClick = { batchSelectedIds = batchSelectedIds - matching.map(GeneralEqPreset::id).toSet() },
                enabled = matching.isNotEmpty(),
            ) { Text("Select none") }
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
                        val count = onSavePresets(toSave)
                        batchSelectedIds = emptySet()
                        onMessage("$count ${if (count == 1) "General EQ" else "General EQs"} saved to My EQs. Initial export started.")
                    }
                },
                enabled = selectedPresets.isNotEmpty(),
            ) { Text("Save selected (${selectedPresets.size})") }
            OutlinedButton(
                onClick = {
                    val canonicalIds = selectedPresets.mapTo(mutableSetOf(), GeneralEqPreset::canonicalProfileId)
                    scope.launch {
                        onHideCanonicalProfiles(canonicalIds)
                        batchSelectedIds = emptySet()
                        onMessage("${canonicalIds.size} ${if (canonicalIds.size == 1) "EQ" else "EQs"} hidden. Restore them in Settings → Hidden EQs.")
                    }
                },
                enabled = selectedPresets.isNotEmpty(),
            ) { Text("Hide selected") }
        }

        if (matching.isEmpty()) {
            Text(
                text = if (catalog.generalPresets.isEmpty()) {
                    "No visible General EQs are available. Hidden EQs can be restored from Settings → Hidden EQs."
                } else {
                    "No General EQs match this search and filter."
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
                                    ).joinToString(" · ").ifBlank { "General parametric EQ" },
                                )
                                if (preset.id in savedPresetIds) {
                                    Text(
                                        "Saved in My EQs for this output",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (!preset.isVerified) {
                                    Text(
                                        "Community submission — not independently verified.",
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                preset.sourceUrl?.let { sourceUrl ->
                                    TextButton(onClick = { onOpenUrl(sourceUrl) }) {
                                        Text("Source")
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

''',
)

# ---- Settings → Hidden EQs --------------------------------------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """package com.weekssa.opraeqforuapp.ui.screens\n\nimport androidx.compose.foundation.clickable\n""",
    """package com.weekssa.opraeqforuapp.ui.screens\n\nimport androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.clickable\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import androidx.compose.foundation.layout.Column\n""",
    """import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.fillMaxSize\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n""",
    """import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import androidx.compose.material3.Checkbox\n""",
    """import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.automirrored.outlined.ArrowBack\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.Checkbox\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import androidx.compose.runtime.Composable\n""",
    """import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.rememberCoroutineScope\nimport androidx.compose.runtime.saveable.rememberSaveable\nimport androidx.compose.runtime.setValue\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import com.weekssa.opraeqforuapp.domain.export.ExportDevice\n""",
    """import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog\nimport com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile\nimport com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision\nimport com.weekssa.opraeqforuapp.domain.export.ExportDevice\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """import java.util.Date\n""",
    """import java.util.Date\nimport kotlinx.coroutines.launch\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """    onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,\n    modifier: Modifier = Modifier,\n) {\n    Column(\n""",
    """    onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,\n    hiddenCanonicalProfileIds: Set<String>,\n    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,\n    onMessage: (String) -> Unit,\n    modifier: Modifier = Modifier,\n) {\n    var hiddenEqScreenOpen by rememberSaveable { mutableStateOf(false) }\n    if (hiddenEqScreenOpen) {\n        HiddenEqSettingsScreen(\n            catalogState = catalogState,\n            hiddenCanonicalProfileIds = hiddenCanonicalProfileIds,\n            onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,\n            onMessage = onMessage,\n            onBack = { hiddenEqScreenOpen = false },\n            modifier = modifier,\n        )\n        return\n    }\n\n    Column(\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """        }\n\n        SectionDivider()\n        SectionTitle(\"Export folder\")\n""",
    """        }\n        TextButton(onClick = { hiddenEqScreenOpen = true }) {\n            Text(\"Hidden EQs · ${hiddenCanonicalProfileIds.size}\")\n        }\n        Text(\n            text = \"Hidden EQs remain in the living archive and in any existing My EQs collection; this setting changes ordinary library visibility only.\",\n            style = MaterialTheme.typography.bodySmall,\n            color = MaterialTheme.colorScheme.onSurfaceVariant,\n        )\n\n        SectionDivider()\n        SectionTitle(\"Export folder\")\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/SettingsScreen.kt",
    """@Composable\nprivate fun SectionTitle(title: String) {\n""",
    r'''private data class HiddenEqRow(
    val canonicalProfileId: String,
    val title: String,
    val subtitle: String,
)

@Composable
private fun HiddenEqSettingsScreen(
    catalogState: CatalogState,
    hiddenCanonicalProfileIds: Set<String>,
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var selectedIds by remember(hiddenCanonicalProfileIds) { mutableStateOf<Set<String>>(emptySet()) }
    val rows = remember(catalogState, hiddenCanonicalProfileIds) {
        hiddenEqRows(catalogState, hiddenCanonicalProfileIds)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("Settings", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            "Hidden EQs",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Hidden items are still archived. No rows are selected by default; choose the EQs you want to make visible again.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            TextButton(onClick = { selectedIds = rows.mapTo(mutableSetOf(), HiddenEqRow::canonicalProfileId) }) {
                Text("Select all")
            }
            TextButton(onClick = { selectedIds = emptySet() }) { Text("Select none") }
        }
        Button(
            onClick = {
                val toUnhide = selectedIds
                scope.launch {
                    onUnhideCanonicalProfiles(toUnhide)
                    selectedIds = emptySet()
                    onMessage("${toUnhide.size} ${if (toUnhide.size == 1) "EQ" else "EQs"} unhidden.")
                }
            },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) { Text("Unhide selected (${selectedIds.size})") }

        if (rows.isEmpty()) {
            Text(
                "No EQs are hidden.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = HiddenEqRow::canonicalProfileId) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (row.canonicalProfileId in selectedIds) {
                                    selectedIds - row.canonicalProfileId
                                } else {
                                    selectedIds + row.canonicalProfileId
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = row.canonicalProfileId in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + row.canonicalProfileId
                                else selectedIds - row.canonicalProfileId
                            },
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(row.title)
                            Text(
                                row.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun hiddenEqRows(
    catalogState: CatalogState,
    hiddenCanonicalProfileIds: Set<String>,
): List<HiddenEqRow> {
    if (hiddenCanonicalProfileIds.isEmpty()) return emptyList()
    val catalog = (catalogState as? CatalogState.Ready)?.catalog
    if (catalog == null) {
        return hiddenCanonicalProfileIds.sorted().map { id ->
            HiddenEqRow(id, "Archived EQ", "Catalog details unavailable · $id")
        }
    }

    val rows = mutableListOf<HiddenEqRow>()
    val found = mutableSetOf<String>()
    catalog.profiles
        .filter { it.canonicalProfileId in hiddenCanonicalProfileIds }
        .groupBy(OpraEqProfile::canonicalProfileId)
        .forEach { (canonicalId, revisions) ->
            val profile = revisions.firstOrNull { !it.isHistoricalRevision() } ?: revisions.first()
            val product = catalog.product(profile.productId)
            val vendor = product?.let { catalog.vendor(it.vendorId) }
            rows += HiddenEqRow(
                canonicalProfileId = canonicalId,
                title = product?.name ?: "Headphone EQ",
                subtitle = listOfNotNull(
                    vendor?.name,
                    profile.author?.takeIf(String::isNotBlank),
                    profile.details?.takeIf(String::isNotBlank),
                ).joinToString(" · ").ifBlank { canonicalId },
            )
            found += canonicalId
        }
    catalog.generalPresets
        .filter { it.canonicalProfileId in hiddenCanonicalProfileIds }
        .groupBy { it.canonicalProfileId }
        .forEach { (canonicalId, revisions) ->
            val preset = revisions.firstOrNull { it.isLatestRevision } ?: revisions.first()
            rows += HiddenEqRow(
                canonicalProfileId = canonicalId,
                title = preset.displayName,
                subtitle = listOfNotNull(
                    "General EQ",
                    preset.creator?.takeIf(String::isNotBlank),
                    preset.category.name.lowercase().replaceFirstChar(Char::titlecase),
                ).joinToString(" · "),
            )
            found += canonicalId
        }
    (hiddenCanonicalProfileIds - found).forEach { id ->
        rows += HiddenEqRow(id, "Archived EQ", "Catalog details unavailable · $id")
    }
    return rows.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

@Composable
private fun SectionTitle(title: String) {
''',
)

# ---- App shell wiring: batch General export + hide callbacks ----------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """    data class GeneralEq(val presetId: String, override val device: ExportDevice) : ActiveOutputExportRequest\n}\n""",
    """    data class GeneralEq(val presetId: String, override val device: ExportDevice) : ActiveOutputExportRequest\n    data class GeneralEqBatch(val presetIds: Set<String>, override val device: ExportDevice) : ActiveOutputExportRequest\n}\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onToggleGeneralPreset: suspend (GeneralEqPreset) -> Boolean,\n    onImportPersonal: suspend (String, String, String, String?, String) -> String?,\n""",
    """    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,\n    onSaveGeneralPreset: suspend (GeneralEqPreset) -> Boolean,\n    onHideCanonicalProfiles: suspend (Set<String>) -> Unit,\n    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,\n    onImportPersonal: suspend (String, String, String, String?, String) -> SavedEqRecord,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """    onExportGeneralEq: suspend (Uri, String, ExportDevice) -> PresetExportSummary,\n    onCheckForUpdates: suspend () -> AppUpdateCheckResult,\n""",
    """    onExportGeneralEq: suspend (Uri, String, ExportDevice) -> PresetExportSummary,\n    onExportGeneralEqs: suspend (Uri, Set<String>, ExportDevice) -> PresetExportSummary,\n    onCheckForUpdates: suspend () -> AppUpdateCheckResult,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """            is ActiveOutputExportRequest.GeneralEq -> onExportGeneralEq(uri, request.presetId, request.device)\n            null -> null\n""",
    """            is ActiveOutputExportRequest.GeneralEq -> onExportGeneralEq(uri, request.presetId, request.device)\n            is ActiveOutputExportRequest.GeneralEqBatch -> onExportGeneralEqs(uri, request.presetIds, request.device)\n            null -> null\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """    val requestExportGeneralEq: (String) -> Unit = { presetId ->\n        runExportRequest(ActiveOutputExportRequest.GeneralEq(presetId, activeOutput))\n    }\n""",
    """    val requestExportGeneralEq: (String) -> Unit = { presetId ->\n        runExportRequest(ActiveOutputExportRequest.GeneralEq(presetId, activeOutput))\n    }\n    val requestExportGeneralEqs: (Set<String>) -> Unit = { presetIds ->\n        runExportRequest(ActiveOutputExportRequest.GeneralEqBatch(presetIds, activeOutput))\n    }\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """                        favoriteProfileIds = favoriteProfileIds,\n                        savedGeneralPresetIds = savedGeneralPresetIds,\n                        onToggleFavorite = onToggleFavorite,\n                        onToggleGeneralPreset = { preset ->\n                            val selected = onToggleGeneralPreset(preset)\n                            if (selected) requestExportGeneralEq(preset.id)\n                            selected\n                        },\n""",
    """                        favoriteProfileIds = favoriteProfileIds,\n                        savedGeneralPresetIds = savedGeneralPresetIds,\n                        hiddenCanonicalProfileIds = appPreferences.hiddenCanonicalProfileIds,\n                        onToggleFavorite = onToggleFavorite,\n                        onSaveGeneralPresets = { presets ->\n                            val presetIds = presets.mapTo(mutableSetOf(), GeneralEqPreset::id)\n                            presets.forEach { preset -> onSaveGeneralPreset(preset) }\n                            if (presetIds.isNotEmpty()) requestExportGeneralEqs(presetIds)\n                            presetIds.size\n                        },\n                        onHideCanonicalProfiles = onHideCanonicalProfiles,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/ui/EqLibraryApp.kt",
    """                        onDirectBlackPearlFlashEnabledChange = onDirectBlackPearlFlashEnabledChange,\n                        modifier = Modifier.fillMaxSize(),\n""",
    """                        onDirectBlackPearlFlashEnabledChange = onDirectBlackPearlFlashEnabledChange,\n                        hiddenCanonicalProfileIds = appPreferences.hiddenCanonicalProfileIds,\n                        onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,\n                        onMessage = ::showMessage,\n                        modifier = Modifier.fillMaxSize(),\n""",
)

# ---- Activity/repository wiring ---------------------------------------------
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt",
    """                    onToggleGeneralPreset = { preset ->\n                        savedGeneralEqRepository.toggleForOutput(activeOutputId, preset)\n                    },\n                    onImportPersonal = { manufacturer, model, displayName, target, peqText ->\n                        importPersonalEq(activeOutputId, manufacturer, model, displayName, target, peqText)\n                    },\n""",
    """                    onSaveGeneralPreset = { preset ->\n                        savedGeneralEqRepository.saveForOutput(activeOutputId, preset)\n                    },\n                    onHideCanonicalProfiles = appPreferencesRepository::hideCanonicalProfiles,\n                    onUnhideCanonicalProfiles = appPreferencesRepository::unhideCanonicalProfiles,\n                    onImportPersonal = { manufacturer, model, displayName, target, peqText ->\n                        savedEqRepository.importPersonal(\n                            outputId = activeOutputId,\n                            manufacturer = manufacturer,\n                            model = model,\n                            displayName = displayName,\n                            target = target,\n                            peqText = peqText,\n                        )\n                    },\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt",
    """                    onExportGeneralEq = { uri, presetId, device ->\n                        exportGeneralEq(uri, presetId, device, activeOutputId)\n                    },\n                    onCheckForUpdates = updateCoordinator::checkNow,\n""",
    """                    onExportGeneralEq = { uri, presetId, device ->\n                        exportGeneralEq(uri, presetId, device, activeOutputId)\n                    },\n                    onExportGeneralEqs = { uri, presetIds, device ->\n                        exportGeneralEqs(uri, presetIds, device, activeOutputId)\n                    },\n                    onCheckForUpdates = updateCoordinator::checkNow,\n""",
)
replace_once(
    "app/src/main/java/com/weekssa/opraeqforuapp/MainActivity.kt",
    """    private suspend fun flashManagedProfile(\n""",
    """    private suspend fun exportGeneralEqs(\n        treeUri: Uri,\n        presetIds: Set<String>,\n        device: ExportDevice,\n        outputId: String,\n    ): PresetExportSummary {\n        val records = presetIds.sorted().mapNotNull { presetId ->\n            savedGeneralEqRepository.getForOutput(outputId, presetId)\n        }\n        return exportRepository.exportSelected(\n            treeUri = treeUri,\n            headphones = records.map(savedGeneralEqRepository::toExportRecord),\n            device = device,\n        )\n    }\n\n    private suspend fun flashManagedProfile(\n""",
)

# ---- Living-archive publication regression gate -----------------------------
replace_once(
    "tools/catalog_pipeline.py",
    """def atomic_write_json(path: Path, payload: Any) -> None:\n""",
    '''def archive_regression_errors(baseline: dict[str, Any], candidate: dict[str, Any]) -> list[str]:\n    """Reject loss or in-place acoustic mutation of already-published canonical history."""\n    errors: list[str] = []\n    candidate_profiles = {\n        str(profile.get("canonical_profile_id") or ""): profile\n        for profile in candidate.get("profiles", [])\n        if str(profile.get("canonical_profile_id") or "").strip()\n    }\n    for baseline_profile in baseline.get("profiles", []):\n        profile_id = str(baseline_profile.get("canonical_profile_id") or "").strip()\n        if not profile_id:\n            continue\n        candidate_profile = candidate_profiles.get(profile_id)\n        if candidate_profile is None:\n            errors.append(f"archived canonical profile disappeared: {profile_id}")\n            continue\n        candidate_revisions = {\n            str(revision.get("revision_id") or ""): revision\n            for revision in candidate_profile.get("revisions", [])\n            if str(revision.get("revision_id") or "").strip()\n        }\n        for baseline_revision in baseline_profile.get("revisions", []):\n            revision_id = str(baseline_revision.get("revision_id") or "").strip()\n            if not revision_id:\n                continue\n            candidate_revision = candidate_revisions.get(revision_id)\n            if candidate_revision is None:\n                errors.append(f"archived revision disappeared: {profile_id}/{revision_id}")\n                continue\n            old_fingerprint = str(baseline_revision.get("acoustic_fingerprint") or "").strip()\n            new_fingerprint = str(candidate_revision.get("acoustic_fingerprint") or "").strip()\n            if old_fingerprint and new_fingerprint != old_fingerprint:\n                errors.append(\n                    f"archived revision acoustic fingerprint changed in place: "\n                    f"{profile_id}/{revision_id}"\n                )\n    return errors\n\n\ndef atomic_write_json(path: Path, payload: Any) -> None:\n''',
)
replace_once(
    "tools/catalog_pipeline.py",
    """def publish_snapshot(candidate: Path, published: Path, last_known_good: Path) -> str:\n    snapshot = load_json(candidate)\n    errors = validate_snapshot(snapshot)\n    if errors:\n        raise RegistryError(\"candidate snapshot rejected: \" + \"; \".join(errors))\n""",
    """def publish_snapshot(\n    candidate: Path,\n    published: Path,\n    last_known_good: Path,\n    baseline: Path | None = None,\n) -> str:\n    snapshot = load_json(candidate)\n    errors = validate_snapshot(snapshot)\n    if errors:\n        raise RegistryError(\"candidate snapshot rejected: \" + \"; \".join(errors))\n    if baseline is not None and baseline.exists() and baseline.resolve() != candidate.resolve():\n        baseline_snapshot = load_json(baseline)\n        baseline_errors = validate_snapshot(baseline_snapshot)\n        if baseline_errors:\n            raise RegistryError(\"archive baseline rejected: \" + \"; \".join(baseline_errors))\n        archive_errors = archive_regression_errors(baseline_snapshot, snapshot)\n        if archive_errors:\n            raise RegistryError(\"living archive regression: \" + \"; \".join(archive_errors))\n""",
)
replace_once(
    "tools/catalog_pipeline.py",
    """def command_publish(args: argparse.Namespace) -> int:\n    digest = publish_snapshot(Path(args.candidate), Path(args.published), Path(args.last_known_good))\n""",
    """def command_publish(args: argparse.Namespace) -> int:\n    candidate = Path(args.candidate)\n    if args.baseline:\n        baseline: Path | None = Path(args.baseline)\n    else:\n        default_baseline = Path(\"catalog/catalog.json\")\n        baseline = default_baseline if default_baseline.exists() else None\n    digest = publish_snapshot(candidate, Path(args.published), Path(args.last_known_good), baseline=baseline)\n""",
)
replace_once(
    "tools/catalog_pipeline.py",
    """    publish.add_argument(\"--last-known-good\", default=\"catalog/catalog.last-known-good.json\")\n    publish.set_defaults(func=command_publish)\n""",
    """    publish.add_argument(\"--last-known-good\", default=\"catalog/catalog.last-known-good.json\")\n    publish.add_argument(\"--baseline\", help=\"Prior published catalog to enforce living-archive preservation; defaults to catalog/catalog.json when present.\")\n    publish.set_defaults(func=command_publish)\n""",
)

# ---- Regression tests --------------------------------------------------------
write(
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/library/PersonalEqStrictImportTest.kt",
    r'''package com.weekssa.opraeqforuapp.domain.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalEqStrictImportTest {
    @Test
    fun `strict personal import preserves supported Equalizer APO values`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            """
            Preamp: -6.4 dB
            Filter 1: ON PK Fc 31.5 Hz Gain 2.1 dB Q 0.70
            Filter 2: OFF PK Fc 120 Hz Gain -3.0 dB Q 1.40
            Filter 3: ON LS Fc 105 Hz Gain 1.8 dB Q 0.71
            Filter 4: ON HS Fc 8000 Hz Gain -1.2 dB Q 0.80
            """.trimIndent(),
        )

        assertTrue(parsed.errors.toString(), parsed.isValid)
        assertEquals(-6.4, parsed.parsedEq.preampGainDb!!, 0.0001)
        assertEquals(3, parsed.parsedEq.filters.size)
        assertEquals(EqFilterType.PEAK, parsed.parsedEq.filters[0].type)
        assertEquals(31.5, parsed.parsedEq.filters[0].frequencyHz, 0.0001)
        assertEquals(2.1, parsed.parsedEq.filters[0].gainDb!!, 0.0001)
        assertEquals(EqFilterType.LOW_SHELF, parsed.parsedEq.filters[1].type)
        assertEquals(EqFilterType.HIGH_SHELF, parsed.parsedEq.filters[2].type)
    }

    @Test
    fun `missing preamp remains null`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            "Filter 1: ON PK Fc 1000 Hz Gain -2 dB Q 2.0",
        )
        assertTrue(parsed.isValid)
        assertNull(parsed.parsedEq.preampGainDb)
    }

    @Test
    fun `malformed filter blocks save instead of silently importing partial EQ`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            """
            Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0
            Filter 2: ON PK Fc 500 Hz Q 1.0
            Filter 3: ON PK Fc 1000 Hz Gain -2 dB Q 2.0
            """.trimIndent(),
        )
        assertFalse(parsed.isValid)
        assertEquals(2, parsed.parsedEq.filters.size)
        assertTrue(parsed.errors.any { it.contains("Line 2") && it.contains("Gain") })
    }

    @Test
    fun `unsupported enabled filter blocks personal import`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            "Filter 1: ON LP Fc 12000 Hz Q 0.7",
        )
        assertFalse(parsed.isValid)
        assertTrue(parsed.errors.any { it.contains("unsupported active filter type LP") })
    }

    @Test
    fun `unsupported file contents fail clearly`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal("{\"filters\": []}")
        assertFalse(parsed.isValid)
        assertTrue(parsed.errors.single().contains("isn't supported yet"))
    }
}
''',
)
write(
    "app/src/test/java/com/weekssa/opraeqforuapp/domain/catalog/HiddenCanonicalProfilesTest.kt",
    r'''package com.weekssa.opraeqforuapp.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenCanonicalProfilesTest {
    @Test
    fun `hiding canonical lineage removes all revisions from browse but not unrelated EQs`() {
        val catalog = OpraCatalog(
            vendors = listOf(
                OpraVendor("v1", "Maker One"),
                OpraVendor("v2", "Maker Two"),
            ),
            products = listOf(
                OpraProduct("p1", "v1", "Model One", "headphones", ""),
                OpraProduct("p2", "v2", "Model Two", "headphones", ""),
            ),
            profiles = listOf(
                profile("latest-1", "p1", "canonical-one"),
                profile("history-1", "p1", "canonical-one"),
                profile("latest-2", "p2", "canonical-two"),
            ),
            generalPresets = listOf(
                GeneralEqPreset(
                    id = "g1@r1",
                    displayName = "Bass",
                    category = GeneralEqCategory.SOUND,
                    creator = "Creator",
                    soundImpactSummary = null,
                    sourceUrl = null,
                    preampGainDb = null,
                    bands = listOf(OpraBand("peak_dip", 100.0, 1.0, 1.0, null)),
                    canonicalProfileId = "general-one",
                ),
            ),
        )

        val visible = catalog.excludingHiddenCanonicalProfiles(setOf("canonical-one", "general-one"))

        assertEquals(listOf("latest-2"), visible.profiles.map(OpraEqProfile::id))
        assertEquals(listOf("p2"), visible.products.map(OpraProduct::id))
        assertEquals(listOf("v2"), visible.vendors.map(OpraVendor::id))
        assertTrue(visible.generalPresets.isEmpty())
        assertEquals(3, catalog.profiles.size) // original archive projection is untouched
    }

    @Test
    fun `future revision with same canonical identity remains hidden`() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v", "Maker")),
            products = listOf(OpraProduct("p", "v", "Model", "headphones", "")),
            profiles = listOf(
                profile("revision-a", "p", "lineage"),
                profile("revision-b", "p", "lineage"),
            ),
        )
        assertTrue(catalog.excludingHiddenCanonicalProfiles(setOf("lineage")).profiles.isEmpty())
    }

    private fun profile(id: String, productId: String, canonicalId: String) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Creator",
        details = "Latest",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(OpraBand("peak_dip", 1000.0, -1.0, 1.0, null)),
        canonicalProfileId = canonicalId,
    )
}
''',
)
write(
    "tools/test_catalog_archive_preservation.py",
    r'''#!/usr/bin/env python3
import unittest

from catalog_pipeline import archive_regression_errors


class ArchivePreservationTests(unittest.TestCase):
    def snapshot(self, *, include_profile=True, include_old_revision=True, old_fingerprint="fp-old"):
        revisions = []
        if include_old_revision:
            revisions.append({"revision_id": "r1", "acoustic_fingerprint": old_fingerprint})
        revisions.append({"revision_id": "r2", "acoustic_fingerprint": "fp-new"})
        profiles = []
        if include_profile:
            profiles.append({"canonical_profile_id": "profile-one", "revisions": revisions})
        return {"profiles": profiles}

    def test_missing_published_profile_is_rejected(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(include_profile=False))
        self.assertIn("archived canonical profile disappeared: profile-one", errors)

    def test_missing_published_revision_is_rejected(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(include_old_revision=False))
        self.assertIn("archived revision disappeared: profile-one/r1", errors)

    def test_published_revision_acoustics_cannot_change_in_place(self):
        errors = archive_regression_errors(self.snapshot(), self.snapshot(old_fingerprint="changed"))
        self.assertTrue(any("acoustic fingerprint changed in place" in error for error in errors))

    def test_metadata_only_candidate_preserves_archive(self):
        baseline = self.snapshot()
        candidate = self.snapshot()
        candidate["profiles"][0]["creator"] = "Updated attribution"
        self.assertEqual([], archive_regression_errors(baseline, candidate))


if __name__ == "__main__":
    unittest.main()
''',
)

print("Applied approved v0.3 archive / Hide-Unhide / personal-import implementation patch.")
