package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import java.nio.charset.Charset
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
    val mimeType: String = "application/xml",
    val charsetName: String = "ISO-8859-1",
    val deviceName: String = "UAPP",
    val transformation: String = "Exact",
    val fidelity: DevicePresetFidelity = DevicePresetFidelity.EXACT,
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
    return finalizePlan(candidates)
}

fun buildEqLibraryExportPlan(
    headphones: List<ManagedHeadphoneRecord>,
    device: ExportDevice? = null,
): PresetExportPlan {
    val requestedTextDevices = when (device) {
        null -> ExportDevice.selectableOutputs.filterNot { it == ExportDevice.UAPP }
        ExportDevice.UAPP -> emptyList()
        else -> listOf(device)
    }
    val includeUapp = device == null || device == ExportDevice.UAPP

    val candidates = buildList {
        headphones.forEach { headphone ->
            val manufacturer = safeSharedPathSegment(headphone.vendorName)
            val model = safeSharedPathSegment(headphone.productName)
            headphone.profiles.forEach { profile ->
                if (!profile.selected) return@forEach
                val presetName = profile.generatedPresetName ?: return@forEach
                val fingerprint = profile.generatedFromFingerprint ?: profile.fingerprint

                if (includeUapp) {
                    profile.generatedXml?.let { xml ->
                        val capabilities = requireNotNull(ExportDevice.UAPP.eqCapabilities)
                        val fidelity = determineDeviceFidelity(profile.lastKnownProfile, capabilities)
                        val transformation = when (fidelity) {
                            DevicePresetFidelity.EXACT ->
                                "Source EQ preserved in ToneBoosters/UAPP XML."
                            DevicePresetFidelity.OPTIMIZED ->
                                "EQ Library optimized conversion for ToneBoosters/UAPP XML using the current device capability profile."
                        }
                        add(
                            PresetExportCandidate(
                                profileId = profile.profileId,
                                productId = headphone.productId,
                                manufacturerName = manufacturer,
                                modelName = model,
                                relativeDirectory = "${ExportDevice.UAPP.folderName}/$manufacturer/$model",
                                fileName = "$presetName.${ExportDevice.UAPP.extension}",
                                xml = xml,
                                generatedFingerprint = "$fingerprint:${ExportDevice.UAPP.name}",
                                contentHash = sha256(xml.toByteArray(Charsets.ISO_8859_1)),
                                mimeType = ExportDevice.UAPP.mimeType,
                                charsetName = Charsets.ISO_8859_1.name(),
                                deviceName = ExportDevice.UAPP.folderName,
                                transformation = transformation,
                                fidelity = fidelity,
                            ),
                        )
                    }
                }

                requestedTextDevices
                    .mapNotNull { requestedDevice ->
                        buildFileExportDeviceVariant(profile.lastKnownProfile, requestedDevice)
                    }
                    .forEach { variant ->
                        val charset = Charsets.UTF_8
                        add(
                            PresetExportCandidate(
                                profileId = profile.profileId,
                                productId = headphone.productId,
                                manufacturerName = manufacturer,
                                modelName = model,
                                relativeDirectory = "${variant.device.folderName}/$manufacturer/$model",
                                fileName = "$presetName.${variant.device.extension}",
                                xml = variant.content,
                                generatedFingerprint = "$fingerprint:${variant.device.name}",
                                contentHash = sha256(variant.content.toByteArray(charset)),
                                mimeType = variant.device.mimeType,
                                charsetName = charset.name(),
                                deviceName = variant.device.folderName,
                                transformation = variant.transformation,
                                fidelity = variant.fidelity,
                            ),
                        )
                    }
            }
        }
    }
    return finalizePlan(candidates)
}

private fun finalizePlan(candidates: List<PresetExportCandidate>): PresetExportPlan {
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

fun presetBytes(candidate: PresetExportCandidate): ByteArray =
    candidate.xml.toByteArray(Charset.forName(candidate.charsetName))

fun safeSharedPathSegment(value: String): String {
    val trimmed = value.trim()
    require(trimmed.isNotEmpty()) { "EQ Library folder segment must not be blank." }
    return trimmed
        .replace(Regex("[\\\\/]"), "-")
        .replace(Regex("[\\u0000-\\u001f\\u007f]"), "-")
        .trim(' ', '.')
        .ifEmpty { throw IllegalArgumentException("EQ Library folder segment becomes empty after filesystem sanitization.") }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
