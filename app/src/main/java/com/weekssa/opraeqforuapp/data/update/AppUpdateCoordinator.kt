package com.weekssa.opraeqforuapp.data.update

import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository

class AppUpdateCoordinator(
    private val installedVersion: String,
    private val preferencesRepository: AppPreferencesRepository,
    private val releaseRepository: GitHubReleaseUpdateRepository = GitHubReleaseUpdateRepository(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun initialize() {
        preferencesRepository.initializeInstalledVersion(installedVersion)
        val now = nowMillis()
        val lastAttempt = preferencesRepository.snapshot().updates.lastCheckAttemptMillis
        if (lastAttempt == null || now - lastAttempt >= AUTOMATIC_CHECK_INTERVAL_MILLIS) {
            checkNow()
        }
    }

    suspend fun checkNow(): AppUpdateCheckResult {
        val now = nowMillis()
        preferencesRepository.markUpdateCheckAttempt(now)
        val result = releaseRepository.check(installedVersion)
        when (result) {
            is AppUpdateCheckResult.UpdateAvailable ->
                preferencesRepository.storeLatestRelease(result.release, now)
            is AppUpdateCheckResult.UpToDate ->
                preferencesRepository.storeLatestRelease(result.release, now)
            AppUpdateCheckResult.Unavailable -> Unit
        }
        return result
    }

    companion object {
        private const val AUTOMATIC_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
