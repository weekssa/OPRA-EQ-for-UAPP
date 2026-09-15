package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlCapturedEqTest {
    @Test
    fun capturePreservesAllNativeBandsExcludesPlaybackGainAndRematchesExactly() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = nativeBands(),
                globalGainRaw = -18 * 256,
                sessionGeneration = 7,
                verifiedAtEpochMillis = 1234L,
            ),
        )

        val draft = buildBlackPearlCapturedEqDraft(
            captureId = "capture-1",
            snapshotBundle = bundle,
            association = null,
        )

        assertEquals(10, draft.profile.bands?.size)
        assertNull(draft.profile.preampGainDb)
        assertTrue(draft.manufacturer.isBlank())
        assertTrue(draft.model.isBlank())
        assertEquals(bundle.fingerprint, draft.captureMetadata.nativeFingerprint)
        assertEquals(2.0, draft.profile.bands!![0].gainDb!!, 0.0)
        assertEquals(1.0, draft.profile.bands!![0].q!!, 0.0)

        val resolved = BlackPearlHardwareEqMatchResolver.resolve(
            actual = bundle.fingerprint,
            candidates = listOf(
                BlackPearlSavedEqCandidate(
                    identity = SavedHardwareEqIdentity(
                        savedEqKey = "saved:captured",
                        displayName = "Captured EQ",
                    ),
                    profile = draft.profile,
                ),
            ),
        )
        assertTrue(resolved.match is HardwareEqMatch.Exact)
        assertEquals(
            "saved:captured",
            (resolved.match as HardwareEqMatch.Exact).savedEq.savedEqKey,
        )
    }

    @Test
    fun capturePreservesRealHeadphoneAssociationWithoutChangingNativeIdentity() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = nativeBands(),
                globalGainRaw = -8 * 256,
                sessionGeneration = 3,
                verifiedAtEpochMillis = 44L,
            ),
        )
        val association = SavedEqHeadphoneAssociation(
            productId = "headphone-123",
            manufacturer = "Example Audio",
            model = "Reference One",
        )

        val draft = buildBlackPearlCapturedEqDraft("capture-2", bundle, association)

        assertEquals(association.productId, draft.productId)
        assertEquals(association.manufacturer, draft.manufacturer)
        assertEquals(association.model, draft.model)
        assertEquals(bundle.fingerprint, draft.captureMetadata.nativeFingerprint)
    }

    private fun nativeBands(): List<BlackPearlReadCodec.NativeBand> =
        (0 until BlackPearlProtocol.BAND_COUNT).map { index ->
            BlackPearlReadCodec.NativeBand(
                index = index,
                type = when (index) {
                    1 -> EqFilterType.LOW_SHELF
                    8 -> EqFilterType.HIGH_SHELF
                    else -> EqFilterType.PEAK
                },
                frequencyRawHz = 100 + index * 700,
                gainRaw256 = if (index == 0) 2 * 256 else 0,
                qRaw256 = 256,
                activeSlot = 2,
            )
        }
}
