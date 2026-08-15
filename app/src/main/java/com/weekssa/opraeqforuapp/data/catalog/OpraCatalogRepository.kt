package com.weekssa.opraeqforuapp.data.catalog

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface CatalogState {
    data object Loading : CatalogState

    data class Ready(
        val catalog: OpraCatalog,
        val lastSuccessfulRefreshMillis: Long,
        val isRefreshing: Boolean = false,
    ) : CatalogState

    data class Unavailable(
        val reason: CatalogRefreshFailureReason,
    ) : CatalogState
}

enum class CatalogRefreshFailureReason {
    Network,
    InvalidCatalog,
    Storage,
}

sealed interface CatalogRefreshResult {
    data class Success(
        val catalog: OpraCatalog,
        val refreshedAtMillis: Long,
    ) : CatalogRefreshResult

    data class Failure(
        val reason: CatalogRefreshFailureReason,
        val usingSavedCatalog: Boolean,
    ) : CatalogRefreshResult
}

fun interface OpraCatalogSource {
    suspend fun downloadTo(destination: File)
}

class HttpOpraCatalogSource(
    private val userAgent: String,
    private val catalogUrl: URL = URL(DEFAULT_CATALOG_URL),
) : OpraCatalogSource {
    override suspend fun downloadTo(destination: File) = withContext(Dispatchers.IO) {
        val connection = (catalogUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/x-ndjson, application/json, text/plain, */*")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("OPRA catalog request failed with HTTP $responseCode.")
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_CATALOG_BYTES) {
                throw IOException("OPRA catalog exceeds the download safety limit.")
            }

            connection.inputStream.buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_CATALOG_BYTES) {
                            throw IOException("OPRA catalog exceeds the download safety limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_CATALOG_URL = "https://opra.roonlabs.net/database_v1.jsonl"
        private const val CONNECT_TIMEOUT_MILLIS = 20_000
        private const val READ_TIMEOUT_MILLIS = 60_000
        private const val MAX_CATALOG_BYTES = 128L * 1024L * 1024L
    }
}

class OpraCatalogRepository(
    filesDir: File,
    private val source: OpraCatalogSource,
    private val parser: OpraCatalogParser = OpraCatalogParser(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val catalogDirectory = File(filesDir, "opra/catalog")
    private val currentFile = File(catalogDirectory, "database_v1.jsonl")
    private val candidateFile = File(catalogDirectory, "database_v1.candidate.jsonl")
    private val refreshMutex = Mutex()
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)

    val state: StateFlow<CatalogState> = mutableState.asStateFlow()

    suspend fun initialize() {
        val cached = loadCurrentCatalog()
        if (cached != null) {
            mutableState.value = CatalogState.Ready(
                catalog = cached,
                lastSuccessfulRefreshMillis = currentFile.lastModified().takeIf { it > 0L } ?: nowMillis(),
            )
        }

        val ready = mutableState.value as? CatalogState.Ready
        val isFresh = ready != null &&
            nowMillis() - ready.lastSuccessfulRefreshMillis < AUTO_REFRESH_INTERVAL_MILLIS
        if (!isFresh) {
            refresh()
        }
    }

    suspend fun refresh(): CatalogRefreshResult = refreshMutex.withLock {
        val previous = mutableState.value as? CatalogState.Ready
        mutableState.value = previous?.copy(isRefreshing = true) ?: CatalogState.Loading

        val cacheDirectoryReady = withContext(Dispatchers.IO) {
            catalogDirectory.isDirectory || catalogDirectory.mkdirs()
        }
        if (!cacheDirectoryReady) {
            return@withLock fail(CatalogRefreshFailureReason.Storage, previous)
        }

        candidateFile.delete()

        try {
            try {
                source.downloadTo(candidateFile)
            } catch (_: IOException) {
                return@withLock fail(CatalogRefreshFailureReason.Network, previous)
            }

            val candidate = try {
                withContext(Dispatchers.IO) { parser.parse(candidateFile) }
            } catch (_: CatalogParseException) {
                return@withLock fail(CatalogRefreshFailureReason.InvalidCatalog, previous)
            } catch (_: IOException) {
                return@withLock fail(CatalogRefreshFailureReason.Storage, previous)
            }

            val refreshedAt = nowMillis()
            try {
                withContext(Dispatchers.IO) {
                    promoteCandidate()
                    currentFile.setLastModified(refreshedAt)
                }
            } catch (_: IOException) {
                return@withLock fail(CatalogRefreshFailureReason.Storage, previous)
            }

            mutableState.value = CatalogState.Ready(
                catalog = candidate,
                lastSuccessfulRefreshMillis = refreshedAt,
            )
            CatalogRefreshResult.Success(candidate, refreshedAt)
        } finally {
            candidateFile.delete()
        }
    }

    private suspend fun loadCurrentCatalog(): OpraCatalog? = withContext(Dispatchers.IO) {
        if (!currentFile.isFile) return@withContext null
        try {
            parser.parse(currentFile)
        } catch (_: Exception) {
            null
        }
    }

    private fun fail(
        reason: CatalogRefreshFailureReason,
        previous: CatalogState.Ready?,
    ): CatalogRefreshResult.Failure {
        if (previous != null) {
            mutableState.value = previous.copy(isRefreshing = false)
        } else {
            mutableState.value = CatalogState.Unavailable(reason)
        }
        return CatalogRefreshResult.Failure(
            reason = reason,
            usingSavedCatalog = previous != null,
        )
    }

    @Throws(IOException::class)
    private fun promoteCandidate() {
        try {
            Files.move(
                candidateFile.toPath(),
                currentFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                candidateFile.toPath(),
                currentFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
