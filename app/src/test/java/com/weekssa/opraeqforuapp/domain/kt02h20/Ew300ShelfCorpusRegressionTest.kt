package com.weekssa.opraeqforuapp.domain.kt02h20

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlCapturedEqDraft
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.library.AcousticFingerprint
import com.weekssa.opraeqforuapp.domain.library.AutoEqProfileAdapter
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqProfile
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import com.weekssa.opraeqforuapp.domain.library.CommunityProfileAdapter
import com.weekssa.opraeqforuapp.domain.library.EqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.EqPresetPurpose
import com.weekssa.opraeqforuapp.domain.library.EqProfileScope
import com.weekssa.opraeqforuapp.domain.library.EqRevision
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.ParametricEqTextParser
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import org.junit.Test

/**
 * Cross-source regression corpus for the EW300's derived five-Peak representation.
 *
 * Source shelves remain canonical shelves. Only the device-specific derived representation may be
 * fitted to Peak bands, and only when the fixed RMS/max response gates pass.
 */
class Ew300ShelfCorpusRegressionTest {
    @Test
    fun opraCurrentMixedShelfFixtureIsDeterministicAndNeverSilentlyTruncated() {
        // OPRA public data, CC BY-SA 4.0. Current fixture source reviewed 2026-09-22:
        // Austrian Audio The Composer (Suede Earpads), Oratory1990, Harman Target.
        val source = OpraEqProfile(
            id = "opra:the-composer-suede:oratory1990",
            productId = "the-composer-suede",
            author = "Oratory1990",
            details = "Harman Target",
            link = "https://github.com/opra-project/OPRA/issues/79",
            profileType = "parametric_eq",
            preampGainDb = -6.8,
            bands = listOf(
                band("low_shelf", 105.0, 4.0, 0.71),
                band("peak_dip", 210.0, -1.3, 1.4),
                band("peak_dip", 1_280.0, 3.0, 1.3),
                band("peak_dip", 1_550.0, -2.6, 2.5),
                band("peak_dip", 2_200.0, 7.5, 1.0),
                band("peak_dip", 2_930.0, -5.3, 2.5),
                band("high_shelf", 3_000.0, 1.0, 0.35),
                band("peak_dip", 5_500.0, -5.3, 3.5),
                band("peak_dip", 8_000.0, 2.0, 1.4),
                band("high_shelf", 10_000.0, -1.0, 0.71),
            ),
        )
        val canonical = requireNotNull(
            com.weekssa.opraeqforuapp.domain.library.OpraProfileAdapter.adapt(
                vendor = OpraVendor("austrian_audio", "Austrian Audio"),
                product = OpraProduct(
                    id = source.productId,
                    vendorId = "austrian_audio",
                    name = "The Composer (Suede Earpads)",
                    type = "headphones",
                    subtype = "over_the_ear",
                ),
                profile = source,
                discoveredAtEpochSeconds = 1,
            ),
        )
        val projected = legacyHeadphoneProfile(canonical)

        assertThat(canonical.latestRevision.filters.map { it.type }).containsAtLeast(
            EqFilterType.LOW_SHELF,
            EqFilterType.HIGH_SHELF,
        )
        assertDeterministicShelfContract(projected)
        assertThat(source.bands).containsExactlyElementsIn(source.bands.orEmpty()).inOrder()
    }

    @Test
    fun autoEqShelfTextSurvivesAdapterAndProducesBoundedPeakOnlyRepresentation() {
        val canonical = requireNotNull(
            AutoEqProfileAdapter.adapt(
                metadata = AutoEqProfileAdapter.Metadata(
                    manufacturer = "Fixture",
                    model = "AutoEq Shelf",
                    sourceRecordId = "autoeq-shelf",
                    sourceUrl = "https://github.com/jaakkopasanen/AutoEq",
                    measurementSource = "Fixture measurement",
                    targetName = "Fixture target",
                ),
                parametricEqText = """
                    Preamp: -4.0 dB
                    Filter 1: ON LS Fc 105 Hz Gain 4.0 dB Q 0.71
                    Filter 2: ON PK Fc 1000 Hz Gain -1.5 dB Q 1.20
                """.trimIndent(),
            ),
        )
        val projected = legacyHeadphoneProfile(canonical)

        assertThat(canonical.latestRevision.filters.first().type).isEqualTo(EqFilterType.LOW_SHELF)
        val ready = assertReadyShelfContract(projected)
        assertThat(ready.representation.sourceBandCount).isEqualTo(2)
    }

