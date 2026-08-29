package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.library.legacyAcousticSignature

data class ManagedCatalogChangeSummary(
    val newProfileCount: Int = 0,
    val updatedSelectedProfileCount: Int = 0,
    val removedSelectedProfileCount: Int = 0,
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
    val existingWithSignatures = existingProfiles.mapNotNull { entity ->
        runCatching { snapshotCodec.decode(entity.snapshotJson) }
            .getOrNull()
            ?.legacyAcousticSignature()
            ?.let { signature -> entity to signature }
    }
    val currentIds = currentProfiles.mapTo(mutableSetOf(), OpraEqProfile::id)
    val migratedAliasIds = mutableSetOf<String>()
    var newCount = 0
    var updatedSelectedCount = 0
    var removedSelectedCount = 0
    var becameNotCompatibleSelectedCount = 0

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
        val selectable = profile.assessCompatibility().category.isSelectable

        if (existing == null) {
            newCount += 1
            val selected = autoIncludeNewProfiles && selectable && !profile.isHistoricalRevision()
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
            val changed = fingerprint != existing.fingerprint || migrated
            val becameNotCompatible = selectedBeforeMigration && !selectable
            if (changed && selectedBeforeMigration && !migrated) updatedSelectedCount += 1
            if (becameNotCompatible) becameNotCompatibleSelectedCount += 1

            val selected = if (becameNotCompatible) false else selectedBeforeMigration
            val shouldRegenerate = selected && selectable &&
                (changed || existing.generatedXml == null || existing.generatedFromFingerprint != fingerprint)
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
                generatedXml = generated?.xml ?: existing.generatedXml,
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
        becameNotCompatibleSelectedProfileCount = becameNotCompatibleSelectedCount,
        affectedProductIds = if (
            newCount > 0 || updatedSelectedCount > 0 || removedSelectedCount > 0 || becameNotCompatibleSelectedCount > 0
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
    val xml: String,
    val fingerprint: String,
    val generatedAtMillis: Long,
)

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
    val result = ToneBoostersConverter.convert(profile, presetName)
    return GeneratedManagedPreset(
        presetName = result.presetName,
        xml = result.xml,
        fingerprint = fingerprint,
        generatedAtMillis = nowMillis,
    )
}
