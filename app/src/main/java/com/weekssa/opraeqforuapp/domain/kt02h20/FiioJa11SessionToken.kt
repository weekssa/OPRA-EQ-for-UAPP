package com.weekssa.opraeqforuapp.domain.kt02h20

/** Immutable USB identity captured for one multi-command JA11 transaction. */
data class FiioJa11SessionToken(
    val deviceFingerprintKey: String,
    val usbProductId: Int,
    val sessionGeneration: Long,
    val detachGeneration: Long,
    val deviceSerialIdentity: String? = fiioJa11SerialIdentity(deviceFingerprintKey),
) {
    init {
        require(deviceFingerprintKey.isNotBlank())
        require(FiioJa11Protocol.supportsProductId(usbProductId))
        require(sessionGeneration > 0L)
        require(detachGeneration >= 0L)
    }
}