    @Test
    fun communityHighShelfTextSurvivesAdapterAndProducesBoundedPeakOnlyRepresentation() {
        val canonical = requireNotNull(
            CommunityProfileAdapter.adapt(
                metadata = CommunityProfileAdapter.Metadata(
                    sourceId = "fixture-community",
                    sourceRecordId = "community-high-shelf",
                    sourceUrl = "https://example.invalid/community-high-shelf",
                    manufacturer = "Fixture",
                    model = "Community Shelf",
                    creator = "Fixture Creator",
                ),
                parametricEqText = """
                    Preamp: -3.0 dB
                    Filter 1: ON PK Fc 120 Hz Gain 2.0 dB Q 1.00
                    Filter 2: ON HS Fc 8000 Hz Gain -3.0 dB Q 0.71
                """.trimIndent(),
            ),
        )
        val projected = legacyHeadphoneProfile(canonical)

        assertThat(canonical.latestRevision.filters.last().type).isEqualTo(EqFilterType.HIGH_SHELF)
        assertReadyShelfContract(projected)
    }

    @Test
    fun strictPersonalImportPreservesBothShelfTypesAndSourceValues() {
        val text = """
            Preamp: -3.0 dB
            Filter 1: ON LS Fc 120 Hz Gain 2.5 dB Q 0.71
            Filter 2: ON PK Fc 1000 Hz Gain -1.0 dB Q 1.10
            Filter 3: ON HS Fc 9000 Hz Gain -2.0 dB Q 0.71
        """.trimIndent()
        val strict = ParametricEqTextParser.parseStrictPersonal(text)
        assertThat(strict.isValid).isTrue()
        assertThat(strict.parsedEq.filters.map { it.type }).containsExactly(
            EqFilterType.LOW_SHELF,
            EqFilterType.PEAK,
            EqFilterType.HIGH_SHELF,
        ).inOrder()

        val source = strict.parsedEq.toProfile("personal-import")
        val sourceBands = source.bands.orEmpty().map { it.copy() }
        assertReadyShelfContract(source)
        assertThat(source.bands).containsExactlyElementsIn(sourceBands).inOrder()
    }

    @Test
    fun savedPersonalEqUsesTheSameImmutableShelfProfile() {
        val source = OpraEqProfile(
            id = "personal-eq:shelf",
            productId = "personal-product:shelf",
            author = "Personal",
            details = "Shelf fixture",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -3.0,
            bands = listOf(
                band("low_shelf", 100.0, 3.0, 0.71),
                band("peak_dip", 1_500.0, -1.0, 1.0),
            ),
        )
        val record = SavedEqRecord(
            entryId = "personal:shelf",
            kind = SavedEqKind.Personal,
            sourceProfileId = null,
            productId = source.productId,
            manufacturer = "",
            model = "Shelf fixture",
            displayName = "Shelf fixture",
            profile = source,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )

        val before = record.profile.copy(bands = record.profile.bands?.map { it.copy() })
        assertReadyShelfContract(record.profile)
        assertThat(record.profile).isEqualTo(before)
    }

