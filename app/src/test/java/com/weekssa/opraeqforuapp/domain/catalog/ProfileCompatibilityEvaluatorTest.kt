package com.weekssa.opraeqforuapp.domain.catalog

import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCompatibilityEvaluatorTest {
    @Test
    fun generatedSafetyHeadroomAllowsTargetCompatibilityWithoutReplacingSourcePreamp() {
        val profile = profile(
            sourcePreamp = null,
            safetyHeadroom = -4.5,
        )

        val assessment = profile.assessCompatibility()

        assertEquals(ProfileCompatibility.FullyCompatible, assessment.category)
        assertEquals(null, profile.preampGainDb)
        assertEquals(-4.5, profile.eqLibrarySafetyHeadroomDb!!, 0.0)
        assertEquals(-4.5, profile.effectivePlaybackPreampDb()!!, 0.0)
        assertTrue(profile.usesEqLibrarySafetyHeadroom())
    }

    @Test
    fun missingSourcePreampAndMissingSafetyHeadroomRemainNotCompatible() {
        val assessment = profile(
            sourcePreamp = null,
            safetyHeadroom = null,
        ).assessCompatibility()

        assertEquals(ProfileCompatibility.NotCompatible, assessment.category)
        assertTrue(assessment.reason.orEmpty().contains("no source preamp"))
        assertTrue(assessment.reason.orEmpty().contains("no EQ Library-generated safety headroom"))
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
