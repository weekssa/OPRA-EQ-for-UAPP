package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType

/**
 * Exact device-native identity for one PEQ band.
 *
 * Units are intentionally opaque here: each device adapter converts its native storage fields to
 * stable integer units (for example Black Pearl gain/Q raw 1/256 units, JA11 gain tenths/Q
 * hundredths, or JM12 gain tenths/Q thousandths). Equality is therefore exact and never depends on
 * floating-point tolerances or display rounding.
 */
data class HardwareEqNativeBandFingerprint(
    val index: Int,
    val enabled: Boolean,
    val type: EqFilterType,
    val frequencyUnits: Long,
    val gainUnits: Long,
    val qUnits: Long,
) {
    init {
        require(index >= 0) { "Native EQ band index must not be negative" }
        require(frequencyUnits > 0) { "Native EQ frequency units must be positive" }
        require(qUnits > 0) { "Native EQ Q units must be positive" }
    }
}

/**
 * Deterministic native EQ fingerprint used for My DAC matching.
 *
 * Active slot and ordinary playback/global volume are deliberately excluded because neither is the
 * acoustic identity of a saved EQ. A dedicated EQ preamp may be included only when that exact
 * device's semantics establish it as part of the EQ representation.
 */
data class HardwareEqNativeFingerprint(
    val deviceId: DacDeviceId,
    val eqEnabled: Boolean,
    val bands: List<HardwareEqNativeBandFingerprint>,
    val dedicatedEqPreampUnits: Long? = null,
) {
    init {
        require(bands.isNotEmpty()) { "Native EQ fingerprint requires at least one hardware band" }
        require(bands.map { it.index }.distinct().size == bands.size) {
            "Native EQ fingerprint band indices must be unique"
        }
        require(bands.map { it.index } == bands.map { it.index }.sorted()) {
            "Native EQ fingerprint bands must be ordered by hardware index"
        }
    }

    /** Flat is response-shape flat; ordinary playback volume and EQ-wide level trim do not change it. */
    val isFlatResponse: Boolean
        get() = !eqEnabled || bands.all { band -> !band.enabled || band.gainUnits == 0L }
}

data class SavedHardwareEqFingerprint(
    val identity: SavedHardwareEqIdentity,
    val fingerprint: HardwareEqNativeFingerprint,
)

/**
 * Saved target derivation metadata retained alongside the exact native fingerprint.
 *
 * Match identity itself remains native equality. Fidelity/adaptation are presentation metadata for
 * the exact deterministic target representation and never participate in equality.
 */
data class SavedHardwareEqRepresentation(
    val identity: SavedHardwareEqIdentity,
    val fingerprint: HardwareEqNativeFingerprint,
    val fidelity: DevicePresetFidelity,
    val adaptationSummary: String,
    val representationVersion: Int,
) {
    init {
        require(adaptationSummary.isNotBlank()) { "Saved hardware adaptation summary must not be blank" }
        require(representationVersion > 0) { "Saved hardware representation version must be positive" }
    }

    fun asFingerprint(): SavedHardwareEqFingerprint = SavedHardwareEqFingerprint(identity, fingerprint)
}

/**
 * A deterministic match plus the saved target representations considered for that device.
 *
 * Keeping candidate metadata with the result lets presentation show fidelity/adaptation for an exact
 * match without re-deriving it or attaching metadata to Unknown hardware state.
 */
data class HardwareEqMatchResolution(
    val match: HardwareEqMatch,
    val savedRepresentations: List<SavedHardwareEqRepresentation>,
) {
    init {
        require(savedRepresentations.map { it.identity.savedEqKey }.distinct().size == savedRepresentations.size) {
            "Resolved saved hardware representations must have unique identities"
        }
    }

    fun representation(savedEqKey: String): SavedHardwareEqRepresentation? =
        savedRepresentations.firstOrNull { it.identity.savedEqKey == savedEqKey }
}

/**
 * More than one saved EQ can quantize to the exact same native hardware representation. In that
 * case EQ Library must not arbitrarily attribute the DAC to one creator/profile.
 */
data class AmbiguousExactHardwareEqMatch(
    val savedEqs: List<SavedHardwareEqIdentity>,
) : HardwareEqMatch {
    init {
        require(savedEqs.size >= 2) { "Ambiguous exact match requires at least two saved EQs" }
        require(savedEqs.map { it.savedEqKey }.distinct().size == savedEqs.size) {
            "Ambiguous exact matches must have unique saved EQ identities"
        }
    }
}

/**
 * Conservative deterministic matcher for a freshly read native hardware EQ fingerprint.
 *
 * There is deliberately no nearest-response/fuzzy path. A non-flat fingerprint is Exact only when
 * the complete native identity equals a saved derived representation. Multiple exact matches remain
 * explicitly ambiguous rather than receiving invented unique provenance. Otherwise the result is
 * Unknown; ModifiedKnown is established separately only when explicit lineage exists.
 */
object HardwareEqMatcher {
    fun match(
        actual: HardwareEqNativeFingerprint,
        saved: List<SavedHardwareEqFingerprint>,
    ): HardwareEqMatch {
        if (actual.isFlatResponse) return HardwareEqMatch.Flat

        val exact = saved
            .asSequence()
            .filter { candidate -> candidate.fingerprint.deviceId == actual.deviceId }
            .filter { candidate -> candidate.fingerprint == actual }
            .distinctBy { candidate -> candidate.identity.savedEqKey }
            .map { candidate -> candidate.identity }
            .sortedBy { identity -> identity.savedEqKey }
            .toList()

        return when (exact.size) {
            0 -> HardwareEqMatch.Unknown
            1 -> HardwareEqMatch.Exact(exact.single())
            else -> AmbiguousExactHardwareEqMatch(exact)
        }
    }

    fun resolve(
        actual: HardwareEqNativeFingerprint,
        saved: List<SavedHardwareEqRepresentation>,
    ): HardwareEqMatchResolution {
        val unique = saved
            .distinctBy { it.identity.savedEqKey }
            .sortedBy { it.identity.savedEqKey }
        return HardwareEqMatchResolution(
            match = match(actual, unique.map(SavedHardwareEqRepresentation::asFingerprint)),
            savedRepresentations = unique,
        )
    }
}
