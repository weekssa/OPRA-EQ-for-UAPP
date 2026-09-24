package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.EqBandOrderProvenance
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Compatibility bridge for the v0.2 managed-headphone/export engine plus the v0.3 General EQ UI.
 *
 * Canonical headphone revisions are projected as selectable legacy profiles so the stable v0.2
 * selection/export engine can support revision-aware behavior. General presets are projected into
 * a separate collection with no headphone/product identity; this prevents Effect/Genre presets
 * from being represented as fake headphones while still keeping one canonical catalog snapshot.
 */
object CanonicalLegacyCatalogAdapter {
    fun adapt(snapshot: CatalogSnapshot): OpraCatalog {
        val vendors = linkedMapOf<String, OpraVendor>()
        val products = mutableListOf<OpraProduct>()
        val profiles = mutableListOf<OpraEqProfile>()

        snapshot.profiles
            .filter(CanonicalEqProfile::isHeadphoneProfile)
            .groupBy { requireNotNull(it.headphone).normalizedKey }
            .toSortedMap()
            .values
            .forEach { canonicalProfiles ->
                val representative = canonicalProfiles.first()
                val headphone = requireNotNull(representative.headphone)
                val identity = legacyIdentity(canonicalProfiles)
                vendors.putIfAbsent(
                    identity.vendorId,
                    OpraVendor(id = identity.vendorId, name = headphone.manufacturer),
                )
                products += OpraProduct(
                    id = identity.productId,
                    vendorId = identity.vendorId,
                    name = displayProductName(headphone),
                    type = "headphones",
                    subtype = "",
                    aliases = canonicalProfiles
                        .flatMap { requireNotNull(it.headphone).modelAliases }
                        .distinct(),
                )
                canonicalProfiles.forEach { canonical ->
                    profiles += revisionProfiles(canonical, identity.vendorId, identity.productId)
                }
            }

        val generalPresets = snapshot.profiles
            .filter(CanonicalEqProfile::isGeneralPreset)
            .flatMap(::generalRevisionPresets)
            .distinctBy(GeneralEqPreset::id)
            .sortedWith(
                compareBy<GeneralEqPreset> { it.category.ordinal }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
                    .thenBy { it.id },
            )

        return OpraCatalog(
            vendors = vendors.values.toList(),
            products = products.distinctBy(OpraProduct::id),
            profiles = profiles.distinctBy(OpraEqProfile::id),
            generalPresets = generalPresets,
        )
    }

    /** Resolve only when a displayed legacy view exactly matches one current canonical revision. */
    fun resolveSelection(snapshot: CatalogSnapshot, legacy: OpraEqProfile): CanonicalEqSelection? {
        val matches = snapshot.profiles.asSequence()
            .filter(CanonicalEqProfile::isHeadphoneProfile)
            .filter { it.canonicalProfileId == legacy.canonicalProfileId }
            .flatMap { profile ->
                val headphone = requireNotNull(profile.headphone)
                val group = snapshot.profiles.filter { candidate ->
                    candidate.isHeadphoneProfile && candidate.headphone?.normalizedKey == headphone.normalizedKey
                }
                val identity = legacyIdentity(group)
                profile.revisions.asSequence().map { revision ->
                    val selection = CanonicalEqSelection(
                        profile = profile,
                        selectedRevisionId = revision.revisionId,
                        compatibilityVendorId = identity.vendorId,
                        compatibilityProductId = identity.productId,
                    )
                    val projected = revisionProfile(profile, revision, identity.vendorId, identity.productId)
                    selection to projected
                }
            }
            .filter { (_, projected) -> projected.matchesCanonicalProjection(legacy) }
            .map { it.first }
            .distinct()
            .toList()
        return matches.singleOrNull()
    }

    /** Resolve General EQ only against the exact current canonical preset/revision projection. */
    fun resolveSelection(snapshot: CatalogSnapshot, preset: GeneralEqPreset): CanonicalEqSelection? {
        val matches = snapshot.profiles.asSequence()
            .filter(CanonicalEqProfile::isGeneralPreset)
            .flatMap { profile ->
                profile.revisions.asSequence().map { revision ->
                    val selection = CanonicalEqSelection(profile, revision.revisionId)
                    selection to generalRevisionPreset(profile, revision)
                }
            }
            .filter { (_, projected) -> projected == preset }
            .map { it.first }
            .distinct()
            .toList()
        return matches.singleOrNull()
    }

