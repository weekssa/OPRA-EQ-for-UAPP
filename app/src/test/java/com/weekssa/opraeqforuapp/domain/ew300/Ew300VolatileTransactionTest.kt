package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300VolatileTransactionTest {
    @Test
    fun planHasReadWriteReadRestoreReadOrderAndExactPayloads() {
        val stock = stockSnapshot()
        val plan = Ew300VolatileTransaction.Plan.forField(stock, 0x26, bytes(0xF6, 0xFF, 0x64, 0))

        assertEquals(
            listOf(
                Ew300VolatileTransaction.Step.BASELINE_READ,
                Ew300VolatileTransaction.Step.TEMPORARY_WRITE,
                Ew300VolatileTransaction.Step.TEMPORARY_READBACK,
                Ew300VolatileTransaction.Step.RESTORE_WRITE,
                Ew300VolatileTransaction.Step.RESTORE_READBACK,
            ),
            plan.steps().map { it.stage },
        )
        assertArrayEquals(bytes(0x4B, 0x26, 0, 0, 0, 0x52, 0, 0, 0, 0, 0), plan.steps()[0].report)
        assertArrayEquals(bytes(0x4B, 0x26, 0, 0, 0, 0x57, 0, 0xF6, 0xFF, 0x64, 0), plan.steps()[1].report)
        assertArrayEquals(bytes(0x4B, 0x26, 0, 0, 0, 0x57, 0, 0xF5, 0xFF, 0x64, 0), plan.steps()[3].report)
        assertTrue(plan.restorationRequired)
        assertTrue(plan.restored(bytes(0x4B, 0x26, 0, 0, 0, 0x52, 0, 0xF5, 0xFF, 0x64, 0)))
        assertFalse(plan.restored(bytes(0x4B, 0x26, 0, 0, 0, 0x52, 0, 0xF6, 0xFF, 0x64, 0)))
    }

    @Test
    fun provisionalFilterPlanChangesOnlyObservedTypeByte() {
        val stock = stockSnapshot()
        val plan = Ew300VolatileTransaction.Plan.forProvisionalFilterType(
            stock,
            0x27,
            Ew300QualifiedVolatileProtocol.ProvisionalFilterType.HIGH_SHELF,
        )

        assertArrayEquals(bytes(0x20, 0x03, 0x00, 0), plan.target.data())
        assertArrayEquals(bytes(0x20, 0x03, 0x04, 0), plan.temporary.data())
    }

    @Test
    fun stockSnapshotRejectsMissingOrOutOfOrderFields() {
        val fields = (0x26..0x2F).map { address ->
            Ew300VolatileTransaction.RawField.of(address, bytes(0, 0, 0, 0))
        }
        assertEquals((0x26..0x2F).toList(), Ew300VolatileTransaction.StockSnapshot.of(fields).addresses())
        assertFails { Ew300VolatileTransaction.StockSnapshot.of(fields.drop(1)) }
        assertFails { Ew300VolatileTransaction.RawField.of(0x24, bytes(0, 0, 0, 0)) }
    }

    private fun stockSnapshot(): Ew300VolatileTransaction.StockSnapshot =
        Ew300VolatileTransaction.StockSnapshot.of((0x26..0x2F).map { address ->
            val data = when (address) {
                0x26 -> bytes(0xF5, 0xFF, 0x64, 0)
                0x27 -> bytes(0x20, 0x03, 0x00, 0)
                else -> bytes(0, 0, 0, 0)
            }
            Ew300VolatileTransaction.RawField.of(address, data)
        })

    private fun assertFails(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
