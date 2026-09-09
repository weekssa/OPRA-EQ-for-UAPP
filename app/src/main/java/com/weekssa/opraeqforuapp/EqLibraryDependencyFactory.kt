package com.weekssa.opraeqforuapp

import android.content.Context
import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlGainStatePreferences
import com.weekssa.opraeqforuapp.data.catalog.HttpOpraCatalogSource
import com.weekssa.opraeqforuapp.data.catalog.OpraCatalogRepository
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.export.AndroidSafDocumentStore
import com.weekssa.opraeqforuapp.data.export.PresetCleanupRepository
import com.weekssa.opraeqforuapp.data.export.PresetExportRepository
import com.weekssa.opraeqforuapp.data.hardware.HardwareEqRepository
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidFiioJa11UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.AndroidJcallyJm12UsbTransport
import com.weekssa.opraeqforuapp.data.kt02h20.JcallyJm12GainStatePreferences
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogRepository
import com.weekssa.opraeqforuapp.data.library.CanonicalFirstCatalogRepository
import com.weekssa.opraeqforuapp.data.library.HttpCanonicalCatalogSource
import com.weekssa.opraeqforuapp.data.library.SavedEqRepository
import com.weekssa.opraeqforuapp.data.library.SavedGeneralEqRepository
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.data.preferences.eqLibraryPreferencesDataStore
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.data.update.AppUpdateCoordinator
import com.weekssa.opraeqforuapp.data.update.GitHubReleaseUpdateRepository
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Flasher
import com.weekssa.opraeqforuapp.ui.EqLibraryViewModel
import java.net.URL

/**
 * Manual DI composition root. Android Context is consumed only while constructing platform data
 * sources; it is never passed to a ViewModel or repository.
 */
internal fun createEqLibraryDependencies(context: Context): EqLibraryViewModel.Dependencies {
    val appContext = context.applicationContext
    val database = OpraEqDatabase.create(appContext)
    val preferencesRepository = AppPreferencesRepository(appContext.eqLibraryPreferencesDataStore)
    val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"

    val catalogRepository = CanonicalFirstCatalogRepository(
        canonicalRepository = CanonicalCatalogRepository(
            filesDir = appContext.filesDir,
            source = HttpCanonicalCatalogSource(
                userAgent = userAgent,
                catalogUrl = URL(BuildConfig.CANONICAL_CATALOG_URL),
            ),
        ),
        legacyFallback = OpraCatalogRepository(
            filesDir = appContext.filesDir,
            source = HttpOpraCatalogSource(
                userAgent = userAgent,
                catalogUrl = URL(BuildConfig.OPRA_CATALOG_URL),
            ),
        ),
    )
    val managedHeadphonesRepository = ManagedHeadphonesRepository(database)
    val savedEqRepository = SavedEqRepository(database)
    val savedGeneralEqRepository = SavedGeneralEqRepository(database)
    val documentStore = AndroidSafDocumentStore(appContext)
    val exportRepository = PresetExportRepository(
        database = database,
        documentStore = documentStore,
    )
    val cleanupRepository = PresetCleanupRepository(
        database = database,
        documentStore = documentStore,
    )
    val syncCoordinator = CatalogSyncCoordinator(
        catalogRepository = catalogRepository,
        managedHeadphonesRepository = managedHeadphonesRepository,
    )
    val updateCoordinator = AppUpdateCoordinator(
        installedVersion = BuildConfig.VERSION_NAME,
        preferencesRepository = preferencesRepository,
        releaseRepository = GitHubReleaseUpdateRepository(
            latestReleaseUrl = URL(BuildConfig.LATEST_RELEASE_API_URL),
        ),
    )

    val blackPearlTransport = AndroidBlackPearlUsbTransport(appContext)
    val fiioJa11Transport = AndroidFiioJa11UsbTransport(appContext)
    val jcallyJm12Transport = AndroidJcallyJm12UsbTransport(appContext)
    val dacSessionRepository = DacSessionRepository(
        blackPearlTransport = blackPearlTransport,
        fiioJa11Transport = fiioJa11Transport,
        jcallyJm12Transport = jcallyJm12Transport,
    )
    val hardwareRepository = HardwareEqRepository(
        dacSessionRepository = dacSessionRepository,
        blackPearlFlasher = BlackPearlFlasher(
            blackPearlTransport,
            BlackPearlGainStatePreferences(appContext),
        ),
        fiioJa11Flasher = FiioJa11Flasher(fiioJa11Transport),
        jcallyJm12Flasher = JcallyJm12Flasher(
            jcallyJm12Transport,
            JcallyJm12GainStatePreferences(appContext),
        ),
    )

    return EqLibraryViewModel.Dependencies(
        preferencesRepository = preferencesRepository,
        catalogRepository = catalogRepository,
        managedHeadphonesRepository = managedHeadphonesRepository,
        savedEqRepository = savedEqRepository,
        savedGeneralEqRepository = savedGeneralEqRepository,
        exportRepository = exportRepository,
        cleanupRepository = cleanupRepository,
        syncCoordinator = syncCoordinator,
        updateCoordinator = updateCoordinator,
        hardwareRepository = hardwareRepository,
    )
}