    /** Compatibility view for existing consumers, derived from the canonical selection on read. */
    fun projectSelection(selection: CanonicalEqSelection, productId: String): OpraEqProfile {
        require(selection.profile.isHeadphoneProfile) { "Headphone action requires a canonical headphone profile" }
        require(productId.isNotBlank()) { "Compatibility product identity must not be blank" }
        require(selection.compatibilityProductId == null || selection.compatibilityProductId == productId) {
            "Compatibility product identity does not match the canonical selection"
        }
        val revision = selection.selectedRevision
        val primary = revision.sourceReferences.filter(EqSourceReference::isPrimary).singleOrNull()
        val vendorId = selection.compatibilityVendorId
            ?: primary?.sourceVendorId
            ?: "eq-library-vendor:${slug(requireNotNull(selection.profile.headphone).manufacturer)}"
        return revisionProfile(selection.profile, revision, vendorId, productId)
    }

    fun matchesSelection(
        selection: CanonicalEqSelection,
        legacy: OpraEqProfile,
        productId: String = legacy.productId,
    ): Boolean {
        if (selection.profile.canonicalProfileId != legacy.canonicalProfileId) return false
        val projected = runCatching { projectSelection(selection, productId) }.getOrNull() ?: return false
        return legacy.matchesCanonicalProjection(projected)
    }

    /** Compatibility view for existing General EQ export/action consumers. */
    fun projectGeneralSelection(
        selection: CanonicalEqSelection,
        presetId: String,
    ): OpraEqProfile {
        require(selection.profile.isGeneralPreset) { "General EQ action requires a canonical general preset" }
        require(presetId.isNotBlank()) { "General EQ preset identity must not be blank" }
        val preset = generalRevisionPreset(selection.profile, selection.selectedRevision)
        return OpraEqProfile(
            id = presetId,
            productId = GENERAL_PRODUCT_ID,
            canonicalProfileId = selection.profile.canonicalProfileId,
            author = preset.creator,
            details = preset.soundImpactSummary,
            link = preset.sourceUrl,
            profileType = "parametric_eq",
            preampGainDb = preset.preampGainDb,
            bands = preset.bands,
            eqLibrarySafetyHeadroomDb = preset.eqLibrarySafetyHeadroomDb,
            isVerified = preset.isVerified,
        )
    }

    fun matchesGeneralSelection(
        selection: CanonicalEqSelection,
        legacy: OpraEqProfile,
        presetId: String,
    ): Boolean {
        if (!selection.profile.isGeneralPreset) return false
        val projected = runCatching { projectGeneralSelection(selection, presetId) }.getOrNull() ?: return false
        return legacy.matchesCanonicalProjection(projected)
    }

    fun projectGeneralPreset(selection: CanonicalEqSelection): GeneralEqPreset {
        require(selection.profile.isGeneralPreset) { "General EQ action requires a canonical general preset" }
        return generalRevisionPreset(selection.profile, selection.selectedRevision)
    }

    fun isSameGeneralSelection(selection: CanonicalEqSelection, preset: GeneralEqPreset): Boolean =
        selection.profile.isGeneralPreset && projectGeneralPreset(selection) == preset

    private fun revisionProfiles(
        profile: CanonicalEqProfile,
        vendorId: String,
        productId: String,
    ): List<OpraEqProfile> =
        profile.revisions
            .sortedWith(
                compareByDescending<EqRevision> { it.isLatest }
                    .thenByDescending {
                        it.sourceUpdatedAtEpochSeconds ?: it.firstSeenAtEpochSeconds ?: Long.MIN_VALUE
                    },
            )
            .map { revision -> revisionProfile(profile, revision, vendorId, productId) }

    private fun generalRevisionPresets(profile: CanonicalEqProfile): List<GeneralEqPreset> =
        profile.revisions
            .sortedWith(
                compareByDescending<EqRevision> { it.isLatest }
                    .thenByDescending {
                        it.sourceUpdatedAtEpochSeconds ?: it.firstSeenAtEpochSeconds ?: Long.MIN_VALUE
                    },
            )
            .map { revision ->
                generalRevisionPreset(profile, revision)
            }

