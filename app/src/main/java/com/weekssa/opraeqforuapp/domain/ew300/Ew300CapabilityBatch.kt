package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Declarative, allowlisted EW300 capability checks. The default plan is read-only; mutating
 * cases must be explicitly supplied by a future signed diagnostic build and are never inferred
 * from a button tap or a guessed register.
 */
enum class Ew300CapabilityTestKind {
    READ_ONLY_SNAPSHOT,
    REVERSIBLE_VOLATILE_FIELD,
    PERSISTENCE_CANDIDATE,
}

data class Ew300CapabilityTestCase(
    val id: String,
    val title: String,
    val purpose: String,
    val kind: Ew300CapabilityTestKind,
    val register: Int? = null,
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(purpose.isNotBlank())
        require(register == null || register in 0..0xFF)
    }
}

data class Ew300CapabilityCaseResult(
    val caseId: String,
    val status: Status,
    val message: String,
    val registerValues: Map<Int, ByteArray> = emptyMap(),
) {
    enum class Status { PASS, FAIL, INCONCLUSIVE, SKIPPED }
}

data class Ew300CapabilityReport(
    val planVersion: String,
    val deviceFingerprintKey: String,
    val cases: List<Ew300CapabilityCaseResult>,
    val stateKnown: Boolean,
    val stoppedAfterFailure: Boolean,
) {
    val status: Ew300CapabilityCaseResult.Status = when {
        cases.any { it.status == Ew300CapabilityCaseResult.Status.FAIL } -> Ew300CapabilityCaseResult.Status.FAIL
        cases.any { it.status == Ew300CapabilityCaseResult.Status.INCONCLUSIVE } -> Ew300CapabilityCaseResult.Status.INCONCLUSIVE
        cases.any { it.status == Ew300CapabilityCaseResult.Status.SKIPPED } -> Ew300CapabilityCaseResult.Status.INCONCLUSIVE
        else -> Ew300CapabilityCaseResult.Status.PASS
    }

    fun toReadableText(): String = buildString {
        appendLine("EQ Library EW300 capability report")
        appendLine("Plan: $planVersion")
        appendLine("Result: $status")
        appendLine("Device fingerprint: ${deviceFingerprintKey.redactedForEw300Report()}")
        appendLine("Device state known after run: $stateKnown")
        appendLine("Stopped after first failure: $stoppedAfterFailure")
        cases.forEach { result ->
            appendLine()
            appendLine("${result.caseId}: ${result.status}")
            appendLine(result.message.redactedForEw300Report(deviceFingerprintKey))
            result.registerValues.toSortedMap().forEach { (register, value) ->
                appendLine("register 0x${register.toString(16).padStart(2, '0')}: ${value.hex()}")
            }
        }
    }

    /** Small dependency-free JSON export for Android's share-sheet/report path. */
    fun toJson(): String = buildString {
        append("{\"planVersion\":\"${planVersion.jsonEscape()}\",")
        append("\"deviceFingerprintKey\":\"${requireNotNull(deviceFingerprintKey.redactedForEw300Report()).jsonEscape()}\",")
        append("\"status\":\"$status\",")
        append("\"stateKnown\":$stateKnown,")
        append("\"stoppedAfterFailure\":$stoppedAfterFailure,")
        append("\"cases\":[")
        cases.forEachIndexed { index, result ->
            if (index > 0) append(',')
            append("{\"id\":\"${result.caseId.jsonEscape()}\",")
            append("\"status\":\"${result.status}\",")
            append("\"message\":\"${requireNotNull(result.message.redactedForEw300Report(deviceFingerprintKey)).jsonEscape()}\",")
            append("\"registerValues\":{")
            result.registerValues.toSortedMap().entries.forEachIndexed { valueIndex, (register, value) ->
                if (valueIndex > 0) append(',')
                append("\"0x${register.toString(16).padStart(2, '0')}\":\"${value.hex()}\"")
            }
            append("}}")
        }
        append("]}")
    }

    private fun String.jsonEscape(): String = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun ByteArray.hex(): String = joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
}

