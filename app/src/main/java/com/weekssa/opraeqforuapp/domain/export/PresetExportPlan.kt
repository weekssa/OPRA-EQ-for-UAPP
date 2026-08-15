package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import java.security.MessageDigest

data class PresetExportCandidate(
    val profileId: String,
    val productId: String,
    val manufacturerName: String,
    val modelName: String,
    val relativeDirectory: String,
    val fileName: String,
    val xml: String,
    val generatedFingerprint: String,
    val contentHash: String,
)

data class PresetExportPlan(
    val candidates: List<PresetExportCandidate>,
    val duplicateConflicts: List<PresetExportCandidate>,
)

fun buildPresetExportPlan(headphones: List<ManagedHeadphoneRecord>): PresetExportPlan {
    val candidates = headphones.flatMap { headphone ->
        headphone.profiles.mapNotNull { profile ->
            if (!profile.selected) return@mapNotNull null
            val presetName = profile.generatedPresetName ?: return@mapNotNull null
            val xml = profile.generatedXml ?: return@mapNotNull null
            val fingerprint = profile.generatedFromFingerprint ?: return@mapNotNull null
            val manufacturer = safeSharedPathSegment(headphone.vendorName)
            val model = safeSharedPathSegment(headphone.productName)
            PresetExportCandidate(
                profileId = profile.profileId,
                productId = headphone.productId,
                manufacturerName = manufacturer,
                modelName = model,
                relativeDirectory = "$manufacturer/$model",
                fileName = "$presetName.xml",
                xml = xml,
                generatedFingerprint = fingerprint,
                contentHash = sha256(xml.toByteArray(Charsets.ISO_8859_1)),
            )
        }
    }

    val duplicates = candidates
        .groupBy { it.relativeDirectory to it.fileName }
        .values
        .filter { it.size > 1 }
        .flatten()
        .toSet()

    return PresetExportPlan(
        candidates = candidates.filterNot(duplicates::contains),
        duplicateConflicts = duplicates.sortedWith(compareBy({ it.relativeDirectory }, { it.fileName }, { it.profileId })),
    )
}

fun safeSharedPathSegment(value: String): String {
    val trimmed = value.trim()
    require(trimmed.isNotEmpty()) { "OPRA folder segment must not be blank." }
    return trimmed
        .replace(Regex("[\\\\/]"), "-")
        .replace(Regex("[\\u0000-\\u001f\\u007f]"), "-")
        .trim(' ', '.')
        .ifEmpty { throw IllegalArgumentException("OPRA folder segment becomes empty after filesystem sanitization.") }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
