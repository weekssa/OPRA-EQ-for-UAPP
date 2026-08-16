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
    fun rejectsNonSemverAndPrereleaseForNormalReleaseChannel() {
        assertNull(SemVer.parse("1.0"))
        assertNull(SemVer.parse("v1.0.0-beta.1"))
        assertNull(SemVer.parse("01.0.0"))
    }

    @Test
    fun comparesMajorMinorPatchInOrder() {
        assertTrue(SemVer(0, 2, 0) > SemVer(0, 1, 9))
        assertTrue(SemVer(1, 0, 0) > SemVer(0, 99, 99))
        assertTrue(SemVer(1, 0, 1) > SemVer(1, 0, 0))
    }
}
