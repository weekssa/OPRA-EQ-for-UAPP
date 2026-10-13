package com.weekssa.opraeqforuapp.data.hardware

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.ConvergenceProviderTestApplication
import com.weekssa.opraeqforuapp.createEqLibraryDependencies
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.dac.FiioJa11SnapshotReader
import com.weekssa.opraeqforuapp.data.dac.FiioJa11SnapshotSource
import com.weekssa.opraeqforuapp.data.kt02h20.Ja11UsbConnectionShadow
import com.weekssa.opraeqforuapp.data.kt02h20.Ja11UsbManagerShadow
import com.weekssa.opraeqforuapp.data.kt02h20.ja11TestDevice
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceOwner
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergencePhase
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceReadContext
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = ConvergenceProviderTestApplication::class,
    shadows = [Ja11UsbManagerShadow::class, Ja11UsbConnectionShadow::class])
class Ja11ConvergenceWiringTest {
    @Test
    fun realRepositoryIdentifiesConnectedObserverAndFlashCompletionWithoutFixingTheirPolicies() = runBlocking {
        val application = RuntimeEnvironment.getApplication() as ConvergenceProviderTestApplication
        val recorder = application.convergenceDiagnostics
        val manager = application.getSystemService(Context.USB_SERVICE) as UsbManager
        val shadow = Shadow.extract<Ja11UsbManagerShadow>(manager)
        Ja11UsbManagerShadow.openAllowed = true
        Ja11UsbConnectionShadow.claimAllowed = true
        val repository = createEqLibraryDependencies(application).hardwareRepository
        val session = field(repository, "dacSessionRepository") as DacSessionRepository
        val source = ControlledSource()
        setField(session, "fiioJa11SnapshotReader", FiioJa11SnapshotReader(source))
        shadow.addOrUpdateUsbDevice(ja11TestDevice("/dev/bus/usb/001/001", 0x0102), true)
        try {
            repository.connectFiioJa11()
            await { recorder.snapshot().records.any {
                it.event.owner == Ja11ConvergenceOwner.CONNECTED_OBSERVER && it.event.phase == Ja11ConvergencePhase.REFRESH_ENDED
            } }
            assertThat(repository.fiioJa11SnapshotState.value.bundle).isNull()
            assertThat(source.reads.get()).isEqualTo(1)
            assertThat(recorder.snapshot().records.map { it.event.phase }).contains(Ja11ConvergencePhase.PROGRAM_MISSING)

            source.misses.set(1)
            // Unsupported source stops before any transfer; only the real completion/read path runs.
            val result = repository.flashFiioJa11(OpraEqProfile(
                "fixture", "fixture", null, null, null, "parametric_eq", 0.0,
                listOf(OpraBand("unsupported", 1_000.0, 0.0, 1.0, null)),
            ))
            assertThat(result).isInstanceOf(Kt02h20FlashResult.NotSuitable::class.java)
            val events = recorder.snapshot().records.map { it.event }
                .filter { it.owner == Ja11ConvergenceOwner.FLASH_COMPLETION }
            assertThat(events.filter { it.phase == Ja11ConvergencePhase.ATTEMPT_STARTED }.map { it.attempt })
                .containsExactly(1, 2).inOrder()
            assertThat(events.map { it.phase }).contains(Ja11ConvergencePhase.PUBLISHED)
            assertThat(repository.fiioJa11SnapshotState.value.bundle).isNotNull()
            assertThat(events.last().phase).isEqualTo(Ja11ConvergencePhase.FLASH_RETURNED)
        } finally { repository.close() }
    }

    @Test
    fun closingRepositoryCancelsBlockedAcquisitionWithoutPublicationOrRetry() = runBlocking {
        val application = RuntimeEnvironment.getApplication() as ConvergenceProviderTestApplication
        val manager = application.getSystemService(Context.USB_SERVICE) as UsbManager
        val shadow = Shadow.extract<Ja11UsbManagerShadow>(manager)
        val repository = createEqLibraryDependencies(application).hardwareRepository
        val session = field(repository, "dacSessionRepository") as DacSessionRepository
        val entered = CompletableDeferred<Unit>()
        val blocked = CompletableDeferred<Unit>()
        val source = ControlledSource(beforeProgram = { entered.complete(Unit); blocked.await() })
        setField(session, "fiioJa11SnapshotReader", FiioJa11SnapshotReader(source))
        shadow.addOrUpdateUsbDevice(ja11TestDevice("/dev/bus/usb/001/001", 0x0102), true)
        try {
            repository.connectFiioJa11()
            withTimeout(3_000) { entered.await() }
            repository.close()
            await { application.convergenceDiagnostics.snapshot().records.any { it.event.phase == Ja11ConvergencePhase.CANCELLED } }
            assertThat(repository.fiioJa11SnapshotState.value.bundle).isNull()
            assertThat(source.reads.get()).isEqualTo(1)
            assertThat(application.convergenceDiagnostics.snapshot().records.none { it.event.phase == Ja11ConvergencePhase.PUBLISHED }).isTrue()
        } finally { repository.close() }
    }

