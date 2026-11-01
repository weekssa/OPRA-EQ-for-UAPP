package com.weekssa.opraeqforuapp.data.kt02h20

/**
 * One-line, non-sensitive description of a digital-filter probe exchange.
 *
 * The probe itself is a read; this record exists so a failed discovery can be classified as
 * "never sent", "no response", "response rejected", or "valid" without a USB bus capture. It never
 * includes device identifiers or user data.
 */
internal object Ja11FilterProbeDiagnostics {
    fun describe(
        generation: Long,
        detachGeneration: Long,
        requestHex: String,
        response: ByteArray?,
        sessionStable: Boolean,
    ): String = buildString {
        append("Ja11FilterProbe generation=").append(generation)
        append(" detach=").append(detachGeneration)
        append(" request=").append(requestHex)
        append(" responseLength=").append(response?.size ?: -1)
        append(" response=").append(response?.let(::hexOrEmpty) ?: "none")
        append(" sessionStable=").append(sessionStable)
    }

    private fun hexOrEmpty(bytes: ByteArray): String =
        bytes.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
