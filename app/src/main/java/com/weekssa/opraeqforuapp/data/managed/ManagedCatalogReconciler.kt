package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter

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
    val currentIds = currentProfiles.mapTo(mutableSetOf(), OpraEqProfile::id)
    var newCount = 0
    var updatedSelectedCount = 0
    var removedSelectedCount = 0
    var becameNotCompatibleSelectedCount = 0

    val reconciledCurrent = currentProfiles.map { profile ->
        val existing = existingById[profile.id]
        val fingerprint = snapshotCodec.fingerprint(profile)
        val selectable = profile.assessCompatibility().category.isSelectable

        if (existing == null) {
            newCount += 1
            val selected = autoIncludeNewProfiles && selectable
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
            val changed = fingerprint != existing.fingerprint
            val wasSelected = existing.selected
            val becameNotCompatible = wasSelected && !selectable
            if (changed && wasSelected) updatedSelectedCount += 1
            if (becameNotCompatible) becameNotCompatibleSelectedCount += 1

            val selected = if (becameNotCompatible) false else existing.selected
            val shouldRegenerate = selected && selectable &&
                (changed || existing.generatedXml == null || existing.generatedFromFingerprint != fingerprint)
            val generated = if (shouldRegenerate) {
                generateManagedPreset(productName, profile, fingerprint, nowMillis)
            } else {
                null
            }

            existing.copy(
                selected = selected,
                snapshotJson = snapshotCodec.encode(profile),
                fingerprint = fingerprint,
                lastSeenAtMillis = nowMillis,
                isUpdatedUnreviewed = existing.isUpdatedUnreviewed || (changed && wasSelected),
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
        .filter { it.profileId !in currentIds }
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
    )
}

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
