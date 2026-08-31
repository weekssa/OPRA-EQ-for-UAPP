package com.weekssa.opraeqforuapp.data.catalog

import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpraCatalogParserTest {
    private val parser = OpraCatalogParser()

    @Test
    fun parsesVendorProductAndEqRelationships() {
        val catalog = parser.parse(sampleCatalog().reader())

        assertEquals("Sennheiser", catalog.vendors.single().name)
        assertEquals("HD 600", catalog.products.single().name)
        assertEquals(1, catalog.profileCount("sennheiser_hd600"))
        assertEquals("oratory1990", catalog.profiles.single().author)
    }

    @Test
    fun rejectsBrokenJsonInsteadOfReplacingKnownGoodData() {
        val broken = sampleCatalog() + "\n{not json}"

        assertThrows(CatalogParseException::class.java) {
            parser.parse(broken.reader())
        }
    }

    @Test
    fun rejectsOrphanProductRelationship() {
        val orphan = """
            {"type":"vendor","id":"sennheiser","data":{"name":"Sennheiser"}}
            {"type":"product","id":"hd600","data":{"vendor_id":"missing","name":"HD 600","type":"headphones","subtype":"over_the_ear"}}
            {"type":"eq","id":"eq1","data":{"product_id":"hd600","author":"A","type":"parametric_eq","parameters":{"gain_db":0.0,"bands":[]}}}
        """.trimIndent()

        assertThrows(CatalogParseException::class.java) {
            parser.parse(orphan.reader())
        }
    }

    @Test
    fun outputUnsupportedFilterRemainsSelectableCanonicalSource() {
        val catalog = parser.parse(sampleCatalog(filterType = "band_stop").reader())
        val profile = catalog.profiles.single()

        assertEquals(
            ProfileCompatibility.FullyCompatible,
            profile.assessCompatibility().category,
        )
        assertEquals(
            ProfileCompatibility.NotCompatible,
            profile.assessUappCompatibility().category,
        )
    }

    @Test
    fun moreThanTenPriorityBandsRemainCanonicalWhileUappReportsLimitation() {
        val bands = (1..11).joinToString(",") { index ->
            "{\"type\":\"peak_dip\",\"frequency\":${100 + index},\"gain_db\":0.0,\"q\":1.0}"
        }
        val catalog = parser.parse(sampleCatalog(rawBands = bands).reader())
        val profile = catalog.profiles.single()

        assertEquals(
            ProfileCompatibility.FullyCompatible,
            profile.assessCompatibility().category,
        )
        assertEquals(
            ProfileCompatibility.CompatibleWithLimitation,
            profile.assessUappCompatibility().category,
        )
    }

    @Test
    fun missingBandGainUsesOpraDocumentedZeroDefault() {
        val rawBands = "{\"type\":\"peak_dip\",\"frequency\":1000.0,\"q\":1.0}"
        val catalog = parser.parse(sampleCatalog(rawBands = rawBands).reader())

        assertEquals(
            ProfileCompatibility.FullyCompatible,
            catalog.profiles.single().assessCompatibility().category,
        )
    }

    private fun sampleCatalog(
        filterType: String = "peak_dip",
        rawBands: String = "{\"type\":\"$filterType\",\"frequency\":1000.0,\"gain_db\":-2.0,\"q\":1.0}",
    ): String = """
        {"type":"vendor","id":"sennheiser","data":{"name":"Sennheiser"}}
        {"type":"product","id":"sennheiser_hd600","data":{"vendor_id":"sennheiser","name":"HD 600","type":"headphones","subtype":"over_the_ear"}}
        {"type":"eq","id":"sennheiser_hd600_oratory","data":{"product_id":"sennheiser_hd600","author":"oratory1990","details":"Harman Target","type":"parametric_eq","parameters":{"gain_db":-5.5,"bands":[$rawBands]}}}
    """.trimIndent()

    private fun String.reader(): BufferedReader = BufferedReader(StringReader(this))
}
