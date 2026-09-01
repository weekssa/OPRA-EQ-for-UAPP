from pathlib import Path


def p(path: str) -> Path:
    return Path(path)


def replace_once(path: str, old: str, new: str) -> None:
    file = p(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {file}, found {count}: {old[:160]!r}")
    file.write_text(text.replace(old, new, 1))


def replace_between(path: str, start: str, end: str, new_body: str) -> None:
    file = p(path)
    text = file.read_text()
    a = text.find(start)
    if a < 0:
        raise SystemExit(f"Start marker not found in {file}: {start!r}")
    b = text.find(end, a + len(start))
    if b < 0:
        raise SystemExit(f"End marker not found in {file}: {end!r}")
    file.write_text(text[:a] + new_body + text[b:])


# Exact-selection domain. The legacy persisted/API boolean is retained through v0.3 migrations,
# but now means the per-headphone in-app review preference and never silently selects a profile.
p('app/src/main/java/com/weekssa/opraeqforuapp/domain/managed/ManagedSelection.kt').write_text('''package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource

/** New managed headphones notify about newly discovered EQs by default. */
const val DEFAULT_NOTIFY_NEW_PROFILES = true

/**
 * Legacy source/storage name retained through v0.3 migrations. It no longer means silent
 * selection; it is the per-headphone "Notify me about new EQs" preference.
 */
@Deprecated("Use DEFAULT_NOTIFY_NEW_PROFILES")
const val DEFAULT_AUTO_INCLUDE_NEW_PROFILES = DEFAULT_NOTIFY_NEW_PROFILES

data class StoredProfileSelection(
    val selected: Boolean,
    val explicitlyExcluded: Boolean,
)

data class ManagedHeadphoneSelection(
    val productId: String,
    val autoIncludeNewProfiles: Boolean,
    val profileSelections: Map<String, StoredProfileSelection>,
) {
    fun isSelected(profile: OpraEqProfile): Boolean {
        if (!profile.isUsableParametricSource()) return false
        return profileSelections[profile.id]?.selected == true
    }
}

/** A never-managed headphone starts with an explicit empty selection. */
fun defaultStagedSelectedProfileIds(@Suppress("UNUSED_PARAMETER") profiles: List<OpraEqProfile>): Set<String> =
    emptySet()

fun managedSelectionCommitEnabled(
    isManaged: Boolean,
    stagedSelectedProfileIds: Set<String>,
    baselineSelectedProfileIds: Set<String>,
    @Suppress("UNUSED_PARAMETER") autoIncludeNewProfiles: Boolean,
    @Suppress("UNUSED_PARAMETER") baselineAutoIncludeNewProfiles: Boolean,
): Boolean {
    if (!isManaged) return stagedSelectedProfileIds.isNotEmpty()
    return stagedSelectedProfileIds != baselineSelectedProfileIds
}

fun selectionUpdatesForSave(
    profiles: List<OpraEqProfile>,
    stagedSelectedProfileIds: Set<String>,
    @Suppress("UNUSED_PARAMETER") autoIncludeNewProfiles: Boolean,
): Map<String, StoredProfileSelection> {
    val profileById = profiles.associateBy(OpraEqProfile::id)
    val invalidSelectedIds = stagedSelectedProfileIds.filter { profileId ->
        val profile = profileById[profileId]
        profile == null || !profile.isUsableParametricSource()
    }
    require(invalidSelectedIds.isEmpty()) {
        "Selection contains unknown or unusable source profile IDs: ${invalidSelectedIds.joinToString()}"
    }

    return profiles.associate { profile ->
        val selected = profile.isUsableParametricSource() && profile.id in stagedSelectedProfileIds
        profile.id to StoredProfileSelection(
            selected = selected,
            explicitlyExcluded = false,
        )
    }
}

fun selectableProfileIds(
    profiles: List<OpraEqProfile>,
    includeHistorical: Boolean = false,
    verifiedOnly: Boolean = false,
): Set<String> = profiles.asSequence()
    .filter(OpraEqProfile::isUsableParametricSource)
    .filter { includeHistorical || !it.isHistoricalRevision() }
    .filter { !verifiedOnly || it.isVerified }
    .map(OpraEqProfile::id)
    .toSet()
''')

# Catalog reconciliation: future profiles are review candidates, never automatic selections.
path = 'app/src/main/java/com/weekssa/opraeqforuapp/data/managed/ManagedCatalogReconciler.kt'
replace_once(path,
'''        if (existing == null) {
            newCount += 1
            val selected = autoIncludeNewProfiles &&
                sourceUsable &&
                profile.isVerified &&
                !profile.isHistoricalRevision()
            val generated = if (selected) {
                generateManagedPreset(productName, profile, fingerprint, nowMillis)
            } else {
                null
            }
            ManagedProfileEntity(''',
'''        if (existing == null) {
            newCount += 1
            val selected = false
            val generated = null
            ManagedProfileEntity(''')
replace_once(path,
'''                isNewUnreviewed = true,
                isUpdatedUnreviewed = false,''',
'''                isNewUnreviewed = autoIncludeNewProfiles,
                isUpdatedUnreviewed = false,''')
replace_once(path,
'''            val selected = when {
                becameUnusable -> false
                selectedBeforeMigration -> true
                becameVerified &&
                    autoIncludeNewProfiles &&
                    sourceUsable &&
                    !explicitlyExcludedBeforeMigration &&
                    !profile.isHistoricalRevision() -> true
                else -> false
            }''',
'''            val selected = when {
                becameUnusable -> false
                selectedBeforeMigration -> true
                else -> false
            }''')
replace_once(path,
'''                    existing.isUpdatedUnreviewed || (changed && selectedBeforeMigration)
                },''',
'''                    existing.isUpdatedUnreviewed ||
                        (autoIncludeNewProfiles && changed && selectedBeforeMigration)
                },''')

# Repository semantics: shared legacy field = headphone notification preference; output selections exact.
path = 'app/src/main/java/com/weekssa/opraeqforuapp/data/managed/ManagedHeadphonesRepository.kt'
replace_once(path,
'''     * Returns only the headphones saved for one output context. Canonical/local source snapshots
     * remain shared, while selected/excluded flags and automatic-new-profile policy are projected
     * from the output-scoped tables.
''',
'''     * Returns only the headphones saved for one output context. Canonical/local source snapshots
     * remain shared, while selected flags are projected from the output-scoped tables. The legacy
     * autoIncludeNewProfiles field is the headphone-level new-EQ review preference.
''')
replace_once(path,
'''                    // Legacy/shared field is retained for migration compatibility only. OR keeps it
                    // conservative while output-specific rows are the actual source of truth.
                    autoIncludeNewProfiles = (existingHeadphone?.autoIncludeNewProfiles ?: false) || autoIncludeNewProfiles,''',
'''                    // Legacy column name retained for migration compatibility. It now stores the
                    // headphone-level "Notify me about new EQs" preference.
                    autoIncludeNewProfiles = autoIncludeNewProfiles,''')
replace_once(path,
'''                    // Shared state only tracks the union. Output-scoped policy is reconciled below.
                    autoIncludeNewProfiles = false,''',
'''                    // Legacy parameter name: controls whether new/changed profiles become review items.
                    autoIncludeNewProfiles = headphone.autoIncludeNewProfiles,''')
replace_between(path,
'''    private suspend fun reconcileOutputSelection(
''',
'''    private suspend fun syncSharedSelectionUnion(productId: String) {
''',
'''    private suspend fun reconcileOutputSelection(
        output: OutputManagedHeadphoneEntity,
        currentProfiles: List<com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile>,
    ) {
        val existingById = dao.getOutputProfiles(output.outputId, output.productId)
            .associateBy(OutputManagedProfileEntity::profileId)
        val updates = currentProfiles.map { profile ->
            val existing = existingById[profile.id]
            val selected = profile.isUsableParametricSource() && existing?.selected == true
            OutputManagedProfileEntity(
                outputId = output.outputId,
                productId = output.productId,
                profileId = profile.id,
                selected = selected,
                explicitlyExcluded = false,
            )
        }
        if (updates.isNotEmpty()) dao.upsertOutputProfiles(updates)
    }

''')
replace_once(path,
'''    autoIncludeNewProfiles = output.autoIncludeNewProfiles,''',
'''    autoIncludeNewProfiles = autoIncludeNewProfiles,''')

# Selection editor becomes an exact chooser. It preserves the notification preference when saving,
# but no longer exposes it here.
path = 'app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt'
replace_once(path, 'import androidx.compose.material3.Switch\n', '')
replace_once(path,
'''    val dirty = initialized &&
        (stagedSelectedIds != baselineSelectedIds || autoInclude != baselineAutoInclude)''',
'''    val dirty = initialized && stagedSelectedIds != baselineSelectedIds''')
replace_once(path,
'''        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Automatically include new EQs",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(checked = autoInclude, onCheckedChange = { autoInclude = it })
        }

        Text(
            text = "New headphones start with no EQs selected. Unverified EQs are never added automatically. Output compatibility never hides or silently removes a saved source curve.",''',
'''        Text(
            text = "New headphones start with no EQs selected. New profiles always appear in EQ Library; saved selections change only when you choose them. My EQs can notify you when new verified or unverified profiles arrive.",''')

# Dedicated review surface. Merely opening/backing out never acknowledges the batch.
p('app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/NewEqReviewScreen.kt').write_text('''package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import kotlinx.coroutines.launch

@Composable
internal fun NewEqReviewScreen(
    headphone: ManagedHeadphoneRecord,
    activeOutput: ExportDevice,
    onAddSelected: suspend (Set<String>) -> Unit,
    onDismissBatch: suspend () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.isNewUnreviewed && !it.noLongerAvailable }
    }
    val updatedProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed }
    }
    var selectedIds by remember(headphone.productId, newProfiles.map { it.profileId }) {
        mutableStateOf<Set<String>>(emptySet())
    }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("My EQs", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = "New EQs for ${headphone.productName}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = buildList {
                if (newProfiles.isNotEmpty()) add("${newProfiles.size} new")
                if (updatedProfiles.isNotEmpty()) add("${updatedProfiles.size} updated")
            }.joinToString(" · "),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Choose any new EQs you want to add. Dismiss marks this batch reviewed without hiding or deleting anything. Back leaves the review pending.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (newProfiles.isNotEmpty()) {
                item(key = "new-heading") {
                    Text("New EQs", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                items(newProfiles, key = ManagedProfileRecord::profileId) { profile ->
                    val source = profile.lastKnownProfile
                    val checked = profile.profileId in selectedIds
                    ListItem(
                        leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                        headlineContent = { Text(source.author?.takeIf(String::isNotBlank) ?: "Creator information missing") },
                        supportingContent = {
                            Column {
                                Text(if (source.isVerified) "New · Verified" else "New · Unverified")
                                if (!source.isVerified) {
                                    Text(
                                        "Community submission — not independently verified. Review the source before use.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                source.details?.takeIf(String::isNotBlank)?.let { Text(it) }
                                Text(
                                    "${outputShortName(activeOutput)}: ${outputStatusLabel(assessDeviceExportability(source, activeOutput))}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                source.link?.takeIf(String::isNotBlank)?.let { url ->
                                    TextButton(onClick = { onOpenUrl(url) }) { Text("Source") }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { value ->
                                    selectedIds = if (value) selectedIds + profile.profileId else selectedIds - profile.profileId
                                },
                            ),
                    )
                    HorizontalDivider()
                }
            }

            if (updatedProfiles.isNotEmpty()) {
                item(key = "updated-heading") {
                    Text("Updated EQs", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                items(updatedProfiles, key = { "updated:${it.profileId}" }) { profile ->
                    val source = profile.lastKnownProfile
                    ListItem(
                        headlineContent = { Text(source.author?.takeIf(String::isNotBlank) ?: "Creator information missing") },
                        supportingContent = {
                            Column {
                                Text("Updated tuning · ${if (profile.selected) "already selected" else "not selected"}")
                                source.details?.takeIf(String::isNotBlank)?.let { Text(it) }
                                source.link?.takeIf(String::isNotBlank)?.let { url ->
                                    TextButton(onClick = { onOpenUrl(url) }) { Text("Source") }
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { scope.launch { onDismissBatch() } }) {
                Text(if (newProfiles.isEmpty()) "Done" else "Dismiss")
            }
            if (newProfiles.isNotEmpty()) {
                Button(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { scope.launch { onAddSelected(selectedIds) } },
                ) { Text("Add selected (${selectedIds.size})") }
            }
        }
    }
}

private fun outputShortName(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "UAPP / ToneBoosters"
    ExportDevice.BLACK_PEARL -> "Black Pearl"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Universal PEQ"
    ExportDevice.POWERAMP -> "Poweramp"
    ExportDevice.WAVELET -> "Wavelet"
}

private fun outputStatusLabel(status: DeviceExportability): String = when (status) {
    DeviceExportability.EXACT -> "Exact"
    DeviceExportability.OPTIMIZED -> "Optimized"
    DeviceExportability.NOT_REPRESENTABLE -> "Not exportable"
}
''')

# Managed detail: no acknowledgment on open; preference + explicit review actions live here.
path = 'app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt'
replace_once(path, 'import androidx.compose.material3.TextButton\n', 'import androidx.compose.material3.Switch\nimport androidx.compose.material3.TextButton\n')
replace_once(path, 'import androidx.compose.runtime.LaunchedEffect\n', '')
replace_once(path,
'''    var editing by remember(headphone.productId) { mutableStateOf(false) }
''',
'''    var editing by remember(headphone.productId) { mutableStateOf(false) }
    var reviewingNewEqs by remember(headphone.productId) { mutableStateOf(false) }
''')
replace_once(path,
'''    LaunchedEffect(headphone.productId) {
        onMarkReviewed(headphone.productId)
    }

''', '')
replace_once(path,
'''    if (editing && readyCatalog != null && product != null) {
''',
'''    val pendingNewCount = headphone.profiles.count { it.isNewUnreviewed && !it.noLongerAvailable }
    val pendingUpdatedCount = headphone.profiles.count {
        it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed
    }

    if (reviewingNewEqs) {
        NewEqReviewScreen(
            headphone = headphone,
            activeOutput = activeOutput,
            onAddSelected = { selectedNewIds ->
                val selectedIds = headphone.profiles
                    .filter(ManagedProfileRecord::selected)
                    .mapTo(mutableSetOf(), ManagedProfileRecord::profileId) + selectedNewIds
                onSaveSelection(headphone.productId, selectedIds, headphone.autoIncludeNewProfiles)
                onMarkReviewed(headphone.productId)
                if (selectedNewIds.isNotEmpty()) onExportProduct(headphone.productId)
                reviewingNewEqs = false
                onMessage("New EQ review completed.")
            },
            onDismissBatch = {
                onMarkReviewed(headphone.productId)
                reviewingNewEqs = false
                onMessage("New EQs marked reviewed. They remain available in EQ Library.")
            },
            onOpenUrl = onOpenUrl,
            onBack = { reviewingNewEqs = false },
            modifier = modifier,
        )
        return
    }

    if (editing && readyCatalog != null && product != null) {
''')
replace_once(path,
'''        Text(
            text = if (headphone.autoIncludeNewProfiles) {
                "Automatically include new verified EQ profiles: On"
            } else {
                "Automatically include new verified EQ profiles: Off"
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
''',
'''        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Notify me about new EQs", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Show an in-app review when new verified or unverified EQs, or a changed selected tuning, arrive for this headphone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = headphone.autoIncludeNewProfiles,
                onCheckedChange = { enabled ->
                    scope.launch {
                        val selectedIds = headphone.profiles
                            .filter(ManagedProfileRecord::selected)
                            .mapTo(mutableSetOf(), ManagedProfileRecord::profileId)
                        onSaveSelection(headphone.productId, selectedIds, enabled)
                        if (!enabled) onMarkReviewed(headphone.productId)
                        onMessage(
                            if (enabled) "New-EQ reviews enabled for ${headphone.productName}."
                            else "New-EQ reviews disabled for ${headphone.productName}.",
                        )
                    }
                },
            )
        }
        if (headphone.autoIncludeNewProfiles && (pendingNewCount > 0 || pendingUpdatedCount > 0)) {
            Button(
                onClick = { reviewingNewEqs = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    buildList {
                        if (pendingNewCount > 0) add("$pendingNewCount new")
                        if (pendingUpdatedCount > 0) add("$pendingUpdatedCount updated")
                    }.joinToString(" · ") + " — Review",
                )
            }
        }
''')
replace_once(path,
'''                    profile.explicitlyExcluded -> Text("Not selected · excluded from automatic inclusion")
                    else -> Text("Not selected")''',
'''                    else -> Text("Not selected")''')

# My EQs row gets a concise pending review indicator.
path = 'app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/MyEqsHomeScreen.kt'
replace_once(path,
'''                            supportingContent = {
                                Text(
                                    if (pendingCount > 0) {
                                        "${headphone.selectedProfileCount} selected · $pendingCount ${if (pendingCount == 1) "preset needs" else "presets need"} export"
                                    } else {
                                        "${headphone.selectedProfileCount} selected profiles"
                                    },
                                )
                            },''',
'''                            supportingContent = {
                                Column {
                                    Text(
                                        if (pendingCount > 0) {
                                            "${headphone.selectedProfileCount} selected · $pendingCount ${if (pendingCount == 1) "preset needs" else "presets need"} export"
                                        } else {
                                            "${headphone.selectedProfileCount} selected profiles"
                                        },
                                    )
                                    newEqAttentionText(headphone)?.let { attention ->
                                        Text(
                                            attention,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            },''')
file = p(path)
text = file.read_text()
marker = '@Composable\nfun BlackPearlConnectionControl('
idx = text.find(marker)
if idx < 0:
    raise SystemExit('MyEqsHomeScreen helper insertion marker not found')
helper = '''private fun newEqAttentionText(headphone: ManagedHeadphoneRecord): String? {
    if (!headphone.autoIncludeNewProfiles) return null
    val newCount = headphone.profiles.count { it.isNewUnreviewed && !it.noLongerAvailable }
    val updatedCount = headphone.profiles.count {
        it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed
    }
    return buildList {
        if (newCount > 0) add("$newCount new ${if (newCount == 1) "EQ" else "EQs"}")
        if (updatedCount > 0) add("$updatedCount ${if (updatedCount == 1) "EQ updated" else "EQs updated"}")
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

'''
file.write_text(text[:idx] + helper + text[idx:])

# Selection-domain regression tests.
p('app/src/test/java/com/weekssa/opraeqforuapp/domain/managed/ManagedSelectionTest.kt').write_text('''package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSelectionTest {
    @Test
    fun firstTimeDefaultsStartEmptyAndNewEqNotificationsOn() {
        val selected = defaultStagedSelectedProfileIds(listOf(compatibleProfile("profile")))
        assertTrue(DEFAULT_NOTIFY_NEW_PROFILES)
        assertTrue(selected.isEmpty())
    }

    @Test
    fun explicitSelectAllCanIncludeVerifiedAndUnverifiedUsableProfiles() {
        val verified = compatibleProfile("verified")
        val unverified = compatibleProfile("unverified").copy(isVerified = false)
        val selected = selectableProfileIds(listOf(verified, unverified))
        assertTrue("verified" in selected)
        assertTrue("unverified" in selected)
    }

    @Test
    fun notificationPreferenceDoesNotSelectFutureVerifiedOrUnverifiedProfiles() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )
        assertFalse(state.isSelected(compatibleProfile("verified")))
        assertFalse(state.isSelected(compatibleProfile("unverified").copy(isVerified = false)))
    }

    @Test
    fun explicitlyStoredSelectionRemainsSelected() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = mapOf(
                "selected" to StoredProfileSelection(selected = true, explicitlyExcluded = false),
            ),
        )
        assertTrue(state.isSelected(compatibleProfile("selected")))
        assertFalse(state.isSelected(compatibleProfile("future")))
    }

    @Test
    fun savingUsesExactSelectionAndCreatesNoAutomaticExclusions() {
        val profiles = listOf(compatibleProfile("selected"), compatibleProfile("not-selected"))
        val updates = selectionUpdatesForSave(
            profiles = profiles,
            stagedSelectedProfileIds = setOf("selected"),
            autoIncludeNewProfiles = true,
        )
        assertTrue(updates.getValue("selected").selected)
        assertFalse(updates.getValue("selected").explicitlyExcluded)
        assertFalse(updates.getValue("not-selected").selected)
        assertFalse(updates.getValue("not-selected").explicitlyExcluded)
    }

    @Test
    fun notificationOnlyChangeDoesNotMakeSelectionEditorDirty() {
        assertFalse(
            managedSelectionCommitEnabled(
                isManaged = true,
                stagedSelectedProfileIds = setOf("profile"),
                baselineSelectedProfileIds = setOf("profile"),
                autoIncludeNewProfiles = true,
                baselineAutoIncludeNewProfiles = false,
            ),
        )
    }

    private fun compatibleProfile(id: String) = OpraEqProfile(
        id = id,
        productId = "product",
        author = "Creator",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
    )
}
''')

# Reconciler tests: only replace the auto-selection-specific cases and leave all other coverage intact.
path = 'app/src/test/java/com/weekssa/opraeqforuapp/data/managed/ManagedCatalogReconcilerTest.kt'
replace_between(path,
'''    @Test
    fun newProfilesAutoIncludeUsableSourcesEvenWhenUappCannotRepresentOne() {
''',
'''    @Test
    fun newUnverifiedProfileIsNotSilentlySelectedWhenAutoIncludeIsOn() {
''',
'''    @Test
    fun newProfilesBecomeReviewableWithoutSilentSelectionWhenNotificationsAreOn() {
        val uappCompatible = compatibleProfile("new-compatible")
        val uappUnsupportedButUsable = compatibleProfile("new-uapp-unsupported").copy(
            bands = listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, 12.0)),
        )

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(uappCompatible, uappUnsupportedButUsable),
            existingProfiles = emptyList(),
            autoIncludeNewProfiles = true,
            nowMillis = 100L,
            snapshotCodec = codec,
        )

        result.profiles.forEach { profile ->
            assertFalse(profile.selected)
            assertTrue(profile.isNewUnreviewed)
            assertNull(profile.generatedXml)
        }
        assertEquals(2, result.changes.newProfileCount)
    }

''')
replace_between(path,
'''    @Test
    fun verificationPromotionAutoSelectsPreviouslyUnselectedProfileWhenAutoIncludeIsOn() {
''',
'''    @Test
    fun verificationPromotionDoesNotAutoSelectWhenAutoIncludeIsOff() {
''',
'''    @Test
    fun verificationPromotionNeverSilentlySelectsPreviouslyUnselectedProfile() {
        val unverified = compatibleProfile("community").copy(isVerified = false)
        val existing = existingEntity(unverified, selected = false, generatedXml = null)
        val verified = unverified.copy(isVerified = true)

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(verified),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = true,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertFalse(reconciled.selected)
        assertNull(reconciled.generatedXml)
        assertEquals(0, result.changes.updatedSelectedProfileCount)
    }

''')

# Guard against stale user-facing wording in the two affected screens.
for file_name, forbidden in {
    'app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ProfileSelectionEditor.kt': 'Automatically include new EQs',
    'app/src/main/java/com/weekssa/opraeqforuapp/ui/screens/ManagedHeadphoneDetailScreen.kt': 'Automatically include new verified EQ profiles',
}.items():
    if forbidden in p(file_name).read_text():
        raise SystemExit(f'Stale wording remains in {file_name}: {forbidden}')