    private fun generalRevisionPreset(profile: CanonicalEqProfile, revision: EqRevision): GeneralEqPreset {
        val primary = revision.sourceReferences.firstOrNull { it.isPrimary }
            ?: revision.sourceReferences.firstOrNull()
        val baseName = profile.tuningLabel?.takeIf(String::isNotBlank)
            ?: profile.target.name?.takeIf(String::isNotBlank)
            ?: when (profile.purpose) {
                EqPresetPurpose.GENRE -> "Genre EQ"
                else -> "Sound EQ"
            }
        val displayName = if (revision.isLatest) {
            baseName
        } else {
            "$baseName · Previous revision${revisionDisplayDate(revision, primary)?.let { " · $it" }.orEmpty()}"
        }
        return GeneralEqPreset(
            id = generalPresetId(profile, revision),
            displayName = displayName,
            canonicalProfileId = profile.canonicalProfileId,
            category = generalCategory(profile, revision),
            creator = profile.creator ?: primary?.creator,
            soundImpactSummary = revision.soundImpactSummary
                ?: SoundImpactSummary.fromFilters(revision.filters)
                    ?.let { "EQ Library summary: $it" },
            sourceUrl = primary?.url,
            preampGainDb = revision.preampGainDb,
            bands = revision.filters.map { filter ->
                OpraBand(
                    type = filter.type.toLegacyType(filter.sourceType),
                    frequency = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                    slope = filter.slope,
                )
            },
            eqLibrarySafetyHeadroomDb = revision.eqLibrarySafetyHeadroomDb,
            isVerified = revision.verificationStatus == VerificationStatus.VERIFIED,
            isLatestRevision = revision.isLatest,
        )
    }

    private fun generalPresetId(profile: CanonicalEqProfile, revision: EqRevision): String =
        "$GENERAL_PRODUCT_ID:${profile.canonicalProfileId}@${revision.revisionId}"

    private fun OpraEqProfile.matchesCanonicalProjection(candidate: OpraEqProfile): Boolean =
        id == candidate.id &&
            canonicalProfileId == candidate.canonicalProfileId &&
            author == candidate.author &&
            link == candidate.link &&
            profileType == candidate.profileType &&
            preampGainDb == candidate.preampGainDb &&
            bands == candidate.bands &&
            eqLibrarySafetyHeadroomDb == candidate.eqLibrarySafetyHeadroomDb &&
            isVerified == candidate.isVerified &&
            bandOrderProvenance == candidate.bandOrderProvenance

    private fun generalCategory(
        profile: CanonicalEqProfile,
        revision: EqRevision,
    ): GeneralEqCategory {
        if (profile.purpose == EqPresetPurpose.GENRE) return GeneralEqCategory.GENRE
        val searchable = listOfNotNull(
            profile.tuningLabel,
            profile.target.name,
            revision.soundImpactSummary,
        ).joinToString(" ").lowercase(Locale.ROOT)
        return if (UTILITY_TERMS.any(searchable::contains)) {
            GeneralEqCategory.UTILITY
        } else {
            GeneralEqCategory.SOUND
        }
    }

    private fun revisionProfile(
        profile: CanonicalEqProfile,
        revision: EqRevision,
        vendorId: String,
        productId: String,
    ): OpraEqProfile {
        val primaryReference = revision.sourceReferences.filter(EqSourceReference::isPrimary).singleOrNull()
        val primary = primaryReference ?: revision.sourceReferences.firstOrNull()
        val opra = revision.sourceReferences.firstOrNull {
            it.sourceId == "opra" && !it.sourceRecordId.isNullOrBlank()
        }
        val verifiedOpraPriority = revision.hasVerifiedOpraBandOrderFor(vendorId, productId)
        val idSource = primaryReference?.takeIf { it.sourceId == "opra" && !it.sourceRecordId.isNullOrBlank() }
            ?: opra
        val legacyProfileId = if (revision.isLatest && idSource != null) {
            requireNotNull(idSource.sourceRecordId)
        } else {
            "eq-library:${profile.canonicalProfileId}@${revision.revisionId}"
        }

        return OpraEqProfile(
            id = legacyProfileId,
            productId = productId,
            canonicalProfileId = profile.canonicalProfileId,
            author = profile.creator,
            details = legacyDetails(profile, revision, primary),
            link = primary?.url,
            profileType = "parametric_eq",
            preampGainDb = revision.preampGainDb,
            bands = revision.filters.map { filter ->
                OpraBand(
                    type = filter.type.toLegacyType(filter.sourceType),
                    frequency = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                    slope = filter.slope,
                )
            },
            eqLibrarySafetyHeadroomDb = revision.eqLibrarySafetyHeadroomDb,
            isVerified = revision.verificationStatus == VerificationStatus.VERIFIED,
            bandOrderProvenance = EqBandOrderProvenance.OPRA_SOURCE_PRIORITY.takeIf { verifiedOpraPriority },
        )
    }

