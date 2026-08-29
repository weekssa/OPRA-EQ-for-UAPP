package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface CanonicalCatalogState {
    data object Loading : CanonicalCatalogState

    data class Ready(
        val snapshot: CatalogSnapshot,
        val refreshedAtMillis: Long,
        val isRefreshing: Boolean = false,
    ) : CanonicalCatalogState

    data class Unavailable(val reason: CanonicalCatalogFailureReason) : CanonicalCatalogState
}

enum class CanonicalCatalogFailureReason {
    Network,
    InvalidCatalog,
    Storage,
}

sealed interface CanonicalCatalogRefreshResult {
    data class Success(
        val snapshot: CatalogSnapshot,
        val refreshedAtMillis: Long,
    ) : CanonicalCatalogRefreshResult

    data class Failure(
        val reason: CanonicalCatalogFailureReason,
        val usingLastKnownGood: Boolean,
    ) : CanonicalCatalogRefreshResult
}

fun interface CanonicalCatalogSource {
    suspend fun downloadTo(destination: File)
}

class HttpCanonicalCatalogSource(
    private val userAgent: String,
    private val catalogUrl: URL = URL(DEFAULT_CATALOG_URL),
) : CanonicalCatalogSource {
    override suspend fun downloadTo(destination: File) = withContext(Dispatchers.IO) {
        val connection = (catalogUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Canonical catalog request failed with HTTP $responseCode.")
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_CATALOG_BYTES) {
                throw IOException("Canonical catalog exceeds the download safety limit.")
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
                            throw IOException("Canonical catalog exceeds the download safety limit.")
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
        const val DEFAULT_CATALOG_URL =
            "https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/eq-library-community-v0.3/catalog/catalog.json"
        private const val CONNECT_TIMEOUT_MILLIS = 20_000
        private const val READ_TIMEOUT_MILLIS = 60_000
        private const val MAX_CATALOG_BYTES = 64L * 1024L * 1024L
    }
}

class CanonicalCatalogRepository(
    filesDir: File,
    private val source: CanonicalCatalogSource,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val catalogDirectory = File(filesDir, "eq-library/catalog")
    private val currentFile = File(catalogDirectory, "catalog.json")
    private val candidateFile = File(catalogDirectory, "catalog.candidate.json")
    private val mutableState = MutableStateFlow<CanonicalCatalogState>(CanonicalCatalogState.Loading)

    val state: StateFlow<CanonicalCatalogState> = mutableState.asStateFlow()

    suspend fun initialize() {
        val cached = loadSnapshot(currentFile)?.takeIf(::isValid)
        if (cached != null) {
            mutableState.value = CanonicalCatalogState.Ready(
                snapshot = cached,
                refreshedAtMillis = currentFile.lastModified().takeIf { it > 0L } ?: nowMillis(),
            )
        }
        val ready = mutableState.value as? CanonicalCatalogState.Ready
        val isFresh = ready != null && nowMillis() - ready.refreshedAtMillis < AUTO_REFRESH_INTERVAL_MILLIS
        if (!isFresh) refresh()
    }

    suspend fun refresh(): CanonicalCatalogRefreshResult = processRefreshMutex.withLock {
        val previous = mutableState.value as? CanonicalCatalogState.Ready
        mutableState.value = previous?.copy(isRefreshing = true) ?: CanonicalCatalogState.Loading

        val directoryReady = withContext(Dispatchers.IO) {
            catalogDirectory.isDirectory || catalogDirectory.mkdirs()
        }
        if (!directoryReady) return@withLock fail(CanonicalCatalogFailureReason.Storage, previous)

        try {
            try {
                candidateFile.delete()
                source.downloadTo(candidateFile)
            } catch (_: IOException) {
                return@withLock fail(CanonicalCatalogFailureReason.Network, previous)
            }

            val candidate = loadSnapshot(candidateFile)
                ?: return@withLock fail(CanonicalCatalogFailureReason.InvalidCatalog, previous)

            if (!isValid(candidate)) {
                return@withLock fail(CanonicalCatalogFailureReason.InvalidCatalog, previous)
            }

            val refreshedAt = nowMillis()
            try {
                withContext(Dispatchers.IO) {
                    promoteCandidate()
                    currentFile.setLastModified(refreshedAt)
                }
            } catch (_: IOException) {
                return@withLock fail(CanonicalCatalogFailureReason.Storage, previous)
            }

            mutableState.value = CanonicalCatalogState.Ready(candidate, refreshedAt)
            CanonicalCatalogRefreshResult.Success(candidate, refreshedAt)
        } finally {
            candidateFile.delete()
        }
    }

    private suspend fun loadSnapshot(file: File): CatalogSnapshot? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        try {
            json.decodeFromString<CatalogSnapshot>(file.readText(Charsets.UTF_8))
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun isValid(snapshot: CatalogSnapshot): Boolean {
        if (snapshot.schemaVersion < 1) return false
        if (snapshot.generatedAt.isBlank() || snapshot.sourceRegistryVersion.isBlank()) return false
        if (snapshot.profiles.isEmpty()) return false
        val profileIds = mutableSetOf<String>()
        return snapshot.profiles.all { profile ->
            profile.canonicalProfileId.isNotBlank() &&
                profileIds.add(profile.canonicalProfileId) &&
                profile.revisions.isNotEmpty() &&
                profile.revisions.count { it.isLatest } == 1 &&
                profile.revisions.all { revision ->
                    revision.revisionId.isNotBlank() && revision.acousticFingerprint.isNotBlank() && revision.filters.isNotEmpty()
                }
        }
    }

    private fun fail(
        reason: CanonicalCatalogFailureReason,
        previous: CanonicalCatalogState.Ready?,
    ): CanonicalCatalogRefreshResult.Failure {
        mutableState.value = previous?.copy(isRefreshing = false) ?: CanonicalCatalogState.Unavailable(reason)
        return CanonicalCatalogRefreshResult.Failure(reason, usingLastKnownGood = previous != null)
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
            Files.move(candidateFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private val processRefreshMutex = Mutex()
    }
}
