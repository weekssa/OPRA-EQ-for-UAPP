package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlackPearlTrackedGainReadTest {
    @Test
    fun trackedGainReadPreservesSignedStoredDeltaWithoutUsbAccess() {
        val transport = CountingTransport()
        val store = FakeGainStore(BlackPearlProtocol.gainDbToRawDelta(2.5))
        val flasher = BlackPearlFlasher(transport, store)

        assertThat(flasher.readTrackedAppliedPlaybackGainDb()).isWithin(1e-9).of(2.5)
        assertThat(transport.readCount).isEqualTo(0)
        assertThat(transport.sendCount).isEqualTo(0)

        store.raw = BlackPearlProtocol.gainDbToRawDelta(-4.25)
        assertThat(flasher.readTrackedAppliedPlaybackGainDb()).isWithin(1e-9).of(-4.25)
        assertThat(transport.readCount).isEqualTo(0)
        assertThat(transport.sendCount).isEqualTo(0)
    }

    private class FakeGainStore(var raw: Int) : BlackPearlGainStateStore {
        override fun readAppliedGainDeltaRaw(): Int = raw

        override fun writeAppliedGainDeltaRaw(rawDelta: Int) {
            raw = rawDelta
        }
    }

    private class CountingTransport : BlackPearlTransport {
        var readCount = 0
        var sendCount = 0

        override suspend fun readActiveSlot(): Byte? {
            readCount += 1
            return null
        }

        override suspend fun readGlobalGainRaw(): Int? {
            readCount += 1
            return null
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            sendCount += 1
            return false
        }
    }
}
