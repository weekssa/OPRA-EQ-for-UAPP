package com.weekssa.opraeqforuapp.domain.ew300

data class Ew300QualificationExport(
    val candidateSourceSha: String,
    val readOnlyReport: Ew300CapabilityReport,
    val persistenceResult: Ew300PersistenceQualificationResult?,
) {
    fun toReadableText(): String = buildString {
        append(readOnlyReport.toReadableText().trimEnd())
        appendLine()
        appendLine()
        appendLine("Signed candidate source: $candidateSourceSha")
        appendLine("Persistence qualification: ${persistenceStatus()}")
        persistenceMessage()?.let { appendLine(it) }
    }

    fun toJson(): String = buildString {
        append("{\"candidateSourceSha\":\"${candidateSourceSha.escape()}\",")
        append("\"readOnlyReport\":${readOnlyReport.toJson()},")
        append("\"persistenceQualification\":{")
        append("\"status\":\"${persistenceStatus()}\",")
        append("\"message\":")
        val message = persistenceMessage()
        if (message == null) append("null") else append("\"${message.escape()}\"")
        append("}}")
    }

    private fun persistenceStatus(): String = when (persistenceResult) {
        is Ew300PersistenceQualificationResult.AwaitingPowerCycle -> "AWAITING_POWER_CYCLE"
        is Ew300PersistenceQualificationResult.Verified -> "VERIFIED"
        is Ew300PersistenceQualificationResult.NotPersistent -> "NOT_PERSISTENT"
        is Ew300PersistenceQualificationResult.Failed -> "FAILED"
        null -> "NOT_STARTED"
    }

    private fun persistenceMessage(): String? = when (val result = persistenceResult) {
        is Ew300PersistenceQualificationResult.AwaitingPowerCycle -> result.message
        is Ew300PersistenceQualificationResult.Verified -> result.message
        is Ew300PersistenceQualificationResult.NotPersistent -> result.message
        is Ew300PersistenceQualificationResult.Failed -> result.message
        null -> null
    }

    private fun String.escape(): String = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