    @Test
    fun actualSessionRejectsCompletedOldGenerationReadAfterSimulatedReplacement() = runBlocking {
        val application = RuntimeEnvironment.getApplication() as ConvergenceProviderTestApplication
        val recorder = application.convergenceDiagnostics
        val manager = application.getSystemService(Context.USB_SERVICE) as UsbManager
        val shadow = Shadow.extract<Ja11UsbManagerShadow>(manager)
        val repository = createEqLibraryDependencies(application).hardwareRepository
        val session = field(repository, "dacSessionRepository") as DacSessionRepository
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockOnce = AtomicBoolean(true)
        val source = ControlledSource(beforeGain = {
            if (blockOnce.compareAndSet(true, false)) { entered.complete(Unit); release.await() }
        })
        setField(session, "fiioJa11SnapshotReader", FiioJa11SnapshotReader(source))
        val oldDevice = ja11TestDevice("/dev/bus/usb/001/001", 0x0102)
        shadow.addOrUpdateUsbDevice(oldDevice, true)
        try {
            repository.connectFiioJa11()
            await { recorder.snapshot().records.any { it.event.phase == Ja11ConvergencePhase.REFRESH_ENDED } }
            val oldGeneration = session.fiioJa11Transport.sessionGeneration
            val acquisitionId = recorder.allocateId()
            val pending = async {
                session.withExclusiveFiioJa11Operation {
                    session.readFiioJa11Snapshot(Ja11ConvergenceReadContext(
                        acquisitionId, 0L, Ja11ConvergenceOwner.EXPLICIT_READ, null, 1,
                    ))
                }
            }
            withTimeout(3_000) { entered.await() }
            shadow.removeUsbDevice(oldDevice)
            application.sendBroadcast(Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                .putExtra(UsbManager.EXTRA_DEVICE, oldDevice))
            shadowOf(Looper.getMainLooper()).idle()
            await { session.fiioJa11Transport.sessionGeneration == 0L }
            val replacement = ja11TestDevice("/dev/bus/usb/001/002", 0x0102)
            shadow.addOrUpdateUsbDevice(replacement, true)
            repository.connectFiioJa11()
            await { session.fiioJa11Transport.sessionGeneration > 0L }
            val replacementGeneration = session.fiioJa11Transport.sessionGeneration
            assertThat(replacementGeneration).isNotEqualTo(oldGeneration)
            release.complete(Unit)
            assertThat(pending.await()).isNull()
            val rejection = recorder.snapshot().records.map { it.event }.single {
                it.acquisitionId == acquisitionId && it.phase == Ja11ConvergencePhase.GENERATION_CHANGED
            }
            assertThat(rejection.generation).isEqualTo(replacementGeneration)
            await { repository.fiioJa11SnapshotState.value.bundle != null }
            assertThat(repository.fiioJa11SnapshotState.value.bundle!!.snapshot.sessionGeneration)
                .isEqualTo(replacementGeneration)
            assertThat(recorder.snapshot().records.none {
                it.event.phase == Ja11ConvergencePhase.PUBLISHED && it.event.bundleGeneration == oldGeneration
            }).isTrue()
        } finally { release.complete(Unit); repository.close() }
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(3_000) { while (!condition()) delay(5) }
    private fun field(owner: Any, name: String): Any = requireNotNull(owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner))
    private fun setField(owner: Any, name: String, value: Any) {
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(owner, value)
    }

    private class ControlledSource(
        private val beforeProgram: suspend () -> Unit = {},
        private val beforeGain: suspend () -> Unit = {},
    ) : FiioJa11SnapshotSource {
        val reads = AtomicInteger()
        val misses = AtomicInteger(1)
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? {
            reads.incrementAndGet()
            beforeProgram()
            return if (misses.getAndDecrement() > 0) null else FiioJa11Protocol.EqProgram.USER_1
        }
        override suspend fun readBand(index: Int) = FiioJa11Protocol.Band("peak_dip", 1_000.0, 0.0, 1.0)
        override suspend fun readGlobalGainDb(): Double { beforeGain(); return 0.0 }
    }
}
