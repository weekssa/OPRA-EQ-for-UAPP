package com.weekssa.opraeqforuapp.data.kt02h20

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import org.junit.Test

class FiioJa11FilterProbeCacheTest {
    @Test
    fun aFreshCacheIsNotCachedForAnySession() {
        val cache = FiioJa11FilterProbeCache()
        assertThat(cache.isCachedFor(1L)).isFalse()
        assertThat(cache.isCachedFor(0L)).isFalse()
    }

    @Test
    fun aSupportedProbeIsReusedOnlyWithinItsSession() {
        val cache = FiioJa11FilterProbeCache()
        cache.record(3L, FiioJa11Protocol.DigitalFilterMode.SLOW_LL)
        assertThat(cache.isCachedFor(3L)).isTrue()
        assertThat(cache.value()).isEqualTo(FiioJa11Protocol.DigitalFilterMode.SLOW_LL)
        assertThat(cache.isCachedFor(4L)).isFalse()
    }

    @Test
    fun anUnsupportedProbeIsCachedSoNoRepeatedTimeoutOccurs() {
        val cache = FiioJa11FilterProbeCache()
        cache.record(2L, null)
        assertThat(cache.isCachedFor(2L)).isTrue()
        assertThat(cache.value()).isNull()
    }

    @Test
    fun aWriteInvalidatesTheProbeUntilItIsReRead() {
        val cache = FiioJa11FilterProbeCache()
        cache.record(5L, FiioJa11Protocol.DigitalFilterMode.FAST_PC)
        cache.invalidate()
        assertThat(cache.isCachedFor(5L)).isFalse()
        cache.record(5L, FiioJa11Protocol.DigitalFilterMode.NON_OS)
        assertThat(cache.value()).isEqualTo(FiioJa11Protocol.DigitalFilterMode.NON_OS)
    }

    @Test
    fun aSessionWithoutAnIdentityNeverCaches() {
        val cache = FiioJa11FilterProbeCache()
        cache.record(0L, FiioJa11Protocol.DigitalFilterMode.FAST_LL)
        assertThat(cache.isCachedFor(0L)).isFalse()
    }
}
