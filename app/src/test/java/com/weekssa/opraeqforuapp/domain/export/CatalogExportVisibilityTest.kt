package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogExportVisibilityTest {
    @Test
    fun defaultBroaderLibraryVisibilityReturnsOriginalCatalog() {
        val catalog = sampleCatalog()

        val result = catalog.forExportTargetVisibility(ExportTargetPreferences())

        assertSame(catalog, result.catalog)
        assertEquals(0, result.hiddenProfileCount)
        assertEquals(2, result.catalog.profiles.size)
    }

    @Test
    fun hidingUnexportableProfilesRemovesOnlyUnsupportedProfilesAndEmptyProducts() {
        val catalog = sampleCatalog()
        val preferences = ExportTargetPreferences(
            selectedTargets = setOf(ExportDevice.UAPP),
            showUnexportablePresets = false,
        )

        val result = catalog.forExportTargetVisibility(preferences)

        assertEquals(listOf("exportable"), result.catalog.profiles.map(OpraEqProfile::id))
        assertEquals(listOf("supported-product"), result.catalog.products.map(OpraProduct::id))
        assertEquals(listOf("vendor"), result.catalog.vendors.map(OpraVendor::id))
        assertEquals(1, result.hiddenProfileCount)
        assertEquals(2, catalog.profiles.size)
    }

    @Test
    fun optimizedProfileRemainsVisibleForSelectedTarget() {
        val optimized = exportableProfile("optimized", "supported-product").copy(
            bands = (1..14).map { index ->
                OpraBand("peak_dip", 100.0 + index * 100.0, 1.0, 1.0, null)
            },
        )
        val catalog = sampleCatalog().copy(profiles = listOf(optimized))
        val preferences = ExportTargetPreferences(
            selectedTargets = setOf(ExportDevice.UAPP),
            showUnexportablePresets = false,
        )

        val result = catalog.forExportTargetVisibility(preferences)

        assertEquals(listOf("optimized"), result.catalog.profiles.map(OpraEqProfile::id))
        assertEquals(0, result.hiddenProfileCount)
        assertEquals(DeviceExportability.OPTIMIZED, assessDeviceExportability(optimized, ExportDevice.UAPP))
    }

    @Test
    fun noSelectedTargetsAndFilteringEnabledProducesEmptyBrowseViewWithoutMutatingSource() {
        val catalog = sampleCatalog()
        val preferences = ExportTargetPreferences(
            selectedTargets = emptySet(),
            showUnexportablePresets = false,
        )

        val result = catalog.forExportTargetVisibility(preferences)

        assertTrue(result.catalog.vendors.isEmpty())
        assertTrue(result.catalog.products.isEmpty())
        assertTrue(result.catalog.profiles.isEmpty())
        assertEquals(2, result.hiddenProfileCount)
        assertEquals(2, catalog.products.size)
        assertEquals(2, catalog.profiles.size)
    }

    private fun sampleCatalog() = OpraCatalog(
        vendors = listOf(OpraVendor("vendor", "Vendor")),
        products = listOf(
            OpraProduct("supported-product", "vendor", "Supported", "headphones", ""),
            OpraProduct("unsupported-product", "vendor", "Unsupported", "headphones", ""),
        ),
        profiles = listOf(
            exportableProfile("exportable", "supported-product"),
            exportableProfile("unsupported", "unsupported-product").copy(
                bands = listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, 12.0)),
            ),
        ),
    )

    private fun exportableProfile(id: String, productId: String) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Creator",
        details = "Test",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -2.0,
        bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
    )
}