class Ew300CapabilityBatch(
    private val transport: Ew300Transport,
) {
    suspend fun run(
        cases: List<Ew300CapabilityTestCase> = defaultReadOnlyPlan(),
    ): Ew300CapabilityReport {
        val fingerprint = transport.deviceFingerprintKey
            ?: return Ew300CapabilityReport(
                planVersion = PLAN_VERSION,
                deviceFingerprintKey = "unavailable",
                cases = cases.map { test ->
                    Ew300CapabilityCaseResult(
                        caseId = test.id,
                        status = Ew300CapabilityCaseResult.Status.INCONCLUSIVE,
                        message = "The exact EW300 fingerprint was unavailable; no operation was sent.",
                    )
                },
                stateKnown = false,
                stoppedAfterFailure = true,
            )

        val results = mutableListOf<Ew300CapabilityCaseResult>()
        var stateKnown = true
        var stopped = false
        for (test in cases) {
            if (stopped) {
                results += Ew300CapabilityCaseResult(
                    caseId = test.id,
                    status = Ew300CapabilityCaseResult.Status.SKIPPED,
                    message = "Skipped because an earlier allowlisted check failed.",
                )
                continue
            }
            val result = when (test.kind) {
                Ew300CapabilityTestKind.READ_ONLY_SNAPSHOT -> readSnapshot(test)
                Ew300CapabilityTestKind.REVERSIBLE_VOLATILE_FIELD -> Ew300CapabilityCaseResult(
                    test.id,
                    Ew300CapabilityCaseResult.Status.SKIPPED,
                    "This mutating case requires an explicit signed diagnostic plan and is not part of the default read-only batch.",
                )
                Ew300CapabilityTestKind.PERSISTENCE_CANDIDATE -> Ew300CapabilityCaseResult(
                    test.id,
                    Ew300CapabilityCaseResult.Status.SKIPPED,
                    "Persistence is intentionally not probed by the default utility; it requires a separately approved exact-device plan.",
                )
            }
            results += result
            if (result.status == Ew300CapabilityCaseResult.Status.FAIL) {
                stateKnown = false
                stopped = true
            }
        }
        return Ew300CapabilityReport(PLAN_VERSION, fingerprint, results, stateKnown, stopped)
    }

    private suspend fun readSnapshot(test: Ew300CapabilityTestCase): Ew300CapabilityCaseResult {
        val registers = test.register?.let(::listOf) ?: snapshotRegisters()
        val values = linkedMapOf<Int, ByteArray>()
        for (register in registers) {
            val value = transport.readRegister(register)
            if (value == null || value.size != 4) {
                return Ew300CapabilityCaseResult(
                    caseId = test.id,
                    status = Ew300CapabilityCaseResult.Status.FAIL,
                    message = "Read-only register snapshot failed at 0x${register.toString(16)}; no later checks were sent.",
                    registerValues = values,
                )
            }
            values[register] = value.copyOf()
        }
        return Ew300CapabilityCaseResult(
            caseId = test.id,
            status = Ew300CapabilityCaseResult.Status.PASS,
            message = "Read-only EW300 state was captured with strict four-byte validation.",
            registerValues = values,
        )
    }

    companion object {
        const val PLAN_VERSION = "ew300-capability-readonly-v1"

        fun defaultReadOnlyPlan(): List<Ew300CapabilityTestCase> = listOf(
            Ew300CapabilityTestCase(
                id = "identity-and-eq-snapshot",
                title = "Read-only EW300 state",
                purpose = "Capture the complete current EQ and gain state before any capability decision.",
                kind = Ew300CapabilityTestKind.READ_ONLY_SNAPSHOT,
            ),
        )

        fun snapshotRegisters(): List<Int> = buildList {
            add(0x24)
            repeat(Ew300Protocol.BAND_COUNT * 2) { add(Ew300Protocol.FIRST_BAND_REGISTER + it) }
            add(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        }
    }
}
