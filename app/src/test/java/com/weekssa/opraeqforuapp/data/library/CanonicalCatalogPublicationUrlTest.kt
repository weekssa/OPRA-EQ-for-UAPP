package com.weekssa.opraeqforuapp.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CanonicalCatalogPublicationUrlTest {
    @Test
    fun runtimeCatalogUsesStableLivePublicationBranch() {
        assertEquals(
            "https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/catalog-live/catalog/catalog.json",
            HttpCanonicalCatalogSource.DEFAULT_CATALOG_URL,
        )
        assertFalse(HttpCanonicalCatalogSource.DEFAULT_CATALOG_URL.contains("v0.3"))
        assertFalse(HttpCanonicalCatalogSource.DEFAULT_CATALOG_URL.contains("eq-library-community"))
    }
}
