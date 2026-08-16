package com.weekssa.opraeqforuapp.data.catalog

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        var calls = 0
        val repository = OpraCatalogRepository(
            filesDir = filesDir,
            source = OpraCatalogSource {
                calls += 1
                throw IOException("offline")
            },
        )

        val result = repository.refresh()

        assertEquals(1, calls)
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
    fun initializeRetriesTransientFirstDownloadFailureBeforeShowingUnavailable() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        var calls = 0
        val repository = OpraCatalogRepository(
            filesDir = filesDir,
            source = OpraCatalogSource { destination ->
                calls += 1
                if (calls == 1) {
                    throw IOException("temporary network startup failure")
                }
                destination.writeText(validCatalog())
            },
            startupRetryDelayMillis = 0L,
        )

        repository.initialize()

        assertEquals(2, calls)
        val ready = repository.state.value as CatalogState.Ready
        assertEquals("HD 600", ready.catalog.products.single().name)
    }

    @Test
    fun initializeReportsUnavailableAfterBothStartupAttemptsFail() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        var calls = 0
        val repository = OpraCatalogRepository(
            filesDir = filesDir,
            source = OpraCatalogSource {
                calls += 1
                throw IOException("offline")
            },
            startupRetryDelayMillis = 0L,
        )

        repository.initialize()

        assertEquals(2, calls)
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

    @Test
    fun concurrentRepositoryInstancesSerializeSharedCatalogRefresh() = runBlocking {
        val filesDir = Files.createTempDirectory("opra-catalog-test").toFile()
        val activeDownloads = AtomicInteger(0)
        val maxConcurrentDownloads = AtomicInteger(0)
        val source = OpraCatalogSource { destination ->
            val active = activeDownloads.incrementAndGet()
            maxConcurrentDownloads.updateAndGet { previous -> maxOf(previous, active) }
            try {
                delay(50)
                destination.writeText(validCatalog())
            } finally {
                activeDownloads.decrementAndGet()
            }
        }
        val firstRepository = OpraCatalogRepository(filesDir = filesDir, source = source)
        val secondRepository = OpraCatalogRepository(filesDir = filesDir, source = source)

        val results = coroutineScope {
            listOf(
                async { firstRepository.refresh() },
                async { secondRepository.refresh() },
            ).awaitAll()
        }

        assertTrue(results.all { it is CatalogRefreshResult.Success })
        assertEquals(1, maxConcurrentDownloads.get())
        assertTrue(firstRepository.state.value is CatalogState.Ready)
        assertTrue(secondRepository.state.value is CatalogState.Ready)
    }

    private fun validCatalog(): String = """
        {"type":"vendor","id":"sennheiser","data":{"name":"Sennheiser"}}
        {"type":"product","id":"sennheiser_hd600","data":{"vendor_id":"sennheiser","name":"HD 600","type":"headphones","subtype":"over_the_ear"}}
        {"type":"eq","id":"eq1","data":{"product_id":"sennheiser_hd600","author":"A","type":"parametric_eq","parameters":{"gain_db":0.0,"bands":[]}}}
    """.trimIndent()
}