    @Test
    fun generalPresetProjectionPreservesShelfBeforeEw300Derivation() {
        val filters = listOf(
            EqFilter(EqFilterType.LOW_SHELF, 105.0, 2.0, 0.71),
            EqFilter(EqFilterType.HIGH_SHELF, 9_000.0, -1.5, 0.71),
        )
        val fingerprint = AcousticFingerprint.of(-2.0, filters)
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "general:shelf",
            headphone = null,
            scope = EqProfileScope.GENERAL,
            purpose = EqPresetPurpose.EFFECT,
            creator = "Fixture",
            target = EqTarget(null, EqTargetKind.CUSTOM_USER),
            tuningLabel = "Gentle shelf",
            revisions = listOf(
                EqRevision(
                    revisionId = "general:shelf:r1",
                    acousticFingerprint = fingerprint,
                    preampGainDb = -2.0,
                    filters = filters,
                    sourceReferences = listOf(fixtureSource("general-shelf")),
                    isLatest = true,
                ),
            ),
        )
        val legacy = CanonicalLegacyCatalogAdapter.adapt(snapshotOf(canonical))
        val preset = legacy.generalPresets.single()
        assertThat(preset.bands.map { it.type }).containsExactly("low_shelf", "high_shelf").inOrder()

        assertReadyShelfContract(preset.toProfile())
    }

    @Test
    fun capturedBlackPearlShelvesRemainShelvesBeforeEw300Derivation() {
        val nativeBands = (0 until 10).map { index ->
            when (index) {
                0 -> BlackPearlReadCodec.NativeBand(
                    index = index,
                    type = EqFilterType.LOW_SHELF,
                    frequencyRawHz = 105,
                    gainRaw256 = 3 * 256,
                    qRaw256 = 182,
                    activeSlot = 0,
                )
                1 -> BlackPearlReadCodec.NativeBand(
                    index = index,
                    type = EqFilterType.HIGH_SHELF,
                    frequencyRawHz = 8_000,
                    gainRaw256 = -2 * 256,
                    qRaw256 = 182,
                    activeSlot = 0,
                )
                else -> BlackPearlReadCodec.NativeBand(
                    index = index,
                    type = EqFilterType.PEAK,
                    frequencyRawHz = 1_000 + index * 100,
                    gainRaw256 = 0,
                    qRaw256 = 256,
                    activeSlot = 0,
                )
            }
        }
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = nativeBands,
                globalGainRaw = 0,
                sessionGeneration = 1,
                verifiedAtEpochMillis = 2,
            ),
        )
        val captured = buildBlackPearlCapturedEqDraft(
            captureId = "shelf-capture",
            snapshotBundle = bundle,
            association = null,
        ).profile

        assertThat(captured.bands.orEmpty().take(2).map { it.type })
            .containsExactly("low_shelf", "high_shelf").inOrder()
        assertDeterministicShelfContract(captured)
    }

    @Test
    fun unsupportedActiveFilterIsExplicitlyNotSuitableRatherThanDropped() {
        val source = OpraEqProfile(
            id = "not-suitable",
            productId = "fixture",
            author = "Fixture",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -2.0,
            bands = listOf(band("low_pass", 6_000.0, 0.0, 0.71)),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, HardwareEqDeviceSpecs.SIMGOT_EW300)

        assertThat(result).isInstanceOf(FiveBandOptimizationResult.NotSuitable::class.java)
        assertThat((result as FiveBandOptimizationResult.NotSuitable).reason).contains("cannot represent")
        assertThat(source.bands.orEmpty().single().type).isEqualTo("low_pass")
    }

    private fun assertReadyShelfContract(profile: OpraEqProfile): FiveBandOptimizationResult.Ready {
        val first = deterministicPair(profile).first
        assertThat(first).isInstanceOf(FiveBandOptimizationResult.Ready::class.java)
        val ready = first as FiveBandOptimizationResult.Ready
        assertThat(ready.representation.fidelity).isEqualTo(DevicePresetFidelity.OPTIMIZED)
        assertThat(ready.representation.usedResponseFit).isTrue()
        assertThat(ready.representation.bands).hasSizeAtMost(5)
        assertThat(ready.representation.bands.map { it.type }.distinct()).containsExactly("peak_dip")
        assertThat(ready.representation.rmsErrorDb)
            .isAtMost(HardwareEqDeviceSpecs.SIMGOT_EW300.maxRmsErrorDb)
        assertThat(ready.representation.maxAbsoluteErrorDb)
            .isAtMost(HardwareEqDeviceSpecs.SIMGOT_EW300.maxAbsoluteErrorDb)
        return ready
    }

    private fun assertDeterministicShelfContract(profile: OpraEqProfile) {
        val (first, second) = deterministicPair(profile)
        assertThat(second).isEqualTo(first)
        when (first) {
            is FiveBandOptimizationResult.Ready -> {
                assertThat(first.representation.fidelity).isEqualTo(DevicePresetFidelity.OPTIMIZED)
                assertThat(first.representation.bands).hasSizeAtMost(5)
                assertThat(first.representation.bands.map { it.type }.distinct()).containsExactly("peak_dip")
                assertThat(first.representation.rmsErrorDb)
                    .isAtMost(HardwareEqDeviceSpecs.SIMGOT_EW300.maxRmsErrorDb)
                assertThat(first.representation.maxAbsoluteErrorDb)
                    .isAtMost(HardwareEqDeviceSpecs.SIMGOT_EW300.maxAbsoluteErrorDb)
            }
            is FiveBandOptimizationResult.NotSuitable -> assertThat(first.reason).isNotEmpty()
        }
    }

    private fun deterministicPair(
        profile: OpraEqProfile,
    ): Pair<FiveBandOptimizationResult, FiveBandOptimizationResult> {
        Kt02h20FiveBandOptimizer.clearCache()
        val first = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)
        Kt02h20FiveBandOptimizer.clearCache()
        val second = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)
        return first to second
    }

    private fun legacyHeadphoneProfile(canonical: CanonicalEqProfile): OpraEqProfile =
        CanonicalLegacyCatalogAdapter.adapt(snapshotOf(canonical)).profiles.single()

    private fun snapshotOf(vararg profiles: CanonicalEqProfile): CatalogSnapshot = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-09-22T00:00:00Z",
        sourceRegistryVersion = "shelf-corpus-test",
        profiles = profiles.toList(),
    )

    private fun ParametricEqTextParser.ParsedEq.toProfile(id: String): OpraEqProfile = OpraEqProfile(
        id = id,
        productId = "personal-product:$id",
        author = "Personal",
        details = "Strict imported shelf fixture",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preampGainDb,
        bands = filters.map { filter ->
            OpraBand(
                type = when (filter.type) {
                    EqFilterType.PEAK -> "peak_dip"
                    EqFilterType.LOW_SHELF -> "low_shelf"
                    EqFilterType.HIGH_SHELF -> "high_shelf"
                    EqFilterType.LOW_PASS -> "low_pass"
                    EqFilterType.HIGH_PASS -> "high_pass"
                    EqFilterType.OTHER -> "other"
                },
                frequency = filter.frequencyHz,
                gainDb = filter.gainDb,
                q = filter.q,
                slope = filter.slope,
            )
        },
    )

    private fun GeneralEqPreset.toProfile(): OpraEqProfile = OpraEqProfile(
        id = id,
        productId = "eq-library-general",
        canonicalProfileId = canonicalProfileId,
        author = creator,
        details = soundImpactSummary,
        link = sourceUrl,
        profileType = "parametric_eq",
        preampGainDb = preampGainDb,
        bands = bands,
        eqLibrarySafetyHeadroomDb = eqLibrarySafetyHeadroomDb,
        isVerified = isVerified,
    )

    private fun fixtureSource(recordId: String): EqSourceReference = EqSourceReference(
        sourceId = "fixture",
        sourceKind = EqSourceKind.COMMUNITY,
        sourceRecordId = recordId,
        url = "https://example.invalid/$recordId",
        creator = "Fixture",
        provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
        redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        isPrimary = true,
    )

    private fun band(type: String, frequency: Double, gain: Double, q: Double): OpraBand =
        OpraBand(type = type, frequency = frequency, gainDb = gain, q = q, slope = null)
}
