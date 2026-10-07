package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

private const val FNV_OFFSET_BASIS = -3750763034362895579L
private const val FNV_PRIME = 1099511628211L

/**
 * Stable exact acoustic identity for legacy compatibility records.
 *
 * Record IDs, author names, source links, and display metadata are deliberately excluded. Filter
 * order and equivalent legacy filter aliases are normalized so mirrors of the same preset resolve
 * to the same identity.
 */
internal fun OpraEqProfile.legacyAcousticSignature(): String? {
    val normalizedBands = sortedLegacyAcousticBandsOrNull() ?: return null
    return buildString {
        append("preamp=")
        append(legacyAcousticFormat(preampGainDb ?: 0.0, 3))
        normalizedBands.forEach {
            append(';')
            append(it)
        }
    }
}

/** Computes the signature hash without materializing the full per-profile signature string. */
internal fun OpraEqProfile.legacyAcousticFingerprintOrNull(): Long? {
    val sourceBands = bands.orEmpty()
    if (sourceBands.isEmpty()) return null

    // Keep only primitive hashes while deriving the bucket key. The full normalized strings are
    // still constructed one at a time, but are not all retained and sorted for every profile.
    val normalizedBandHashes = LongArray(sourceBands.size)
    var normalizedBandCount = 0
    sourceBands.forEach { band ->
        band.legacyAcousticKey()?.let { key ->
            normalizedBandHashes[normalizedBandCount++] = key.legacyAcousticFingerprint()
        }
    }
    if (normalizedBandCount == 0) return null
    normalizedBandHashes.sort(0, normalizedBandCount)

    var fingerprint = FNV_OFFSET_BASIS
    fingerprint = fingerprintAppend(fingerprint, "preamp=")
    fingerprint = fingerprintAppend(fingerprint, legacyAcousticFormat(preampGainDb ?: 0.0, 3))
    fingerprint = fingerprintAppendLong(fingerprint, normalizedBandCount.toLong())
    for (index in 0 until normalizedBandCount) {
        fingerprint = fingerprintAppendLong(fingerprint, normalizedBandHashes[index])
    }
    return fingerprint
}

private fun OpraEqProfile.sortedLegacyAcousticBandsOrNull(): ArrayList<String>? {
    val sourceBands = bands.orEmpty()
    if (sourceBands.isEmpty()) return null

    val normalizedBands = ArrayList<String>(sourceBands.size)
    sourceBands.forEach { band -> band.legacyAcousticKey()?.let(normalizedBands::add) }
    if (normalizedBands.isEmpty()) return null
    normalizedBands.sort()
    return normalizedBands
}

private fun String.legacyAcousticFingerprint(): Long {
    return fingerprintAppend(FNV_OFFSET_BASIS, this)
}

private fun fingerprintAppendLong(initial: Long, value: Long): Long {
    var hash = initial
    for (shift in 0..56 step 8) {
        hash = (hash xor ((value ushr shift) and 0xffL)) * FNV_PRIME
    }
    return hash
}

private fun fingerprintAppend(initial: Long, value: String): Long {
    var hash = initial
    for (character in value) {
        hash = (hash xor character.code.toLong()) * FNV_PRIME
    }
    return hash
}

private fun OpraBand.legacyAcousticKey(): String? {
    val frequencyValue = frequency ?: return null
    return buildString {
        append(normalizedLegacyFilterType(type))
        append('|')
        appendLegacyAcousticFormat(frequencyValue, 3)
        append('|')
        appendLegacyAcousticFormat(gainDb ?: 0.0, 3)
        append('|')
        appendLegacyAcousticFormat(q ?: 0.0, 4)
        append('|')
        appendLegacyAcousticFormat(slope ?: 0.0, 4)
    }
}

private fun normalizedLegacyFilterType(value: String?): String = when (value?.trim()?.lowercase(Locale.ROOT)) {
    "peak_dip", "peak", "pk", "peq" -> "PK"
    "low_shelf", "ls", "lsc" -> "LS"
    "high_shelf", "hs", "hsc" -> "HS"
    "low_pass", "lp" -> "LP"
    "high_pass", "hp" -> "HP"
    else -> value.orEmpty().trim().uppercase(Locale.ROOT)
}

private fun legacyAcousticFormat(value: Double, decimals: Int): String {
    return buildString(16) { appendLegacyAcousticFormat(value, decimals) }
}

/** Appends the stable acoustic value without allocating an intermediate String per field. */
private fun StringBuilder.appendLegacyAcousticFormat(value: Double, decimals: Int) {
    if (!value.isFinite() || decimals !in 3..4) {
        append(legacyAcousticFormatSlow(value, decimals))
        return
    }

    val unitsPerWhole = if (decimals == 3) 1_000L else 10_000L
    val scaled = abs(value) * unitsPerWhole
    if (!scaled.isFinite() || scaled >= Long.MAX_VALUE.toDouble() - 1.0) {
        append(legacyAcousticFormatSlow(value, decimals))
        return
    }

    val lower = floor(scaled)
    val fraction = scaled - lower
    if (abs(fraction - 0.5) <= 0.0000001) {
        append(legacyAcousticFormatSlow(value, decimals))
        return
    }

    val rounded = (lower + if (fraction > 0.5) 1.0 else 0.0).toLong()
    if (java.lang.Double.doubleToRawLongBits(value) < 0L) append('-')
    append(rounded / unitsPerWhole)
    append('.')
    var remainingFraction = rounded % unitsPerWhole
    var place = unitsPerWhole / 10
    repeat(decimals) {
        append(('0'.code + (remainingFraction / place).toInt()).toChar())
        remainingFraction %= place
        if (place > 1L) place /= 10
    }
}

private fun legacyAcousticFormatSlow(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)
