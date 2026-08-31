package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.library.legacyAcousticSignature
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility

data class ManagedCatalogChangeSummary(
    val newProfileCount: Int = 0,
    val updatedSelectedProfileCount: Int = 0,
    val removedSelectedProfileCount: Int = 0,
    /** Legacy field name retained for compatibility; now means the source row became unusable. */
    val becameNotCompatibleSelectedProfileCount: Int = 0,
    val affectedProductIds: Set<String> = emptySet(),
) {
    val hasRelevantChanges: Boolean
        get() = newProfileCount > 0 ||
            updatedSelectedProfileCount > 0 ||
            removedSelectedProfileCount > 0 ||
            becameNotCompatibleSelectedProfileCount > 0

    operator fun plus(other: ManagedCatalogChangeSummary) = ManagedCatalogChangeSummary(
        newProfileCount = newProfileCount + other.newProfileCount,
        updatedSelectedProfileCount = updatedSelectedProfileCount + other.updatedSelectedProfileCount,
        removedSelectedProfileCount = removedSelectedProfileCount + other.removedSelectedProfileCount,
        becameNotCompatibleSelectedProfileCount =
            becameNotCompatibleSelectedProfileCount + other.becameNotCompatibleSelectedProfileCount,
        affectedProductIds = affectedProductIds + other.affectedProductIds,
    )
}

internal data class ReconciledManagedProfiles(
    val profiles: List<ManagedProfileEntity>,
    val changes: ManagedCatalogChangeSummary,
    val profileIdsToDelete: Set<String> = emptySet(),
)

