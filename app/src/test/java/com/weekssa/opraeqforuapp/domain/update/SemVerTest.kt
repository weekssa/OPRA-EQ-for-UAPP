package com.weekssa.opraeqforuapp.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {
    @Test
    fun parsesDevelopmentAndStableVersionsWithOptionalVPrefix() {
        assertEquals(SemVer(0, 1, 0), SemVer.parse("0.1.0"))
        assertEquals(SemVer(1, 0, 0), SemVer.parse("v1.0.0"))
    }

    @Test
    fun parsesBetaPrereleaseAndBuildMetadata() {
        assertEquals(
            SemVer(0, 8, 0, prereleaseIdentifiers = listOf("beta")),
            SemVer.parse("0.8.0-beta"),
        )
        assertEquals(
            SemVer(
                1,
                2,
                3,
                prereleaseIdentifiers = listOf("rc", "2"),
                buildMetadata = "build.45",
            ),
            SemVer.parse("v1.2.3-rc.2+build.45"),
        )
    }

    @Test
    fun rejectsMalformedSemverAndNumericPrereleaseLeadingZero() {
        assertNull(SemVer.parse("1.0"))
        assertNull(SemVer.parse("01.0.0"))
        assertNull(SemVer.parse("1.0.0-beta.01"))
        assertNull(SemVer.parse("1.0.0-"))
    }

    @Test
    fun comparesMajorMinorPatchInOrder() {
        assertTrue(SemVer(0, 2, 0) > SemVer(0, 1, 9))
        assertTrue(SemVer(1, 0, 0) > SemVer(0, 99, 99))
        assertTrue(SemVer(1, 0, 1) > SemVer(1, 0, 0))
    }

    @Test
    fun comparesPrereleasesBeforeTheirStableReleaseAndByIdentifier() {
        assertTrue(SemVer.parse("0.8.0-beta")!! > SemVer.parse("0.7.2")!!)
        assertTrue(SemVer.parse("0.8.0")!! > SemVer.parse("0.8.0-beta")!!)
        assertTrue(SemVer.parse("1.0.0-beta.2")!! > SemVer.parse("1.0.0-beta.1")!!)
        assertTrue(SemVer.parse("1.0.0-beta.10")!! > SemVer.parse("1.0.0-beta.2")!!)
        assertTrue(SemVer.parse("1.0.0-beta.alpha")!! > SemVer.parse("1.0.0-beta.10")!!)
    }

    @Test
    fun buildMetadataDoesNotAffectPrecedence() {
        assertEquals(
            0,
            SemVer.parse("1.2.3+build.1")!!.compareTo(SemVer.parse("1.2.3+build.2")!!),
        )
    }
}
