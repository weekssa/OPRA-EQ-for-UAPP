package com.weekssa.opraeqforuapp.domain.library

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
 * Compatibility bridge for the v0.2 managed-headphone/export engine.
 *
 * Canonical revisions are projected as selectable legacy profiles so the stable v0.2 selection
 * and export engine can support v0.3 revision-aware behavior without a second export path.
 * The latest OPRA-backed revision retains its original OPRA profile ID to preserve existing v0.2
 * managed state. Historical revisions always receive stable synthetic IDs and can therefore be
 * selected/exported explicitly without silently moving a saved selection to a newer revision.
 */
object CanonicalLegacyCatalogAdapter {
    fun adapt(snapshot: CatalogSnapshot): OpraCatalog {
        val vendors = linkedMapOf<String, OpraVendor>()
        val products = mutableListOf<OpraProduct>()
        val profiles = mutableListOf<OpraEqProfile>()

        snapshot.profiles
            .groupBy { it.headphone.normalizedKey }
            .toSortedMap()
            .values
            .forEach { canonicalProfiles ->
                val representative = canonicalProfiles.first()
                val identity = legacyIdentity(canonicalProfiles)
                vendors.putIfAbsent(
                    identity.vendorId,
                    OpraVendor(id = identity.vendorId, name = representative.headphone.manufacturer),
                )
                products += OpraProduct(
                    id = identity.productId,
                    vendorId = identity.vendorId,
                    name = displayProductName(representative.headphone),
                    type = "headphones",
                    subtype = "",
                    aliases = canonicalProfiles
                        .flatMap { it.headphone.modelAliases }
                        .distinct(),
                )
                canonicalProfiles.forEach { canonical ->
                    profiles += revisionProfiles(canonical, identity.productId)
                }
            }

        return OpraCatalog(
            vendors = vendors.values.toList(),
            products = products.distinctBy(OpraProduct::id),
            profiles = profiles.distinctBy(OpraEqProfile::id),
        )
    }

    private fun revisionProfiles(profile: CanonicalEqProfile, productId: String): List<OpraEqProfile> =
        profile.revisions
            .sortedWith(
                compareByDescending<EqRevision> { it.isLatest }
                    .thenByDescending {
                        it.sourceUpdatedAtEpochSeconds ?: it.firstSeenAtEpochSeconds ?: Long.MIN_VALUE
                    },
            )
            .map { revision -> revisionProfile(profile, revision, productId) }

    private fun revisionProfile(
        profile: CanonicalEqProfile,
        revision: EqRevision,
        productId: String,
    ): OpraEqProfile {
        val primary = revision.sourceReferences.firstOrNull { it.isPrimary }
            ?: revision.sourceReferences.firstOrNull()
        val opra = revision.sourceReferences.firstOrNull {
            it.sourceId == "opra" && !it.sourceRecordId.isNullOrBlank()
        }
        val legacyProfileId = if (revision.isLatest && opra != null) {
            requireNotNull(opra.sourceRecordId)
        } else {
            "eq-library:${profile.canonicalProfileId}@${revision.revisionId}"
        }

        return OpraEqProfile(
            id = legacyProfileId,
            productId = productId,
            author = profile.creator,
            details = legacyDetails(profile, revision, primary),
            link = primary?.url,
            profileType = "parametric_eq",
            preampGainDb = revision.preampGainDb,
            bands = revision.filters.map { filter ->
                OpraBand(
                    type = filter.type.toLegacyType(),
                    frequency = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                    slope = filter.slope,
                )
            },
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

        val headphone = profiles.first().headphone
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
        "headphone-community" -> "The HEADPHONE Community"
        "topping-community" -> "Topping Community"
        else -> value
            .split('-', '_')
            .filter(String::isNotBlank)
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.ROOT) } }
            .ifBlank { value }
    }

    private fun displayProductName(headphone: HeadphoneIdentity): String = buildList {
        add(headphone.model)
        headphone.variant?.takeIf(String::isNotBlank)?.let(::add)
        headphone.padsOrMode?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" · ")

    /**
     * Keep the v0.2 engine's internal OPRA filter names here. Device-specific PK/LS/HS formatting
     * happens later in the exporter; using device abbreviations at this bridge would make an
     * otherwise compatible canonical profile fail v0.2 compatibility checks and saved-selection
     * migration.
     */
    private fun EqFilterType.toLegacyType(): String = when (this) {
        EqFilterType.PEAK -> "peak_dip"
        EqFilterType.LOW_SHELF -> "low_shelf"
        EqFilterType.HIGH_SHELF -> "high_shelf"
        EqFilterType.LOW_PASS -> "low_pass"
        EqFilterType.HIGH_PASS -> "high_pass"
        EqFilterType.OTHER -> "other"
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
}
