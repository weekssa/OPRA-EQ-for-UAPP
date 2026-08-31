package com.weekssa.opraeqforuapp.domain.catalog

import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCompatibilityEvaluatorTest {
    @Test
    fun sourceUsabilityDoesNotDependOnOutputPreampSupport() {
        val profile = profile(
            sourcePreamp = null,
            safetyHeadroom = null,
        )

        val assessment = profile.assessCompatibility()

        assertEquals(ProfileCompatibility.FullyCompatible, assessment.category)
        assertTrue(profile.isUsableParametricSource())
    }

    @Test
    fun uappCompatibilityCanUseGeneratedSafetyHeadroomWithoutReplacingSourcePreamp() {
        val profile = profile(
            sourcePreamp = null,
            safetyHeadroom = -4.5,
        )

        val assessment = profile.assessUappCompatibility()

        assertEquals(ProfileCompatibility.FullyCompatible, assessment.category)
        assertEquals(null, profile.preampGainDb)
        assertEquals(-4.5, profile.eqLibrarySafetyHeadroomDb!!, 0.0)
        assertEquals(-4.5, profile.effectivePlaybackPreampDb()!!, 0.0)
        assertTrue(profile.usesEqLibrarySafetyHeadroom())
    }

    @Test
    fun missingPreampIsUappSpecificNotAReasonToHideSourceCurve() {
        val profile = profile(
            sourcePreamp = null,
            safetyHeadroom = null,
        )

        assertEquals(ProfileCompatibility.FullyCompatible, profile.assessCompatibility().category)

        val uapp = profile.assessUappCompatibility()
        assertEquals(ProfileCompatibility.NotCompatible, uapp.category)
        assertTrue(uapp.reason.orEmpty().contains("no source preamp"))
        assertTrue(uapp.reason.orEmpty().contains("no EQ Library-generated safety headroom"))
    }

    @Test
    fun outputUnsupportedFilterCanRemainAUsableCanonicalSource() {
        val profile = profile(
            sourcePreamp = -3.0,
            safetyHeadroom = null,
        ).copy(
            bands = listOf(OpraBand("low_pass", 8_000.0, 0.0, 0.7, 12.0)),
        )

        assertTrue(profile.isUsableParametricSource())
        assertEquals(ProfileCompatibility.FullyCompatible, profile.assessCompatibility().category)
        assertEquals(ProfileCompatibility.NotCompatible, profile.assessUappCompatibility().category)
    }

    @Test
    fun nonParametricRowRemainsUnusableForSelection() {
        val profile = profile(
            sourcePreamp = -3.0,
            safetyHeadroom = null,
        ).copy(profileType = "graphic_eq")

        assertTrue(!profile.isUsableParametricSource())
        assertEquals(ProfileCompatibility.NotCompatible, profile.assessCompatibility().category)
    }

    @Test
    fun sourcePreampAlwaysWinsOverGeneratedMetadata() {
        val profile = profile(
            sourcePreamp = -3.25,
            safetyHeadroom = -7.0,
        )

        assertEquals(-3.25, profile.effectivePlaybackPreampDb()!!, 0.0)
        assertTrue(!profile.usesEqLibrarySafetyHeadroom())
    }

    private fun profile(
        sourcePreamp: Double?,
        safetyHeadroom: Double?,
    ) = OpraEqProfile(
        id = "profile",
        productId = "product",
        author = "Creator",
        details = "Target",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = sourcePreamp,
        bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        eqLibrarySafetyHeadroomDb = safetyHeadroom,
    )
}
