package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300ReconnectGateTest {
    @Test
    fun mutationBlocksAutomaticReconnectUntilSaveIsSent() {
        val gate = Ew300ReconnectGate()

        assertTrue(gate.canAutomaticReconnect())
        gate.beginMutation()
        assertFalse(gate.canAutomaticReconnect())
        gate.markSaveSent()
        assertTrue(gate.canAutomaticReconnect())
        gate.endMutation()
        assertTrue(gate.canAutomaticReconnect())
    }

    @Test
    fun saveBeforeMutationIsIgnoredAndNestedMutationDoesNotReopenEarly() {
        val gate = Ew300ReconnectGate()

        gate.markSaveSent()
        gate.beginMutation()
        gate.beginMutation()
        assertFalse(gate.canAutomaticReconnect())
        gate.markSaveSent()
        gate.endMutation()
        assertTrue(gate.canAutomaticReconnect())
        gate.endMutation()
        assertTrue(gate.canAutomaticReconnect())
    }

    @Test
    fun failedMutationStaysBlockedUntilExplicitManualConnect() {
        val gate = Ew300ReconnectGate()

        gate.beginMutation()
        gate.endMutation()

        assertFalse(gate.canAutomaticReconnect())
        gate.beginManualConnect()
        assertTrue(gate.canAutomaticReconnect())
    }

    @Test
    fun queuedAutomaticReconnectRechecksGateBeforeLaunchingConnection() {
        val gate = Ew300ReconnectGate()
        var connectionCount = 0
        val queuedConnectAutomatically = {
            if (gate.canAutomaticReconnect()) connectionCount++
        }

        gate.beginMutation()
        queuedConnectAutomatically()
        assertThat(connectionCount).isEqualTo(0)

        gate.markSaveSent()
        queuedConnectAutomatically()
        assertThat(connectionCount).isEqualTo(1)
    }
}
