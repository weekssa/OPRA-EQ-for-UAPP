package com.weekssa.opraeqforuapp.data.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.export.ExportDirectoryHandle
import com.weekssa.opraeqforuapp.data.export.ExportDocumentHandle
import com.weekssa.opraeqforuapp.data.export.ExportDocumentStore
import com.weekssa.opraeqforuapp.data.export.ExportLookup
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphoneEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnclaimedEqRecoveryRaceTest {
    @Test
    fun sourceBecomingUnrepresentableAfterPreflightDoesNotSaveOrRewriteOwnership() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java).build()
        try {
            val codec = ManagedProfileSnapshotCodec()
            val initialSource = profile(bandCount = 1)
            val initialProfileRow = managedProfile(initialSource, codec)
            val ownership = ExportOwnershipEntity(
                documentUri = "content://eq-library/owned-export",
                treeUri = "content://eq-library/tree",
                relativeDirectory = "${ExportDevice.UAPP.folderName}/Maker/Model",
                profileId = initialSource.id,
                productId = initialSource.productId,
                fileName = "Old source.txt",
                exportedFingerprint = "original-fingerprint",
                exportedContentHash = "original-content-hash",
                exportedAtMillis = 123L,
            )
            database.managedHeadphonesDao().upsertHeadphone(
                ManagedHeadphoneEntity(
                    productId = initialSource.productId,
                    vendorId = "vendor",
                    vendorName = "Maker",
                    productName = "Model",
                    autoIncludeNewProfiles = false,
                    createdAtMillis = 1L,
                    updatedAtMillis = 1L,
                ),
            )
            database.managedHeadphonesDao().upsertProfiles(listOf(initialProfileRow))
            database.exportOwnershipDao().upsert(ownership)

            val changedDuringRead = profile(bandCount = 11)
            val changedProfileRow = managedProfile(changedDuringRead, codec)
            val documentStore = RecoveryDocumentStore(
                onRead = {
                    database.managedHeadphonesDao().upsertProfiles(listOf(changedProfileRow))
                },
            )
            val repository = UnclaimedEqRepository(
                database = database,
                documentStore = documentStore,
                snapshotCodec = codec,
                ioDispatcher = Dispatchers.IO,
            )

            val failure = runCatching {
                repository.recoverToPersonal(
                    documentUri = ownership.documentUri,
                    manufacturer = "Maker",
                    model = "Model",
                    displayName = "Recovered EQ",
                )
            }.exceptionOrNull()

            assertTrue("Recovery should be rejected after the source becomes unrepresentable", failure != null)
            assertTrue(failure?.message.orEmpty().contains("may no longer match"))
            assertTrue(documentStore.sourceChangedDuringRead)
            assertTrue(database.savedEqDao().observeAll().first().isEmpty())
            assertEquals(ownership, database.exportOwnershipDao().getByDocumentUri(ownership.documentUri))
            assertEquals(changedProfileRow, database.managedHeadphonesDao().getProfiles(initialSource.productId).single())
            assertFalse(shouldBlockUappExportRecovery(ownership, initialProfileRow, codec))
            assertTrue(shouldBlockUappExportRecovery(ownership, changedProfileRow, codec))
        } finally {
            database.close()
        }
    }

    @Test
    fun concurrentRecoveryRequestsCreateOnePersonalEqAndKeepOneOwnedFileAssociation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java).build()
        try {
            val codec = ManagedProfileSnapshotCodec()
            val source = profile(bandCount = 1)
            val ownership = ExportOwnershipEntity(
                documentUri = "content://eq-library/repeated-export",
                treeUri = "content://eq-library/tree",
                relativeDirectory = "${ExportDevice.UAPP.folderName}/Maker/Model",
                profileId = source.id,
                productId = source.productId,
                fileName = "Recover once.txt",
                exportedFingerprint = "fingerprint",
                exportedContentHash = "content-hash",
                exportedAtMillis = 456L,
            )
            database.managedHeadphonesDao().upsertHeadphone(
                ManagedHeadphoneEntity(
                    productId = source.productId,
                    vendorId = "vendor",
                    vendorName = "Maker",
                    productName = "Model",
                    autoIncludeNewProfiles = false,
                    createdAtMillis = 1L,
                    updatedAtMillis = 1L,
                ),
            )
            database.managedHeadphonesDao().upsertProfiles(listOf(managedProfile(source, codec)))
            database.exportOwnershipDao().upsert(ownership)

            val documentStore = RecoveryDocumentStore(
                onRead = {},
                synchronizeConcurrentReads = true,
                markSourceChangedDuringRead = false,
            )
            val repository = UnclaimedEqRepository(
                database = database,
                documentStore = documentStore,
                snapshotCodec = codec,
                ioDispatcher = Dispatchers.IO,
            )

            val recovered = listOf(
                async(Dispatchers.IO) {
                    repository.recoverToPersonal(ownership.documentUri, "Maker", "Model", "Recovered once")
                },
                async(Dispatchers.IO) {
                    repository.recoverToPersonal(
                        ownership.documentUri,
                        "Maker",
                        "Model",
                        "A different duplicate name",
                    )
                },
            ).awaitAll()
            assertFalse("The no-op document store must not report source mutation", documentStore.sourceChangedDuringRead)
            val ownershipAfterConcurrentRecovery = database.exportOwnershipDao().getByDocumentUri(ownership.documentUri)
            val repeated = repository.recoverToPersonal(
                ownership.documentUri,
                "Maker",
                "Model",
                "Another duplicate name",
            )

            assertEquals(1, recovered.map { it.entryId }.distinct().size)
            assertEquals(recovered.first().entryId, repeated.entryId)
            assertEquals(1, database.savedEqDao().observeAll().first().size)
            assertEquals(
                ownershipAfterConcurrentRecovery,
                database.exportOwnershipDao().getByDocumentUri(ownership.documentUri),
            )
            assertEquals(2, documentStore.readCount)
        } finally {
            database.close()
        }
    }

    @Test
    fun malformedPersonalAssociationFailsBeforeReadingOrSaving() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java).build()
        try {
            val ownership = ExportOwnershipEntity(
                documentUri = "content://eq-library/malformed-personal-link",
                treeUri = "content://eq-library/tree",
                relativeDirectory = "${ExportDevice.UAPP.folderName}/Maker/Model",
                profileId = "personal-eq:not-a-uuid",
                productId = "personal-product:orphan",
                fileName = "Malformed association.xml",
                exportedFingerprint = "fingerprint",
                exportedContentHash = "content-hash",
                exportedAtMillis = 789L,
            )
            database.exportOwnershipDao().upsert(ownership)
            val documentStore = RecoveryDocumentStore(onRead = {})
            val repository = UnclaimedEqRepository(
                database = database,
                documentStore = documentStore,
                ioDispatcher = Dispatchers.IO,
            )

            val failure = runCatching {
                repository.recoverToPersonal(
                    documentUri = ownership.documentUri,
                    manufacturer = "Maker",
                    model = "Model",
                    displayName = "Recovered EQ",
                )
            }.exceptionOrNull()

            assertTrue("Malformed association must fail closed", failure != null)
            assertTrue(failure?.message.orEmpty().contains("malformed Personal EQ association"))
            assertEquals(0, documentStore.readCount)
            assertTrue(database.savedEqDao().observeAll().first().isEmpty())
            assertEquals(ownership, database.exportOwnershipDao().getByDocumentUri(ownership.documentUri))
        } finally {
            database.close()
        }
    }

    private fun profile(bandCount: Int) = OpraEqProfile(
        id = PROFILE_ID,
        productId = PRODUCT_ID,
        author = "Creator",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = (1..bandCount).map { index ->
            OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
        },
    )

    private fun managedProfile(
        profile: OpraEqProfile,
        codec: ManagedProfileSnapshotCodec,
    ) = ManagedProfileEntity(
        profileId = profile.id,
        productId = profile.productId,
        selected = true,
        explicitlyExcluded = false,
        snapshotJson = codec.encode(profile),
        fingerprint = codec.fingerprint(profile),
        firstSeenAtMillis = 1L,
        lastSeenAtMillis = 1L,
        isNewUnreviewed = false,
        isUpdatedUnreviewed = false,
        noLongerAvailable = false,
        generatedPresetName = "Old source preset",
        generatedXml = "old-generated-xml",
        generatedFromFingerprint = "old-fingerprint",
        generatedAtMillis = 1L,
    )

    private class RecoveryDocumentStore(
        private val onRead: suspend () -> Unit,
        private val synchronizeConcurrentReads: Boolean = false,
        private val markSourceChangedDuringRead: Boolean = true,
    ) : ExportDocumentStore {
        var sourceChangedDuringRead: Boolean = false
            private set
        private val reads = AtomicInteger()
        private val bothInitialReadsReached = CountDownLatch(1)
        val readCount: Int get() = reads.get()

        override fun openWritableTree(treeUri: String): ExportDirectoryHandle? = null

        override fun openDocument(documentUri: String): ExportLookup<ExportDocumentHandle> =
            ExportLookup.Found(TestDocument(documentUri))

        override fun findDirectory(parent: ExportDirectoryHandle, name: String): ExportLookup<ExportDirectoryHandle> =
            ExportLookup.Missing

        override fun createDirectory(parent: ExportDirectoryHandle, name: String): ExportDirectoryHandle? = null

        override fun findFile(parent: ExportDirectoryHandle, name: String): ExportLookup<ExportDocumentHandle> =
            ExportLookup.Missing

        override fun createFile(
            parent: ExportDirectoryHandle,
            mimeType: String,
            displayName: String,
        ): ExportDocumentHandle? = null

        override fun contentHash(document: ExportDocumentHandle): String? = null

        override fun readBytes(document: ExportDocumentHandle, maxBytes: Int): ByteArray? {
            val currentReadCount = reads.incrementAndGet()
            runBlocking { onRead() }
            if (markSourceChangedDuringRead) sourceChangedDuringRead = true
            if (synchronizeConcurrentReads && currentReadCount <= 2) {
                if (currentReadCount == 2) bothInitialReadsReached.countDown()
                check(bothInitialReadsReached.await(15, TimeUnit.SECONDS)) {
                    "Both recovery requests must reach document read before either can commit."
                }
            }
            return "Filter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1.0".toByteArray()
        }

        override fun writeBytes(document: ExportDocumentHandle, bytes: ByteArray): Boolean = false

        override fun delete(document: ExportDocumentHandle): Boolean = false

        override fun deleteByUri(documentUri: String): Boolean = false
    }

    private data class TestDocument(
        override val uri: String,
    ) : ExportDocumentHandle {
        override val name: String = "Old source.txt"
    }

    private companion object {
        const val PROFILE_ID = "profile:race"
        const val PRODUCT_ID = "product:race"
    }
}
