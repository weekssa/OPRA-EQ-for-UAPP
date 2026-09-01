package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

/** Temporary QA-only fixture used to verify live review-attention clearing on device. */
class QaReviewSeeder(
    private val database: OpraEqDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun seedIfMissing() {
        val dao = database.managedHeadphonesDao()
        if (dao.getHeadphone(PRODUCT_ID) != null) return

        val now = nowMillis()
        val profile = OpraEqProfile(
            id = PROFILE_ID,
            productId = PRODUCT_ID,
            canonicalProfileId = PROFILE_ID,
            author = "QA Fixture",
            details = "Synthetic updated EQ used only to verify that Done clears review attention immediately.",
            link = null,
            profileType = "Parametric EQ",
            preampGainDb = 0.0,
            bands = listOf(
                OpraBand(
                    type = "PK",
                    frequency = 1000.0,
                    gainDb = 1.0,
                    q = 1.0,
                    slope = null,
                ),
            ),
            isVerified = true,
        )
        val codec = ManagedProfileSnapshotCodec()
        val fingerprint = codec.fingerprint(profile)
        val generated = generateManagedPreset(
            productName = PRODUCT_NAME,
            profile = profile,
            fingerprint = fingerprint,
            nowMillis = now,
        )

        dao.upsertHeadphone(
            ManagedHeadphoneEntity(
                productId = PRODUCT_ID,
                vendorId = VENDOR_ID,
                vendorName = VENDOR_NAME,
                productName = PRODUCT_NAME,
                autoIncludeNewProfiles = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        dao.upsertProfiles(
            listOf(
                ManagedProfileEntity(
                    profileId = PROFILE_ID,
                    productId = PRODUCT_ID,
                    selected = true,
                    explicitlyExcluded = false,
                    snapshotJson = codec.encode(profile),
                    fingerprint = fingerprint,
                    firstSeenAtMillis = now,
                    lastSeenAtMillis = now,
                    isNewUnreviewed = false,
                    isUpdatedUnreviewed = true,
                    noLongerAvailable = false,
                    generatedPresetName = generated.presetName,
                    generatedXml = generated.xml,
                    generatedFromFingerprint = generated.fingerprint,
                    generatedAtMillis = generated.generatedAtMillis,
                ),
            ),
        )
        dao.upsertOutputHeadphone(
            OutputManagedHeadphoneEntity(
                outputId = OUTPUT_ID,
                productId = PRODUCT_ID,
                autoIncludeNewProfiles = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        dao.upsertOutputProfiles(
            listOf(
                OutputManagedProfileEntity(
                    outputId = OUTPUT_ID,
                    productId = PRODUCT_ID,
                    profileId = PROFILE_ID,
                    selected = true,
                    explicitlyExcluded = false,
                ),
            ),
        )
    }

    companion object {
        private const val OUTPUT_ID = "UAPP"
        private const val VENDOR_ID = "qa-fixture"
        private const val VENDOR_NAME = "QA Fixture"
        private const val PRODUCT_ID = "qa-review-clear-demo"
        private const val PRODUCT_NAME = "Review Clear Demo"
        private const val PROFILE_ID = "qa-review-clear-demo-profile"
    }
}
