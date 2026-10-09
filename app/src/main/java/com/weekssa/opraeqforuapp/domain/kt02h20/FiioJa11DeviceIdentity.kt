package com.weekssa.opraeqforuapp.domain.kt02h20

/**
 * Returns only a usable USB serial from a JA11 fingerprint. Other descriptor fields, port paths,
 * VID/PID and interface numbers are not physical-unit identity and must not be compared as such.
 */
internal fun fiioJa11SerialIdentity(fingerprintKey: String?): String? {
    val serialFields = fingerprintKey?.split('|')
        ?.filter { it.substringBefore('=') == "serial" }
        ?: return null
    if (serialFields.size != 1) return null
    return serialFields.single().substringAfter('=', "").trim().takeIf(String::isNotBlank)
}

internal enum class FiioJa11RestartContinuity {
    SAME_DEVICE_SERIAL_MATCHED,
    SOLE_RETURNING_JA11_STATE_VERIFIED,
}

/**
 * A stable matching serial adds unit-continuity evidence. Without one, exactly-one-candidate
 * operation continuity can verify returned state but cannot prove that the same physical unit came
 * back.
 */
internal fun fiioJa11RestartContinuity(
    originalFingerprintKey: String?,
    replacementFingerprintKey: String?,
    supportedCandidateCount: Int,
): FiioJa11RestartContinuity? = fiioJa11RestartContinuityFromSerials(
    originalSerialIdentity = fiioJa11SerialIdentity(originalFingerprintKey),
    replacementSerialIdentity = fiioJa11SerialIdentity(replacementFingerprintKey),
    supportedCandidateCount = supportedCandidateCount,
)

internal fun fiioJa11RestartContinuityFromSerials(
    originalSerialIdentity: String?,
    replacementSerialIdentity: String?,
    supportedCandidateCount: Int,
): FiioJa11RestartContinuity? {
    if (supportedCandidateCount != 1) return null
    if (originalSerialIdentity != null && replacementSerialIdentity != null &&
        originalSerialIdentity != replacementSerialIdentity
    ) return null
    return if (originalSerialIdentity != null && originalSerialIdentity == replacementSerialIdentity) {
        FiioJa11RestartContinuity.SAME_DEVICE_SERIAL_MATCHED
    } else {
        FiioJa11RestartContinuity.SOLE_RETURNING_JA11_STATE_VERIFIED
    }
}
