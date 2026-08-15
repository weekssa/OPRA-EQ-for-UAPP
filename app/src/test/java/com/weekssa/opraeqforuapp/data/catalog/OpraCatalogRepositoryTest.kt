package com.weekssa.opraeqforuapp.data.catalog

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpraCatalogRepositoryTest {
    @Test
    fun invalidRefreshKeepsPreviouslyPromotedCatalog() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        var body = validCatalog()
        val source = OpraCatalogSource { destination -> destination.writeText(body) }
        val repository = OpraCatalogRepository(filesDir = filesDir, source = source)

        val first = repository.refresh()
        assertTrue(first is CatalogRefreshResult.Success)
        val firstReady = repository.state.value as CatalogState.Ready
        assertEquals("HD 600", firstReady.catalog.products.single().name)

        body = "{broken json}"
        val second = repository.refresh()

        assertTrue(second is CatalogRefreshResult.Failure)
        second as CatalogRefreshResult.Failure
        assertEquals(CatalogRefreshFailureReason.InvalidCatalog, second.reason)
        assertTrue(second.usingSavedCatalog)
        val preserved = repository.state.value as CatalogState.Ready
        assertEquals("HD 600", preserved.catalog.products.single().name)
        assertFalse(preserved.isRefreshing)
    }

    @Test
    fun networkFailureWithoutCacheLeavesCatalogUnavailable() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        val repository = OpraCatalogRepository(
            filesDir = filesDir,
            source = OpraCatalogSource { throw IOException("offline") },
        )

        val result = repository.refresh()

        assertTrue(result is CatalogRefreshResult.Failure)
        result as CatalogRefreshResult.Failure
        assertEquals(CatalogRefreshFailureReason.Network, result.reason)
        assertFalse(result.usingSavedCatalog)
        assertEquals(
            CatalogState.Unavailable(CatalogRefreshFailureReason.Network),
            repository.state.value,
        )
    }

    @Test
    fun initializeUsesFreshSavedCatalogWithoutRedownloading() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        var calls = 0
        val source = OpraCatalogSource { destination ->
            calls += 1
            destination.writeText(validCatalog())
        }
        val firstRepository = OpraCatalogRepository(filesDir = filesDir, source = source)
        firstRepository.refresh()
        assertEquals(1, calls)

        val secondRepository = OpraCatalogRepository(filesDir = filesDir, source = source)
        secondRepository.initialize()

        assertEquals(1, calls)
        assertTrue(secondRepository.state.value is CatalogState.Ready)
    }

    private fun validCatalog(): String = """
        {"type":"vendor","id":"sennheiser","data":{"name":"Sennheiser"}}
        {"type":"product","id":"sennheiser_hd600","data":{"vendor_id":"sennheiser","name":"HD 600","type":"headphones","subtype":"over_the_ear"}}
        {"type":"eq","id":"eq1","data":{"product_id":"sennheiser_hd600","author":"A","type":"parametric_eq","parameters":{"gain_db":0.0,"bands":[]}}}
    """.trimIndent()
}