internal fun reconcileManagedProfiles(
    productId: String,
    productName: String,
    currentProfiles: List<OpraEqProfile>,
    existingProfiles: List<ManagedProfileEntity>,
    autoIncludeNewProfiles: Boolean,
    nowMillis: Long,
    snapshotCodec: ManagedProfileSnapshotCodec,
): ReconciledManagedProfiles {
    val existingById = existingProfiles.associateBy(ManagedProfileEntity::profileId)
    val existingSnapshots = existingProfiles.associateWith { entity ->
        runCatching { snapshotCodec.decode(entity.snapshotJson) }.getOrNull()
    }
    val existingWithSignatures = existingProfiles.mapNotNull { entity ->
        existingSnapshots[entity]
            ?.legacyAcousticSignature()
            ?.let { signature -> entity to signature }
    }
    val currentIds = currentProfiles.mapTo(mutableSetOf(), OpraEqProfile::id)
    val migratedAliasIds = mutableSetOf<String>()
    var newCount = 0
    var updatedSelectedCount = 0
    var removedSelectedCount = 0
    var becameUnusableSelectedCount = 0

    val reconciledCurrent = currentProfiles.map { profile ->
        val signature = profile.legacyAcousticSignature()
        val acousticAliases = if (signature == null) {
            emptyList()
        } else {
            existingWithSignatures
                .filter { (entity, existingSignature) ->
                    entity.profileId != profile.id && existingSignature == signature
                }
                .map { it.first }
        }
        migratedAliasIds += acousticAliases.map(ManagedProfileEntity::profileId)
        val exactExisting = existingById[profile.id]
        val existing = exactExisting ?: acousticAliases.preferredMigrationSource()
        val fingerprint = snapshotCodec.fingerprint(profile)
        val sourceUsable = profile.isUsableParametricSource()

        if (existing == null) {
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
            ManagedProfileEntity(
                profileId = profile.id,
                productId = productId,
                selected = selected,
                explicitlyExcluded = false,
                snapshotJson = snapshotCodec.encode(profile),
                fingerprint = fingerprint,
                firstSeenAtMillis = nowMillis,
                lastSeenAtMillis = nowMillis,
                isNewUnreviewed = true,
                isUpdatedUnreviewed = false,
                noLongerAvailable = false,
                generatedPresetName = generated?.presetName,
                generatedXml = generated?.xml,
                generatedFromFingerprint = generated?.fingerprint,
                generatedAtMillis = generated?.generatedAtMillis,
            )
        } else {
            val migrated = exactExisting == null
            val selectedBeforeMigration = if (migrated) {
                acousticAliases.any { it.selected }
            } else {
                existing.selected
            }
            val explicitlyExcludedBeforeMigration = if (migrated && !selectedBeforeMigration) {
                acousticAliases.any { it.explicitlyExcluded }
            } else {
                existing.explicitlyExcluded
            }
            val previousSnapshots = if (migrated) {
                acousticAliases.mapNotNull(existingSnapshots::get)
            } else {
                listOfNotNull(existingSnapshots[existing])
            }
            val previouslyVerified = previousSnapshots.any(OpraEqProfile::isVerified)
            val becameVerified = profile.isVerified && previousSnapshots.isNotEmpty() && !previouslyVerified
            val changed = fingerprint != existing.fingerprint || migrated
            val becameUnusable = selectedBeforeMigration && !sourceUsable
            if (changed && selectedBeforeMigration && !migrated) updatedSelectedCount += 1
            if (becameUnusable) becameUnusableSelectedCount += 1

            val selected = when {
                becameUnusable -> false
                selectedBeforeMigration -> true
                becameVerified &&
                    autoIncludeNewProfiles &&
                    sourceUsable &&
                    !explicitlyExcludedBeforeMigration &&
                    !profile.isHistoricalRevision() -> true
                else -> false
            }

            val uappNowRepresentable =
                profile.assessUappCompatibility().category != ProfileCompatibility.NotCompatible
            val shouldRegenerate = selected && sourceUsable && (
                changed ||
                    becameVerified ||
                    existing.generatedPresetName == null ||
                    existing.generatedFromFingerprint != fingerprint ||
                    (uappNowRepresentable && existing.generatedXml == null)
                )
            val generated = if (shouldRegenerate) {
                generateManagedPreset(productName, profile, fingerprint, nowMillis)
            } else {
                null
            }

            existing.copy(
                profileId = profile.id,
                productId = productId,
                selected = selected,
                explicitlyExcluded = explicitlyExcludedBeforeMigration,
                snapshotJson = snapshotCodec.encode(profile),
                fingerprint = fingerprint,
                firstSeenAtMillis = if (migrated) {
                    acousticAliases.minOfOrNull { it.firstSeenAtMillis } ?: existing.firstSeenAtMillis
                } else {
                    existing.firstSeenAtMillis
                },
                lastSeenAtMillis = nowMillis,
                isNewUnreviewed = if (migrated) acousticAliases.any { it.isNewUnreviewed } else existing.isNewUnreviewed,
                isUpdatedUnreviewed = if (migrated) {
                    acousticAliases.any { it.isUpdatedUnreviewed }
                } else {
                    existing.isUpdatedUnreviewed || (changed && selectedBeforeMigration)
                },
                noLongerAvailable = false,
                generatedPresetName = generated?.presetName ?: existing.generatedPresetName,
                // A changed current source that is no longer UAPP-representable must not keep a stale
                // XML artifact. Removed source rows are handled separately and keep their last output.
                generatedXml = if (generated != null) generated.xml else existing.generatedXml,
                generatedFromFingerprint = generated?.fingerprint ?: existing.generatedFromFingerprint,
                generatedAtMillis = generated?.generatedAtMillis ?: existing.generatedAtMillis,
            )
        }
    }

    val retainedRemoved = existingProfiles
        .asSequence()
        .filter { it.profileId !in currentIds && it.profileId !in migratedAliasIds }
        .map { existing ->
            if (!existing.noLongerAvailable && existing.selected) removedSelectedCount += 1
            existing.copy(noLongerAvailable = true)
        }
        .toList()

    val changes = ManagedCatalogChangeSummary(
        newProfileCount = newCount,
        updatedSelectedProfileCount = updatedSelectedCount,
        removedSelectedProfileCount = removedSelectedCount,
        becameNotCompatibleSelectedProfileCount = becameUnusableSelectedCount,
        affectedProductIds = if (
            newCount > 0 || updatedSelectedCount > 0 || removedSelectedCount > 0 || becameUnusableSelectedCount > 0
        ) {
            setOf(productId)
        } else {
            emptySet()
        },
    )

    return ReconciledManagedProfiles(
        profiles = (reconciledCurrent + retainedRemoved).sortedBy(ManagedProfileEntity::profileId),
        changes = changes,
        profileIdsToDelete = migratedAliasIds - currentIds,
    )
}

private fun List<ManagedProfileEntity>.preferredMigrationSource(): ManagedProfileEntity? =
    sortedWith(
        compareByDescending<ManagedProfileEntity> { it.selected }
            .thenByDescending { it.explicitlyExcluded }
            .thenBy { it.firstSeenAtMillis }
            .thenBy { it.profileId },
    ).firstOrNull()

internal data class GeneratedManagedPreset(
    val presetName: String,
    val xml: String?,
    val fingerprint: String,
    val generatedAtMillis: Long,
)

/**
 * Builds stable naming/fingerprint metadata for every usable selected source. UAPP XML is optional:
 * if the source cannot be represented by the established ToneBoosters converter, the canonical
 * source remains saved and selectable while generatedXml stays null.
 */
internal fun generateManagedPreset(
    productName: String,
    profile: OpraEqProfile,
    fingerprint: String,
    nowMillis: Long,
): GeneratedManagedPreset {
    val presetName = ToneBoostersConverter.buildPresetName(
        modelLabel = productName,
        creator = profile.author,
        details = profile.details,
    )
    val result = runCatching { ToneBoostersConverter.convert(profile, presetName) }.getOrNull()
    return GeneratedManagedPreset(
        presetName = result?.presetName ?: presetName,
        xml = result?.xml,
        fingerprint = fingerprint,
        generatedAtMillis = nowMillis,
    )
}
