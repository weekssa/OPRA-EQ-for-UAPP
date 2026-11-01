package com.weekssa.opraeqforuapp.data.kt02h20

/**
 * One-line description of a digital-filter probe exchange.
 *
 * The probe is a read. This record lets a failed discovery be classified as "session/transfer did
 * not complete", "request sent, no reply", "reply received but rejected", or "valid", without a USB
 * bus capture. It contains protocol bytes and session counters only. An unrelated queued HID report
 * could in principle be logged; nothing is written to disk or sent anywhere.
 */
internal object Ja11FilterProbeDiagnostics {
    fun describe(
        generation: Long,
        detachGeneration: Long,
        requestHex: String,
        response: ByteArray?,
        sessionStable: Boolean,
        sessionCurrentBeforeSend: Boolean,
        elapsedMillis: Long,
    ): String = buildString {
        append("Ja11FilterProbe generation=").append(generation)
        append(" detach=").append(detachGeneration)
        append(" sessionCurrentBeforeSend=").append(sessionCurrentBeforeSend)
        append(" request=").append(requestHex)
        append(" elapsedMs=").append(elapsedMillis)
        append(" responseLength=").append(response?.size ?: -1)
        append(" response=").append(response?.let(::hexOrEmpty) ?: "none")
        append(" sessionStable=").append(sessionStable)
    }

    private fun hexOrEmpty(bytes: ByteArray): String =
        bytes.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
