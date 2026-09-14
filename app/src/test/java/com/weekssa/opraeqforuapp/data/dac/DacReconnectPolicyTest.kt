package com.weekssa.opraeqforuapp.data.dac

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DacReconnectPolicyTest {
    @Test
    fun `initial detection never opens a session automatically`() {
        val policy = DacReconnectPolicy()

        assertFalse(
            policy.shouldReconnect(
                isPresent = true,
                isConnected = false,
                isDisconnected = true,
            ),
        )
    }

    @Test
    fun `successful session arms physical reattach for automatic reconnect`() {
        val policy = DacReconnectPolicy()

        assertFalse(
            policy.shouldReconnect(
                isPresent = true,
                isConnected = true,
                isDisconnected = false,
            ),
        )
        assertFalse(
            policy.shouldReconnect(
                isPresent = false,
                isConnected = false,
                isDisconnected = true,
            ),
        )
        assertTrue(
            policy.shouldReconnect(
                isPresent = true,
                isConnected = false,
                isDisconnected = true,
            ),
        )
    }

    @Test
    fun `connecting or error state never loops automatic reconnect`() {
        val policy = DacReconnectPolicy()

        policy.shouldReconnect(
            isPresent = true,
            isConnected = true,
            isDisconnected = false,
        )

        assertFalse(
            policy.shouldReconnect(
                isPresent = true,
                isConnected = false,
                isDisconnected = false,
            ),
        )
    }
}
