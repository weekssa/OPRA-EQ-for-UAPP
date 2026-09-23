package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraFilterTypeNormalizer
import java.util.Locale

/** Creates a source-neutral local snapshot while retaining the legacy profile only as a projection. */
object LocalSavedEqAdapter {
    fun adapt(
        profile: OpraEqProfile,
        displayName: String,
        headphone: HeadphoneIdentity?,
        target: EqTarget,
        tuningLabel: String? = null,
        sourceReference: EqSourceReference,
        verificationStatus: VerificationStatus,
        observedAtEpochSeconds: Long?,
    ): LocalSavedEqSnapshot? {
        if (profile.profileType?.trim()?.lowercase(Locale.ROOT) != "parametric_eq") return null
        if (profile.id.isBlank() || displayName.isBlank()) return null
        if (profile.preampGainDb?.isFinite() == false) return null
        if (profile.eqLibrarySafetyHeadroomDb?.isFinite() == false) return null
        val sourceBands = profile.bands?.takeIf { it.isNotEmpty() } ?: return null
        val filters = sourceBands.map { band -> adaptBand(band) ?: return null }
        val acousticFingerprint = AcousticFingerprint.of(profile.preampGainDb, filters)
        val sourcePriority = AcousticFingerprint.sourcePriority(filters)
        val revision = EqRevision(
            revisionId = "${profile.id}@${acousticFingerprint.take(16)}-order-${sourcePriority.take(16)}",
            acousticFingerprint = acousticFingerprint,
            preampGainDb = profile.preampGainDb,
            filters = filters,
            sourceReferences = listOf(sourceReference),
            soundImpactSummary = SoundImpactSummary.fromFilters(filters),
            firstSeenAtEpochSeconds = observedAtEpochSeconds,
            verificationStatus = verificationStatus,
            isLatest = true,
            eqLibrarySafetyHeadroomDb = profile.eqLibrarySafetyHeadroomDb,
        )
        return LocalSavedEqSnapshot(
            profileId = profile.id,
            displayName = displayName.trim().ifBlank { return null },
            headphone = headphone,
            creator = profile.author?.trim()?.takeIf { it.isNotEmpty() && it != "Personal" },
            target = target,
            tuningLabel = tuningLabel?.trim()?.takeIf(String::isNotEmpty),
            revision = revision,
        )
    }

    fun projectToLegacy(
        snapshot: LocalSavedEqSnapshot,
        productId: String,
        authorFallback: String = "Personal",
    ): OpraEqProfile = OpraEqProfile(
        id = snapshot.profileId,
        productId = productId,
        canonicalProfileId = snapshot.profileId,
        author = snapshot.creator ?: authorFallback,
        details = snapshot.target.name?.takeIf(String::isNotBlank)
            ?: snapshot.tuningLabel?.takeIf(String::isNotBlank)
            ?: snapshot.displayName,
        link = snapshot.revision.sourceReferences.firstOrNull { it.isPrimary }?.url
            ?: snapshot.revision.sourceReferences.firstOrNull()?.url,
        profileType = "parametric_eq",
        preampGainDb = snapshot.revision.preampGainDb,
        bands = snapshot.revision.filters.map(::projectBand),
        eqLibrarySafetyHeadroomDb = snapshot.revision.eqLibrarySafetyHeadroomDb,
        isVerified = snapshot.revision.verificationStatus == VerificationStatus.VERIFIED,
    )

    private fun adaptBand(band: OpraBand): EqFilter? {
        val sourceType = band.type?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val normalized = OpraFilterTypeNormalizer.normalize(sourceType) ?: return null
        val frequency = band.frequency?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        if (band.gainDb?.isFinite() == false || band.q?.isFinite() == false || band.slope?.isFinite() == false) {
            return null
        }
        if (band.q?.let { it <= 0.0 } == true || band.slope?.let { it <= 0.0 } == true) return null
        val type = when (normalized) {
            "peak_dip" -> EqFilterType.PEAK
            "low_shelf" -> EqFilterType.LOW_SHELF
            "high_shelf" -> EqFilterType.HIGH_SHELF
            "low_pass" -> EqFilterType.LOW_PASS
            "high_pass" -> EqFilterType.HIGH_PASS
            else -> EqFilterType.OTHER
        }
        return EqFilter(
            type = type,
            frequencyHz = frequency,
            gainDb = band.gainDb,
            q = band.q,
            slope = band.slope,
            sourceType = sourceType.takeIf { type == EqFilterType.OTHER },
        )
    }

    private fun projectBand(filter: EqFilter): OpraBand = OpraBand(
        type = when (filter.type) {
            EqFilterType.PEAK -> "peak_dip"
            EqFilterType.LOW_SHELF -> "low_shelf"
            EqFilterType.HIGH_SHELF -> "high_shelf"
            EqFilterType.LOW_PASS -> "low_pass"
            EqFilterType.HIGH_PASS -> "high_pass"
            EqFilterType.OTHER -> filter.sourceType ?: "other"
        },
        frequency = filter.frequencyHz,
        gainDb = filter.gainDb,
        q = filter.q,
        slope = filter.slope,
    )
}
