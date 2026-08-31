package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetExportPlanTest {
    @Test
    fun selectedGeneratedProfilesBecomeManufacturerModelCandidates() {
        val headphone = headphone(
            vendor = "Sennheiser",
            model = "HD 650",
            profiles = listOf(profile("p1", selected = true, presetName = "HD 650 - Creator - Target")),
        )

        val plan = buildPresetExportPlan(listOf(headphone))

        assertEquals(1, plan.candidates.size)
        assertEquals("Sennheiser/HD 650", plan.candidates.single().relativeDirectory)
        assertEquals("HD 650 - Creator - Target.xml", plan.candidates.single().fileName)
        assertTrue(plan.duplicateConflicts.isEmpty())
    }

    @Test
    fun unselectedOrUngeneratedProfilesAreNeverExportCandidates() {
        val headphone = headphone(
            profiles = listOf(
                profile("unselected", selected = false, presetName = "Unselected"),
                profile("missing-xml", selected = true, presetName = null),
            ),
        )

        assertTrue(buildPresetExportPlan(listOf(headphone)).candidates.isEmpty())
    }

    @Test
    fun deterministicNameCollisionIsConflictRatherThanRenamed() {
        val headphone = headphone(
            profiles = listOf(
                profile("p1", selected = true, presetName = "Same Name"),
                profile("p2", selected = true, presetName = "Same Name"),
            ),
        )

        val plan = buildPresetExportPlan(listOf(headphone))

        assertTrue(plan.candidates.isEmpty())
        assertEquals(setOf("p1", "p2"), plan.duplicateConflicts.map { it.profileId }.toSet())
        assertTrue(plan.duplicateConflicts.all { it.fileName == "Same Name.xml" })
    }

    @Test
    fun eqLibraryPlanCanTargetExactlyOneDeviceAndCarriesFidelity() {
        val headphone = headphone(
            vendor = "Sennheiser",
            model = "HD 650",
            profiles = listOf(profile("p1", selected = true, presetName = "HD 650 - Creator - Target")),
        )

        val uapp = buildEqLibraryExportPlan(listOf(headphone), ExportDevice.UAPP)
        assertEquals(1, uapp.candidates.size)
        assertTrue(uapp.candidates.all { it.deviceName == ExportDevice.UAPP.folderName })
        assertTrue(uapp.candidates.all { it.relativeDirectory.startsWith("${ExportDevice.UAPP.folderName}/") })
        assertTrue(uapp.candidates.all { it.fidelity == DevicePresetFidelity.EXACT })

        val blackPearl = buildEqLibraryExportPlan(listOf(headphone), ExportDevice.BLACK_PEARL)
        assertEquals(1, blackPearl.candidates.size)
        assertTrue(blackPearl.candidates.all { it.deviceName == ExportDevice.BLACK_PEARL.folderName })
        assertTrue(blackPearl.candidates.all { it.relativeDirectory.startsWith("${ExportDevice.BLACK_PEARL.folderName}/") })
        assertTrue(blackPearl.candidates.all { it.fidelity == DevicePresetFidelity.EXACT })
    }

    @Test
    fun uappBandReductionIsMarkedOptimizedInExportPlan() {
        val sourceBands = (1..11).map { index ->
            OpraBand("peak_dip", 100.0 * index, index / 10.0, 1.0, null)
        }
        val source = profile("p1", selected = true, presetName = "Eleven band target").copy(
            lastKnownProfile = OpraEqProfile(
                id = "p1",
                productId = "product",
                author = "Creator",
                details = "Eleven band target",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = -3.0,
                bands = sourceBands,
            ),
        )

        val plan = buildEqLibraryExportPlan(
            listOf(headphone(profiles = listOf(source))),
            ExportDevice.UAPP,
        )

        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.candidates.single().fidelity)
        assertTrue(plan.candidates.single().transformation.contains("EQ Library optimized conversion"))
        assertEquals(11, source.lastKnownProfile.bands!!.size)
        assertEquals(sourceBands, source.lastKnownProfile.bands)
    }

    @Test
    fun blackPearlNativeShelfIsExactWhenPreampIsZero() {
        val source = profile("p1", selected = true, presetName = "Shelf").copy(
            lastKnownProfile = OpraEqProfile(
                id = "p1",
                productId = "product",
                author = "Creator",
                details = "Shelf target",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = 0.0,
                bands = listOf(OpraBand("low_shelf", 105.0, 4.0, 0.71, null)),
            ),
        )
        val plan = buildEqLibraryExportPlan(
            listOf(headphone(profiles = listOf(source))),
            ExportDevice.BLACK_PEARL,
        )

        assertEquals(1, plan.candidates.size)
        assertEquals(DevicePresetFidelity.EXACT, plan.candidates.single().fidelity)
        assertTrue(plan.candidates.single().transformation.contains("preserved"))
    }

    @Test
    fun blackPearlFileExportPreservesNonzeroSourcePreamp() {
        val source = profile("p1", selected = true, presetName = "Needs preamp").copy(
            lastKnownProfile = OpraEqProfile(
                id = "p1",
                productId = "product",
                author = "Creator",
                details = "Needs preamp",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = -3.0,
                bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
            ),
        )

        val plan = buildEqLibraryExportPlan(
            listOf(headphone(profiles = listOf(source))),
            ExportDevice.BLACK_PEARL,
        )

        assertEquals(1, plan.candidates.size)
        assertEquals(DevicePresetFidelity.EXACT, plan.candidates.single().fidelity)
        assertTrue(plan.candidates.single().xml.contains("Preamp: -3.00 dB"))
    }

    @Test
    fun blackPearlFileExportPreservesGeneratedSafetyHeadroomAsOptimizedMetadata() {
        val source = profile("p1", selected = true, presetName = "Needs headroom").copy(
            lastKnownProfile = OpraEqProfile(
                id = "p1",
                productId = "product",
                author = "Creator",
                details = "Needs headroom",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = null,
                bands = listOf(OpraBand("peak_dip", 1_000.0, 4.6, 1.0, null)),
                eqLibrarySafetyHeadroomDb = -4.6,
            ),
        )

        val plan = buildEqLibraryExportPlan(
            listOf(headphone(profiles = listOf(source))),
            ExportDevice.BLACK_PEARL,
        )

        assertEquals(1, plan.candidates.size)
        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.candidates.single().fidelity)
        assertTrue(plan.candidates.single().xml.contains("Preamp: -4.60 dB"))
    }

    @Test
    fun historicalCanonicalRevisionCanBeExportedWithoutSelectingLatest() {
        val historicalId = "eq-library:autoeq-edition-xs@rev-2023"
        val headphone = headphone(
            vendor = "HIFIMAN",
            model = "Edition XS",
            profiles = listOf(
                profile(historicalId, selected = true, presetName = "Edition XS - AutoEq - Previous revision"),
                profile("eq-library:autoeq-edition-xs@rev-latest", selected = false, presetName = "Edition XS - AutoEq - Latest"),
            ),
        )

        val plan = buildEqLibraryExportPlan(listOf(headphone), ExportDevice.UAPP)

        assertEquals(1, plan.candidates.size)
        assertEquals(historicalId, plan.candidates.single().profileId)
        assertEquals(ExportDevice.UAPP.folderName, plan.candidates.single().deviceName)
        assertTrue(
            plan.candidates.single().relativeDirectory
                .startsWith("${ExportDevice.UAPP.folderName}/HIFIMAN/Edition XS"),
        )
    }

    @Test
    fun folderSanitizationPreservesUnicodeAndOnlyRemovesPathSeparators() {
        assertEquals("A-B 測定", safeSharedPathSegment(" A/B 測定 "))
        assertEquals("Model-Variant", safeSharedPathSegment("Model\\Variant"))
    }

    private fun headphone(
        vendor: String = "Vendor",
        model: String = "Model",
        profiles: List<ManagedProfileRecord>,
    ) = ManagedHeadphoneRecord(
        productId = "product",
        vendorId = "vendor",
        vendorName = vendor,
        productName = model,
        autoIncludeNewProfiles = true,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        profiles = profiles,
    )

    private fun profile(
        id: String,
        selected: Boolean,
        presetName: String?,
    ): ManagedProfileRecord {
        val generated = presetName != null
        return ManagedProfileRecord(
            profileId = id,
            selected = selected,
            explicitlyExcluded = false,
            lastKnownProfile = OpraEqProfile(
                id = id,
                productId = "product",
                author = "Creator",
                details = "Target",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = 0.0,
                bands = listOf(OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null)),
            ),
            fingerprint = "fingerprint-$id",
            firstSeenAtMillis = 1L,
            lastSeenAtMillis = 1L,
            isNewUnreviewed = false,
            isUpdatedUnreviewed = false,
            noLongerAvailable = false,
            generatedPresetName = presetName,
            generatedXml = if (generated) "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<Preset/>\n" else null,
            generatedFromFingerprint = if (generated) "fingerprint-$id" else null,
            generatedAtMillis = if (generated) 1L else null,
        )
    }
}
