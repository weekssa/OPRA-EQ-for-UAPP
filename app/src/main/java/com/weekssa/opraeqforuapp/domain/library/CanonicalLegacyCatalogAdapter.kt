package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import java.util.Locale

/**
 * Compatibility bridge for the v0.2 managed-headphone/export engine.
 *
 * Only latest canonical revisions are projected here. Historical revisions remain in the
 * canonical model and will be surfaced by revision-aware UI instead of being implicitly
 * selected by legacy auto-include behavior.
 *
 * When a canonical headphone has OPRA provenance, the original OPRA vendor/product/profile
 * IDs are retained for the latest OPRA revision. This preserves existing v0.2 managed state
 * while allowing additional source profiles to share the same logical headphone.
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
                )
                canonicalProfiles.forEach { canonical ->
                    profiles += latestProfile(canonical, identity.productId)
                }
            }

        return OpraCatalog(
            vendors = vendors.values.toList(),
            products = products.distinctBy(OpraProduct::id),
            profiles = profiles.distinctBy(OpraEqProfile::id),
        )
    }

    private fun latestProfile(profile: CanonicalEqProfile, productId: String): OpraEqProfile {
        val revision = profile.latestRevision
        val primary = revision.sourceReferences.firstOrNull { it.isPrimary }
            ?: revision.sourceReferences.firstOrNull()
        val opra = revision.sourceReferences.firstOrNull {
            it.sourceId == "opra" && !it.sourceRecordId.isNullOrBlank()
        }
        val legacyProfileId = opra?.sourceRecordId
            ?: "eq-library:${profile.canonicalProfileId}@${revision.revisionId}"

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
        val parts = buildList {
            profile.tuningLabel?.takeIf(String::isNotBlank)?.let(::add)
            profile.target.name?.takeIf(String::isNotBlank)?.let { add("Target: $it") }
            primary?.sourceId?.takeIf(String::isNotBlank)?.let { add("Source: $it") }
            revision.soundImpactSummary?.takeIf(String::isNotBlank)?.let(::add)
        }
        return parts.distinct().joinToString(" · ").takeIf(String::isNotBlank)
    }

    private fun displayProductName(headphone: HeadphoneIdentity): String = buildList {
        add(headphone.model)
        headphone.variant?.takeIf(String::isNotBlank)?.let(::add)
        headphone.padsOrMode?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" · ")

    private fun EqFilterType.toLegacyType(): String = when (this) {
        EqFilterType.PEAK -> "PK"
        EqFilterType.LOW_SHELF -> "LS"
        EqFilterType.HIGH_SHELF -> "HS"
        EqFilterType.LOW_PASS -> "LP"
        EqFilterType.HIGH_PASS -> "HP"
        EqFilterType.OTHER -> "OTHER"
    }

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')

    private data class LegacyIdentity(
        val vendorId: String,
        val productId: String,
    )
}
