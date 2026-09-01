package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.naming.buildPresetName
import com.weekssa.opraeqforuapp.domain.naming.iso88591SafeName
import com.weekssa.opraeqforuapp.domain.xml.ToneBoostersXml
import com.weekssa.opraeqforuapp.domain.xml.ToneBoostersXmlConverter

data class ManagedCatalogReconcileChanges(
    val newProfileCount: Int,
    val updatedSelectedProfileCount: Int,
    val removedSelectedProfileCount: Int,
    val newlyIncompatibleSelectedProfileCount: Int,
) {
    val totalReviewCount: Int
        get() = newProfileCount + updatedSelectedProfileCount + removedSelectedProfileCount +
            newlyIncompatibleSelectedProfileCount
}

data class ManagedCatalogReconcileResult(
    val profiles: List<ManagedProfileEntity>,
    val changes: ManagedCatalogReconcileChanges,
)

fun reconcileManagedProfiles(
    productId: String,
    productName: String,
    currentProfiles: List<OpraEqProfile>,
    existingProfiles: List<ManagedProfileEntity>,
    autoIncludeNewProfiles: Boolean,
    nowMillis: Long,
    snapshotCodec: ManagedProfileSnapshotCodec,
): ManagedCatalogReconcileResult {
    val existingById = existingProfiles.associateBy(ManagedProfileEntity::profileId)
    val existingSnapshots = existingProfiles.associateWith { entity ->
        runCatching { snapshotCodec.decode(entity.snapshotJson) }.getOrNull()
    }
    val existingWithSignatures = existingProfiles.mapNotNull { entity ->
        val profile = existingSnapshots[entity] ?: return@mapNotNull null
        entity to profile.acousticMigrationSignature()
    }
    val currentAcousticSignatureCounts = currentProfiles
        .groupingBy(OpraEqProfile::acousticMigrationSignature)
        .eachCount()
    val currentIds = currentProfiles.mapTo(mutableSetOf(), OpraEqProfile::id)
    val migratedAliasIds = mutableSetOf<String>()

    var newCount = 0
    var updatedSelectedCount = 0
    var removedSelectedCount = 0
    var newlyIncompatibleSelectedCount = 0

    val reconciled = currentProfiles.map { profile ->
        val signature = profile.acousticMigrationSignature()
        val acousticAliases = if (currentAcousticSignatureCounts[signature] == 1) {
            existingWithSignatures
                .filter { (entity, existingSignature) ->
                    entity.profileId != profile.id && existingSignature == signature
                }
                .map { it.first }
        } else {
            emptyList()
        }
        migratedAliasIds += acousticAliases.map(ManagedProfileEntity::profileId)
        val exactExisting = existingById[profile.id]
        val existing = exactExisting ?: acousticAliases.preferredMigrationSource()
        val fingerprint = snapshotCodec.fingerprint(profile)
        val sourceUsable = profile.isUsableParametricSource()

        if (existing == null) {
            newCount += 1
            ManagedProfileEntity(
                profileId = profile.id,
                productId = productId,
                selected = false,
                explicitlyExcluded = false,
                snapshotJson = snapshotCodec.encode(profile),
                fingerprint = fingerprint,
                firstSeenAtMillis = nowMillis,
                lastSeenAtMillis = nowMillis,
                isNewUnreviewed = autoIncludeNewProfiles,
                isUpdatedUnreviewed = false,
                noLongerAvailable = false,
                generatedPresetName = null,
                generatedXml = null,
                generatedFromFingerprint = null,
                generatedAtMillis = null,
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
            val previousWasUsable = previousSnapshots.any(OpraEqProfile::isUsableParametricSource)
            val becameUnusable = selectedBeforeMigration && previousWasUsable && !sourceUsable
            val changed = existing.fingerprint != fingerprint
            val selected = when {
                becameUnusable -> false
                selectedBeforeMigration -> true
                else -> false
            }
            val generated = when {
                !selected -> null
                sourceUsable && (changed || existing.generatedXml == null) ->
                    generateManagedPreset(productName, profile, fingerprint, nowMillis)
                else -> GeneratedManagedPreset(
                    presetName = existing.generatedPresetName,
                    xml = existing.generatedXml,
                    fingerprint = existing.generatedFromFingerprint,
                    generatedAtMillis = existing.generatedAtMillis,
                )
            }

            if (changed && selectedBeforeMigration && sourceUsable) updatedSelectedCount += 1
            if (becameUnusable) newlyIncompatibleSelectedCount += 1

            ManagedProfileEntity(
                profileId = profile.id,
                productId = productId,
                selected = selected,
                explicitlyExcluded = false,
                snapshotJson = snapshotCodec.encode(profile),
                fingerprint = fingerprint,
                firstSeenAtMillis = existing.firstSeenAtMillis,
                lastSeenAtMillis = nowMillis,
                isNewUnreviewed = if (migrated) {
                    acousticAliases.any { it.isNewUnreviewed }
                } else {
                    existing.isNewUnreviewed
                },
                isUpdatedUnreviewed = if (migrated) {
                    acousticAliases.any { it.isUpdatedUnreviewed } ||
                        (autoIncludeNewProfiles && changed && selectedBeforeMigration)
                } else {
                    existing.isUpdatedUnreviewed ||
                        (autoIncludeNewProfiles && changed && selectedBeforeMigration)
                },
                noLongerAvailable = false,
                generatedPresetName = generated?.presetName,
                generatedXml = generated?.xml,
                generatedFromFingerprint = generated?.fingerprint,
                generatedAtMillis = generated?.generatedAtMillis,
            )
        }
    }.toMutableList()

    existingProfiles.forEach { existing ->
        if (existing.profileId !in currentIds && existing.profileId !in migratedAliasIds) {
            if (existing.selected && !existing.noLongerAvailable) removedSelectedCount += 1
            reconciled += existing.copy(
                noLongerAvailable = true,
                isUpdatedUnreviewed = existing.isUpdatedUnreviewed || existing.selected,
            )
        }
    }

    return ManagedCatalogReconcileResult(
        profiles = reconciled,
        changes = ManagedCatalogReconcileChanges(
            newProfileCount = newCount,
            updatedSelectedProfileCount = updatedSelectedCount,
            removedSelectedProfileCount = removedSelectedCount,
            newlyIncompatibleSelectedProfileCount = newlyIncompatibleSelectedCount,
        ),
    )
}

private fun List<ManagedProfileEntity>.preferredMigrationSource(): ManagedProfileEntity? =
    sortedWith(
        compareByDescending<ManagedProfileEntity> { it.selected }
            .thenByDescending { it.generatedXml != null }
            .thenByDescending { it.lastSeenAtMillis }
            .thenBy { it.profileId },
    ).firstOrNull()

private data class AcousticMigrationSignature(
    val normalizedProductId: String,
    val normalizedProfileType: String,
    val normalizedAuthor: String?,
    val normalizedDetails: String?,
    val normalizedPreamp: Double?,
    val normalizedBands: List<String>,
)

private fun OpraEqProfile.acousticMigrationSignature(): AcousticMigrationSignature = AcousticMigrationSignature(
    normalizedProductId = productId.trim().lowercase(),
    normalizedProfileType = profileType.trim().lowercase(),
    normalizedAuthor = author?.trim()?.lowercase(),
    normalizedDetails = details?.trim()?.lowercase(),
    normalizedPreamp = preampGainDb?.normalizedAcousticDouble(),
    normalizedBands = bands.orEmpty().map { band ->
        listOf(
            band.type.trim().lowercase(),
            band.frequency?.normalizedAcousticDouble()?.toString().orEmpty(),
            band.gainDb?.normalizedAcousticDouble()?.toString().orEmpty(),
            band.q?.normalizedAcousticDouble()?.toString().orEmpty(),
            band.slopeDbPerOctave?.normalizedAcousticDouble()?.toString().orEmpty(),
        ).joinToString("|")
    },
)

private fun Double.normalizedAcousticDouble(): Double =
    if (this == -0.0) 0.0 else this

private data class GeneratedManagedPreset(
    val presetName: String?,
    val xml: String?,
    val fingerprint: String?,
    val generatedAtMillis: Long?,
)

private fun generateManagedPreset(
    productName: String,
    profile: OpraEqProfile,
    fingerprint: String,
    nowMillis: Long,
): GeneratedManagedPreset? {
    val assessment = profile.assessCompatibility()
    if (assessment.status != ProfileCompatibility.COMPATIBLE &&
        assessment.status != ProfileCompatibility.LIMITED_TO_FIRST_10_BANDS
    ) {
        return null
    }
    val xml = runCatching { ToneBoostersXmlConverter.convert(profile) }.getOrNull() ?: return null
    val presetName = iso88591SafeName(buildPresetName(productName, profile))
    return GeneratedManagedPreset(
        presetName = presetName,
        xml = xml,
        fingerprint = fingerprint,
        generatedAtMillis = nowMillis,
    )
}

private fun OpraEqProfile.assessCompatibility() =
    com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility(this)
