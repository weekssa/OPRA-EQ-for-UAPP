package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Android-free transaction planning for the EW300's qualified volatile band fields.
 *
 * This model deliberately describes bytes and ordering only. It does not claim that the bytes
 * represent production EQ values, and it has no operation for save, commit, clear, reset, slot,
 * or global gain. A caller must execute every step in order and must finish with the restore read.
 */
object Ew300VolatileTransaction {
    enum class Step {
        BASELINE_READ,
        TEMPORARY_WRITE,
        TEMPORARY_READBACK,
        RESTORE_WRITE,
        RESTORE_READBACK,
    }

    /** A four-byte field from one of the physically qualified band registers. */
    class RawField private constructor(
        val address: Int,
        private val rawData: ByteArray,
    ) {
        fun data(): ByteArray = rawData.copyOf()

        override fun equals(other: Any?): Boolean =
            other is RawField && address == other.address && rawData.contentEquals(other.rawData)

        override fun hashCode(): Int = 31 * address + rawData.contentHashCode()

        override fun toString(): String =
            "RawField(address=0x${address.toString(16)}, data=${rawData.joinToString()})"

        companion object {
            fun of(address: Int, data: ByteArray): RawField {
                require(Ew300QualifiedVolatileProtocol.isQualifiedVolatileRegister(address)) {
                    "Only physically qualified EW300 volatile band registers may be used."
                }
                require(data.size == 4) { "An EW300 band field must contain exactly four bytes." }
                return RawField(address, data.copyOf())
            }
        }
    }

    /** Exact stock values required before a volatile transaction can be planned. */
    class StockSnapshot private constructor(
        private val fields: List<RawField>,
    ) {
        fun field(address: Int): RawField = fields.firstOrNull { it.address == address }
            ?: error("EW300 stock snapshot has no qualified field at 0x${address.toString(16)}")

        fun addresses(): List<Int> = fields.map { it.address }

        companion object {
            fun of(fields: List<RawField>): StockSnapshot {
                val expected = Ew300QualifiedVolatileProtocol.qualifiedVolatileRegisterAddresses()
                require(fields.map { it.address } == expected) {
                    "EW300 stock snapshot must contain the qualified registers in address order."
                }
                return StockSnapshot(fields.map { RawField.of(it.address, it.data()) })
            }
        }
    }

    data class PlannedStep(
        val stage: Step,
        val report: ByteArray,
        /** Non-null only for reads whose exact data must be verified. */
        val expectedReadback: ByteArray?,
    ) {
        init {
            require(report.size == Ew300QualifiedVolatileProtocol.REPORT_SIZE)
            expectedReadback?.let { require(it.size == 4) }
        }

        fun reportCopy(): ByteArray = report.copyOf()
        fun expectedReadbackCopy(): ByteArray? = expectedReadback?.copyOf()
    }

    class Plan private constructor(
        val target: RawField,
        val temporary: RawField,
    ) {
        init {
            require(target.address == temporary.address) { "EW300 target and temporary fields must match." }
        }

        /** The restore is mandatory even when the temporary readback fails. */
        val restorationRequired: Boolean = true

        fun steps(): List<PlannedStep> = listOf(
            PlannedStep(
                Step.BASELINE_READ,
                Ew300QualifiedVolatileProtocol.readReport(target.address),
                target.data(),
            ),
            PlannedStep(
                Step.TEMPORARY_WRITE,
                Ew300QualifiedVolatileProtocol.temporaryWriteReport(target.address, temporary.data()),
                null,
            ),
            PlannedStep(
                Step.TEMPORARY_READBACK,
                Ew300QualifiedVolatileProtocol.readReport(target.address),
                temporary.data(),
            ),
            PlannedStep(
                Step.RESTORE_WRITE,
                Ew300QualifiedVolatileProtocol.temporaryWriteReport(target.address, target.data()),
                null,
            ),
            PlannedStep(
                Step.RESTORE_READBACK,
                Ew300QualifiedVolatileProtocol.readReport(target.address),
                target.data(),
            ),
        )

        fun restored(report: ByteArray): Boolean =
            Ew300QualifiedVolatileProtocol.matchesExpectedReadback(target.address, target.data(), report)

        companion object {
            fun forField(stock: StockSnapshot, address: Int, temporaryData: ByteArray): Plan =
                Plan(stock.field(address), RawField.of(address, temporaryData))

            fun forProvisionalFilterType(
                stock: StockSnapshot,
                address: Int,
                type: Ew300QualifiedVolatileProtocol.ProvisionalFilterType,
            ): Plan {
                val original = stock.field(address)
                val temporary = Ew300QualifiedVolatileProtocol.withProvisionalFilterType(original.data(), type)
                return Plan(original, RawField.of(address, temporary))
            }
        }
    }
}
