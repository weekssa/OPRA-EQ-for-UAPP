package com.weekssa.opraeqforuapp.data.update

import com.weekssa.opraeqforuapp.domain.update.SemVer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AppReleaseInfo(
    val version: String,
    val releaseUrl: String,
    val notes: String,
    val publishedAt: String?,
)

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(val release: AppReleaseInfo) : AppUpdateCheckResult
    data class UpToDate(val release: AppReleaseInfo) : AppUpdateCheckResult
    data object Unavailable : AppUpdateCheckResult
}

class GitHubReleaseUpdateRepository(
    private val latestReleaseUrl: URL = URL(LATEST_RELEASE_API_URL),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun check(installedVersion: String): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val connection = (latestReleaseUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "OPRA-EQ-for-UAPP/$installedVersion")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext AppUpdateCheckResult.Unavailable
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val release = json.decodeFromString<GitHubLatestRelease>(payload)
            val latestVersion = SemVer.parse(release.tagName) ?: return@withContext AppUpdateCheckResult.Unavailable
            val currentVersion = SemVer.parse(installedVersion) ?: return@withContext AppUpdateCheckResult.Unavailable
            val info = AppReleaseInfo(
                version = release.tagName.removePrefix("v"),
                releaseUrl = release.htmlUrl,
                notes = release.body.orEmpty().take(MAX_NOTES_CHARS),
                publishedAt = release.publishedAt,
            )
            if (latestVersion > currentVersion) {
                AppUpdateCheckResult.UpdateAvailable(info)
            } else {
                AppUpdateCheckResult.UpToDate(info)
            }
        } catch (_: IOException) {
            AppUpdateCheckResult.Unavailable
        } catch (_: Exception) {
            AppUpdateCheckResult.Unavailable
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/releases/latest"
        private const val MAX_NOTES_CHARS = 16_000
    }
}

@Serializable
private data class GitHubLatestRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
)
