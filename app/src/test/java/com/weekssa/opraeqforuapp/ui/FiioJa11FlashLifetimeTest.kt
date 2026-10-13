package com.weekssa.opraeqforuapp.ui

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.weekssa.opraeqforuapp.createEqLibraryDependencies
import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DeviceControlSource
import com.weekssa.opraeqforuapp.data.dac.FiioJa11SnapshotReader
import com.weekssa.opraeqforuapp.data.dac.FiioJa11SnapshotSource
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FiioJa11FlashLifetimeTest {
    @Test
    fun losingOnlyTheScreenCallerStillCompletesSaveAndPublishesReplacementEq() = runBlocking {
        val dependencies = createEqLibraryDependencies(RuntimeEnvironment.getApplication())
        val repository = dependencies.hardwareRepository
        val session = field(repository, "dacSessionRepository") as DacSessionRepository
        val hid = field(session.fiioJa11Transport, "hid")
        @Suppress("UNCHECKED_CAST")
        val connected = field(hid, "mutableState") as MutableStateFlow<Kt02h20ConnectionState>
        val source = SnapshotSource()
        val transport = SaveBoundaryTransport()
        val flasher = FiioJa11Flasher(transport)
        setField(repository, "fiioJa11Flasher", flasher)
        // Repository status aliases were created before replacement; preserve real store correlation.
        setField(repository, "fiioJa11OperationTrace", flasher.lastOperationTrace)
        setField(repository, "fiioJa11OperationStatus", flasher.operationStatus)
        setField(session, "fiioJa11SnapshotReader", FiioJa11SnapshotReader(source))
        setField(dependencies.fiioJa11ControlRepository, "source", DeviceSource(session))
        val catalog = EmptyCatalog()
        val vm = EqLibraryViewModel.Factory {
            dependencies.copy(
                catalogRepository = catalog,
                syncCoordinator = CatalogSyncCoordinator(catalog, dependencies.managedHeadphonesRepository),
            )
        }.create(EqLibraryViewModel::class.java)
        val store = ViewModelStore().apply { put("same-vm", vm) }
        val subscription = launch { vm.uiState.collect() }
        try {
            setField(hid, "currentSessionGeneration", 1L)
            connected.value = Kt02h20ConnectionState.Connected
            await { repository.fiioJa11SnapshotState.value.freshness == DacStateFreshness.CURRENT }
            await { vm.uiState.value.fiioJa11DeviceState.isCurrentSession }
            val caller = async { vm.flashFiioJa11FromMyDac(profile()) }
            withTimeout(5_000) { transport.saveEntered.await() }

            connected.value = Kt02h20ConnectionState.Disconnected
            setField(hid, "currentSessionGeneration", 0L)
            await { repository.fiioJa11SnapshotState.value.freshness != DacStateFreshness.CURRENT }
            source.misses.set(2) // Observer returns null once; completion requires its existing bounded retry.
            setField(hid, "currentSessionGeneration", 2L)
            connected.value = Kt02h20ConnectionState.Connected
            caller.cancelAndJoin() // The Compose test proves automatic output fallback causes this disposal.
            transport.saveGate.complete(Unit)

            val converged = withTimeoutOrNull(2_000) {
                await {
                    repository.fiioJa11SnapshotState.value.freshness == DacStateFreshness.CURRENT &&
                        repository.fiioJa11SnapshotState.value.bundle?.snapshot?.sessionGeneration == 2L &&
                        flasher.operationStatus.value is FiioJa11OperationStatus.Completed
                }
                true
            } ?: false
            assertWithMessage(
                "Same ViewModel must finish the authorized Flash after its screen caller disappears; " +
                    "status=${flasher.operationStatus.value}, reads=${source.reads.get()}, " +
                    "freshness=${repository.fiioJa11SnapshotState.value.freshness}, " +
                    "eqGeneration=${repository.fiioJa11SnapshotState.value.bundle?.snapshot?.sessionGeneration}, " +
                    "deviceGeneration=${vm.uiState.value.fiioJa11DeviceState.snapshot?.sessionGeneration}, " +
                    "deviceCurrent=${vm.uiState.value.fiioJa11DeviceState.isCurrentSession}, " +
                    "writes=${transport.commands}",
            ).that(converged).isTrue()
            assertThat(source.reads.get()).isEqualTo(4) // Initial + observer + two completion reads, no new policy.
            assertThat(repository.fiioJa11SnapshotState.value.bundle!!.snapshot.sessionGeneration).isEqualTo(2L)
            assertThat(flasher.lastOperationTrace.value!!.outcome).isEqualTo("Success")
            assertThat(transport.commands).containsExactly(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19).inOrder()
            await { vm.uiState.value.fiioJa11DeviceState.snapshot?.sessionGeneration == 2L }
            assertThat(vm.uiState.value.fiioJa11DeviceState.isCurrentSession).isTrue()
            assertThat(caller.isCancelled).isTrue()
        } finally {
            transport.saveGate.complete(Unit)
            subscription.cancelAndJoin()
            store.clear()
            repository.close()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) { shadowOf(Looper.getMainLooper()).idle(); delay(5) }
    }
    private fun field(owner: Any, name: String): Any = requireNotNull(owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner))
    private fun setField(owner: Any, name: String, value: Any) {
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(owner, value)
    }

    private class EmptyCatalog : AppCatalogRepository {
        private val catalog = OpraCatalog(emptyList(), emptyList(), emptyList())
        override val state = MutableStateFlow<CatalogState>(CatalogState.Ready(catalog, Long.MAX_VALUE))
        override suspend fun initialize() = Unit
        override suspend fun refresh() = error("This lifetime fixture must not use the network")
    }

    private class SnapshotSource : FiioJa11SnapshotSource {
        val reads = AtomicInteger()
        val misses = AtomicInteger()
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? {
            reads.incrementAndGet()
            return if (misses.getAndDecrement() > 0) null else FiioJa11Protocol.EqProgram.USER_1
        }
        override suspend fun readBand(index: Int) = FiioJa11Protocol.Band("peak_dip", 1_000.0, 0.0, 1.0)
        override suspend fun readGlobalGainDb() = 0.0
    }

    private class SaveBoundaryTransport : FiioJa11Transport {
        val saveEntered = CompletableDeferred<Unit>()
        val saveGate = CompletableDeferred<Unit>()
        val commands = mutableListOf<Int>()
        override var sessionGeneration = 1L
        override val detachGeneration get() = if (sessionGeneration == 2L) 1L else 0L
        override suspend fun readEqProgram() = FiioJa11Protocol.EqProgram.USER_1
        override suspend fun readBand(index: Int) = FiioJa11Protocol.Band("peak_dip", 1_000.0, 0.0, 1.0)
        override suspend fun readGlobalGainDb() = 0.0
        override suspend fun sendReport(report: ByteArray): Boolean { commands += report[5].toInt() and 0xff; return true }
        override suspend fun saveToFlash(): Boolean {
            sendReport(FiioJa11Protocol.saveToFlashReport())
            saveEntered.complete(Unit)
            saveGate.await()
            sessionGeneration = 2L
            return true
        }
    }

    private class DeviceSource(private val session: DacSessionRepository) : FiioJa11DeviceControlSource {
        override val sessionGeneration get() = session.fiioJa11Transport.sessionGeneration
        override val detachGeneration get() = 0L
        override val connectedDeviceSerial: String? = null
        override val supportedCandidateCount = 1
        override val connectedProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2
        override fun isSessionCurrent(sessionGeneration: Long) = session.isFiioJa11SessionCurrent(sessionGeneration)
        override suspend fun readOutputVolume() = 60
        override suspend fun readSampleRateLabel() = "384 kHz"
        override suspend fun readFirmwareVersion() = "2.20"
        override suspend fun readHeadsetControlEnabled() = true
        override suspend fun readEqProgram() = FiioJa11Protocol.EqProgram.USER_1
        override suspend fun readUacMode() = FiioJa11Protocol.UacMode.UAC_2
        override suspend fun awaitExpectedReplacementSession(previousGeneration: Long, previousDetachGeneration: Long, previousSerial: String?, timeoutMillis: Long) = false
        override suspend fun writeOutputVolume(level: Int): Boolean = error("No DEVICE mutation in fixture")
        override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean = error("No DEVICE mutation in fixture")
        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean = error("No DEVICE mutation in fixture")
        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean = error("No DEVICE mutation in fixture")
    }

    private fun profile() = OpraEqProfile(
        "same-profile", "same-headphone", null, null, null, "parametric_eq", 0.0,
        List(5) { OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null) },
    )
}