    private fun legacyIdentity(profiles: List<CanonicalEqProfile>): LegacyIdentity {
        val opra = profiles.asSequence()
            .flatMap { it.revisions.asSequence() }
            .flatMap { it.sourceReferences.asSequence() }
            .firstOrNull {
                it.sourceId == "opra" &&
                    !it.sourceVendorId.isNullOrBlank() &&
                    !it.sourceProductId.isNullOrBlank()
            }
        if (opra != null) {
            return LegacyIdentity(
                vendorId = requireNotNull(opra.sourceVendorId),
                productId = requireNotNull(opra.sourceProductId),
            )
        }

        val headphone = requireNotNull(profiles.first().headphone)
        return LegacyIdentity(
            vendorId = "eq-library-vendor:${slug(headphone.manufacturer)}",
            productId = "eq-library-product:${headphone.normalizedKey}",
        )
    }

    private fun legacyDetails(
        profile: CanonicalEqProfile,
        revision: EqRevision,
        primary: EqSourceReference?,
    ): String? {
        val soundImpact = revision.soundImpactSummary?.takeIf(String::isNotBlank)
            ?: SoundImpactSummary.fromFilters(revision.filters)
            ?: "Makes small frequency-response adjustments."
        val measurement = measurementSource(profile.tuningLabel)
        val database = primary?.sourceDataset?.takeIf(String::isNotBlank)
            ?: measurement?.substringBefore(" / ")?.trim()?.takeIf(String::isNotBlank)
            ?: primary?.sourceId?.takeIf(String::isNotBlank)?.let(::displaySourceId)
        val target = profile.target.name?.takeIf(String::isNotBlank)
        val parts = buildList {
            add(if (revision.isLatest) "Latest" else "Previous revision")
            if (!revision.isLatest) {
                revisionDisplayDate(revision, primary)?.let { add("Revision: $it") }
            }
            database?.let { add("Database: $it") }
            measurement?.let { add("Measurement: $it") }
            target?.let { add("Target: $it") }
            primary?.sourceId?.takeIf(String::isNotBlank)?.let { add("Source: ${displaySourceId(it)}") }
            add(soundImpact)
        }
        return parts.distinct().joinToString(" · ").takeIf(String::isNotBlank)
    }

    private fun measurementSource(label: String?): String? {
        val value = label?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val match = Regex("^AutoEq \\((.+) measurement\\)$", RegexOption.IGNORE_CASE).matchEntire(value)
            ?: return null
        return match.groupValues[1].trim().takeIf(String::isNotEmpty)
    }

    private fun revisionDisplayDate(revision: EqRevision, primary: EqSourceReference?): String? {
        val epochSeconds = revision.sourceUpdatedAtEpochSeconds
            ?: primary?.updatedAtEpochSeconds
            ?: primary?.publishedAtEpochSeconds
            ?: revision.firstSeenAtEpochSeconds
            ?: primary?.discoveredAtEpochSeconds
            ?: return null
        return runCatching {
            REVISION_DATE_FORMATTER.format(Instant.ofEpochSecond(epochSeconds))
        }.getOrNull()
    }

    private fun displaySourceId(value: String): String = when (value.lowercase(Locale.ROOT)) {
        "opra" -> "OPRA"
        "autoeq" -> "AutoEQ"
        "oratory1990" -> "oratory1990"
        "mrchillstorm-headphone-target" -> "MrChillStorm"
        "fairbuds" -> "Fairbuds"
        "github-community" -> "GitHub"
        "squiglink" -> "Squiglink"
        "reddit-audio" -> "Reddit"
        "head-fi" -> "Head-Fi"
        "audio-science-review" -> "Audio Science Review"
        "headphone-community", "headphones-community" -> "The HEADPHONE Community"
        "topping-community" -> "Topping Community"
        else -> value
            .split('-', '_')
            .filter(String::isNotBlank)
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.ROOT) } }
            .ifBlank { value }
    }

    fun displayProductName(headphone: HeadphoneIdentity): String = buildList {
        add(headphone.model)
        headphone.variant?.takeIf(String::isNotBlank)?.let(::add)
        headphone.padsOrMode?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" · ")

    private fun EqFilterType.toLegacyType(sourceType: String?): String = when (this) {
        EqFilterType.PEAK -> "peak_dip"
        EqFilterType.LOW_SHELF -> "low_shelf"
        EqFilterType.HIGH_SHELF -> "high_shelf"
        EqFilterType.LOW_PASS -> "low_pass"
        EqFilterType.HIGH_PASS -> "high_pass"
        EqFilterType.OTHER -> sourceType?.trim()?.takeIf(String::isNotEmpty) ?: "other"
    }

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')

    private data class LegacyIdentity(
        val vendorId: String,
        val productId: String,
    )

    private val REVISION_DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

    private val UTILITY_TERMS = listOf(
        "loudness",
        "low volume",
        "low-volume",
        "speech",
        "podcast",
        "dialog",
        "dialogue",
        "voice",
    )

    private const val GENERAL_PRODUCT_ID = "eq-library-general"
}
