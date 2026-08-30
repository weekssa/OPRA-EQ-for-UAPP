package com.weekssa.opraeqforuapp.domain.library

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Locale

object EqFingerprint {
    fun acoustic(
        preampGainDb: Double?,
        filters: List<EqFilter>,
    ): String {
        val normalizedFilters = filters
            .map(::normalizeFilter)
            .sorted()

        val canonical = buildString {
            append("preamp=")
            append(format(preampGainDb, 2))
            append(';')
            normalizedFilters.forEach { filter ->
                append(filter)
                append(';')
            }
        }
        return sha256(canonical)
    }

    /**
     * Original v0.3 headphone lineage algorithm. Keep this byte-for-byte compatible so existing
     * headphone correction IDs never change merely because scope/purpose metadata was added.
     */
    fun lineage(
        headphone: HeadphoneIdentity,
        creator: String?,
        target: EqTarget,
        tuningLabel: String?,
    ): String {
        val canonical = listOf(
            headphone.normalizedKey,
            normalizeText(creator),
            normalizeText(target.name),
            target.kind.name,
            normalizeText(tuningLabel),
        ).joinToString("|")
        return sha256(canonical)
    }

    fun lineage(
        scope: EqProfileScope,
        purpose: EqPresetPurpose,
        headphone: HeadphoneIdentity?,
        creator: String?,
        target: EqTarget,
        tuningLabel: String?,
    ): String {
        if (
            scope == EqProfileScope.HEADPHONE &&
            purpose == EqPresetPurpose.CORRECTION_TUNING &&
            headphone != null
        ) {
            return lineage(headphone, creator, target, tuningLabel)
        }

        val canonical = listOf(
            "scope=${scope.name}",
            "purpose=${purpose.name}",
            headphone?.normalizedKey.orEmpty(),
            normalizeText(creator),
            normalizeText(target.name),
            target.kind.name,
            normalizeText(tuningLabel),
        ).joinToString("|")
        return sha256(canonical)
    }

    fun revisionId(lineageFingerprint: String, acousticFingerprint: String): String =
        sha256("$lineageFingerprint|$acousticFingerprint")

    private fun normalizeFilter(filter: EqFilter): String = listOf(
        filter.type.name,
        format(filter.frequencyHz, 2),
        format(filter.gainDb, 2),
        format(filter.q, 3),
        format(filter.slope, 3),
    ).joinToString("|")

    private fun format(value: Double?, scale: Int): String = value?.let {
        BigDecimal.valueOf(it)
            .setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } ?: "null"

    private fun normalizeText(value: String?): String =
        value.orEmpty().lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
