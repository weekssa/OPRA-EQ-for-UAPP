package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifference
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifferenceField
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqRepresentation
import java.util.Locale

/**
 * Session-scoped proof that one non-exact Black Pearl state was produced by EQ Library from a
 * specific saved representation. The actual fingerprint is intentionally exact: any later outside
 * hardware change breaks this proof and the state returns to Unknown rather than using similarity.
 */
data class BlackPearlKnownLineage(
    val savedRepresentation: SavedHardwareEqRepresentation,
    val actualFingerprint: HardwareEqNativeFingerprint,
    val differences: List<HardwareEqDifference>,
) {
    init {
        require(savedRepresentation.fingerprint.deviceId == DacDeviceId.TRN_BLACK_PEARL)
        require(actualFingerprint.deviceId == DacDeviceId.TRN_BLACK_PEARL)
        require(savedRepresentation.fingerprint != actualFingerprint) {
            "Known modified lineage requires a non-exact hardware representation."
        }
        require(differences.isNotEmpty()) { "Known modified lineage requires explicit differences." }
    }
}

fun buildBlackPearlKnownLineage(
    savedRepresentation: SavedHardwareEqRepresentation,
    actualFingerprint: HardwareEqNativeFingerprint,
): BlackPearlKnownLineage? {
    val expected = savedRepresentation.fingerprint
    require(expected.deviceId == DacDeviceId.TRN_BLACK_PEARL) {
        "Black Pearl lineage requires a Black Pearl saved representation."
    }
    require(actualFingerprint.deviceId == DacDeviceId.TRN_BLACK_PEARL) {
        "Black Pearl lineage requires a Black Pearl actual fingerprint."
    }
    if (expected == actualFingerprint) return null
    // The v0.6 editor cannot change a device-wide EQ-enabled state. If that state differs, the
    // observed hardware no longer proves that the editor created the complete current state.
    if (expected.eqEnabled != actualFingerprint.eqEnabled) return null

    val differences = blackPearlNativeDifferences(expected, actualFingerprint)
    if (differences.isEmpty()) return null
    return BlackPearlKnownLineage(
        savedRepresentation = savedRepresentation,
        actualFingerprint = actualFingerprint,
        differences = differences,
    )
}

/**
 * Applies lineage only after ordinary Flat/Exact/ambiguous matching has failed. This guarantees that
 * exact truth always wins and that a merely similar cold-start hardware state remains Unknown.
 */
fun HardwareEqMatchResolution.withBlackPearlKnownLineage(
    actualFingerprint: HardwareEqNativeFingerprint,
    lineage: BlackPearlKnownLineage?,
): HardwareEqMatchResolution {
    if (match !is HardwareEqMatch.Unknown) return this
    val proven = lineage?.takeIf { it.actualFingerprint == actualFingerprint } ?: return this
    return copy(
        match = HardwareEqMatch.ModifiedKnown(
            savedEq = proven.savedRepresentation.identity,
            differences = proven.differences,
        ),
    )
}

private fun blackPearlNativeDifferences(
    expected: HardwareEqNativeFingerprint,
    actual: HardwareEqNativeFingerprint,
): List<HardwareEqDifference> {
    val expectedByIndex = expected.bands.associateBy(HardwareEqNativeBandFingerprint::index)
    val actualByIndex = actual.bands.associateBy(HardwareEqNativeBandFingerprint::index)
    if (expectedByIndex.keys != actualByIndex.keys) return emptyList()

    return buildList {
        expected.bands.forEach { expectedBand ->
            val actualBand = actualByIndex.getValue(expectedBand.index)
            if (expectedBand.enabled != actualBand.enabled) {
                addDifference(
                    expectedBand.index,
                    HardwareEqDifferenceField.ENABLED,
                    expectedBand.enabled.toString(),
                    actualBand.enabled.toString(),
                )
            }
            if (expectedBand.type != actualBand.type) {
                addDifference(
                    expectedBand.index,
                    HardwareEqDifferenceField.FILTER_TYPE,
                    expectedBand.type.name,
                    actualBand.type.name,
                )
            }
            if (expectedBand.frequencyUnits != actualBand.frequencyUnits) {
                addDifference(
                    expectedBand.index,
                    HardwareEqDifferenceField.FREQUENCY_HZ,
                    "${expectedBand.frequencyUnits} Hz",
                    "${actualBand.frequencyUnits} Hz",
                )
            }
            if (expectedBand.gainUnits != actualBand.gainUnits) {
                addDifference(
                    expectedBand.index,
                    HardwareEqDifferenceField.GAIN_DB,
                    formatDb(expectedBand.gainUnits),
                    formatDb(actualBand.gainUnits),
                )
            }
            if (expectedBand.qUnits != actualBand.qUnits) {
                addDifference(
                    expectedBand.index,
                    HardwareEqDifferenceField.Q,
                    formatQ(expectedBand.qUnits),
                    formatQ(actualBand.qUnits),
                )
            }
        }
    }
}

private fun MutableList<HardwareEqDifference>.addDifference(
    bandIndex: Int,
    field: HardwareEqDifferenceField,
    expected: String,
    actual: String,
) {
    add(
        HardwareEqDifference(
            bandIndex = bandIndex,
            field = field,
            expectedValue = expected,
            actualValue = actual,
        ),
    )
}

private fun formatDb(rawUnits: Long): String =
    String.format(Locale.US, "%+.2f dB", rawUnits.toDouble() / BlackPearlProtocol.GLOBAL_GAIN_RAW_PER_DB)

private fun formatQ(rawUnits: Long): String =
    String.format(Locale.US, "%.3f", rawUnits.toDouble() / 256.0)
