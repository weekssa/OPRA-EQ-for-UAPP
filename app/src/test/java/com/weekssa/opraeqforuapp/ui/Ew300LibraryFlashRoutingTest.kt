package com.weekssa.opraeqforuapp.ui

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.createEqLibraryDependencies
import com.weekssa.opraeqforuapp.data.dac.DacSessionRepository
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Flasher
import com.weekssa.opraeqforuapp.domain.ew300.Ew300GainStateStore
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Transport
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.OutputBehavior
import com.weekssa.opraeqforuapp.ui.screens.hardwareFlashUnavailableReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Proves the existing EQ Library Flash route reaches the EW300 transaction when the parity package
 * is set up, and that it fails with the exact reason when it is not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Ew300LibraryFlashRoutingTest {
    @Test
    fun flashSavedEqRoutesToEw300AndSavesExactlyOnce() = runBlocking {
        val harness = Harness.create()
        try {
            val record = harness.dependencies.savedEqRepository.importPersonal(
                ExportDevice.SIMGOT_EW300.name,
                "Fixture",
                "EW300 fixture",
                "Fixture personal EQ",
                null,
                "Preamp: 0 dB\nFilter 1: ON PK Fc 1000 Hz Gain 0 dB Q 1.0",
            )

            val message = harness.resolve(harness.vm.flashSavedEq(record.entryId))

            assertThat(message).contains("Flash successful · SIMGOT EW300 DSP")
            assertThat(message).contains("Five Peak bands written")
            assertThat(harness.transport.commitCount).isEqualTo(1)
            assertThat(harness.transport.writes).hasSize(10)
        } finally {
            harness.close()
        }
    }

    @Test
    fun flashManagedProfileRoutesToEw300AndSavesExactlyOnce() = runBlocking {
        val harness = Harness.create()
        try {
            harness.dependencies.managedHeadphonesRepository.saveSelection(
                catalog = harness.catalog,
                productId = "fixture-product",
                stagedSelectedProfileIds = setOf("fixture-profile"),
                autoIncludeNewProfiles = false,
                outputId = ExportDevice.SIMGOT_EW300.name,
            )

            val message = harness.resolve(
                harness.vm.flashManagedProfile("fixture-product", "fixture-profile"),
            )

            assertThat(message).contains("Flash successful · SIMGOT EW300 DSP")
            assertThat(harness.transport.commitCount).isEqualTo(1)
            assertThat(harness.transport.writes).hasSize(10)
        } finally {
            harness.close()
        }
    }

    @Test
    fun flashReasonNamesTheExactBlockerForEachState() {
        val ready = hardwareFlashUnavailableReason(
            activeOutput = ExportDevice.SIMGOT_EW300,
            directBlackPearlFlashEnabled = false,
            blackPearlConnected = false,
            directFiioJa11FlashEnabled = false,
            fiioJa11Connected = false,
            directJcallyJm12FlashEnabled = false,
            jcallyJm12Connected = false,
            directEw300FlashEnabled = true,
            ew300Connected = true,
        )
        assertThat(ready).isNull()

        val flashOff = hardwareFlashUnavailableReason(
            activeOutput = ExportDevice.SIMGOT_EW300,
            directBlackPearlFlashEnabled = false,
            blackPearlConnected = false,
            directFiioJa11FlashEnabled = false,
            fiioJa11Connected = false,
            directJcallyJm12FlashEnabled = false,
            jcallyJm12Connected = false,
            directEw300FlashEnabled = false,
            ew300Connected = true,
        )
        assertThat(flashOff).isEqualTo("Direct Flash is off. Enable it in Settings → SIMGOT EW300 DSP.")

        val disconnected = hardwareFlashUnavailableReason(
            activeOutput = ExportDevice.SIMGOT_EW300,
            directBlackPearlFlashEnabled = false,
            blackPearlConnected = false,
            directFiioJa11FlashEnabled = false,
            fiioJa11Connected = false,
            directJcallyJm12FlashEnabled = false,
            jcallyJm12Connected = false,
            directEw300FlashEnabled = true,
            ew300Connected = false,
        )
        assertThat(disconnected).isEqualTo("SIMGOT EW300 DSP is not connected. Connect it in My DAC.")
    }

    @Test
    fun directFlashOffBlocksTheFlashWithTheExactReason() = runBlocking {
        val harness = Harness.create(directFlashEnabled = false)
        try {
            val record = harness.dependencies.savedEqRepository.importPersonal(
                ExportDevice.SIMGOT_EW300.name,
                "Fixture",
                "EW300 fixture",
                "Fixture personal EQ",
                null,
                "Preamp: 0 dB\nFilter 1: ON PK Fc 1000 Hz Gain 0 dB Q 1.0",
            )

            val message = harness.resolve(harness.vm.flashSavedEq(record.entryId))

            assertThat(message).isEqualTo(
                "Enable Direct Flash for SIMGOT EW300 DSP in Settings before flashing.",
            )
            assertThat(harness.transport.writes).isEmpty()
            assertThat(harness.transport.commitCount).isEqualTo(0)
        } finally {
            harness.close()
        }
    }

    @Test
    fun disconnectedDeviceBlocksTheFlashWithTheExactReason() = runBlocking {
        val harness = Harness.create(connected = false)
        try {
            val record = harness.dependencies.savedEqRepository.importPersonal(
                ExportDevice.SIMGOT_EW300.name,
                "Fixture",
                "EW300 fixture",
                "Fixture personal EQ",
                null,
                "Preamp: 0 dB\nFilter 1: ON PK Fc 1000 Hz Gain 0 dB Q 1.0",
            )

            val message = harness.resolve(harness.vm.flashSavedEq(record.entryId))

            assertThat(message).isEqualTo("Connect SIMGOT EW300 DSP before flashing its EQ.")
            assertThat(harness.transport.writes).isEmpty()
            assertThat(harness.transport.commitCount).isEqualTo(0)
        } finally {
            harness.close()
        }
    }

    private class Harness(
        val dependencies: EqLibraryViewModel.Dependencies,
        val vm: EqLibraryViewModel,
        val transport: FakeEw300Transport,
        val database: OpraEqDatabase,
        val catalog: OpraCatalog,
        private val store: ViewModelStore,
    ) {
        fun resolve(text: UiText): String = when (text) {
            is UiText.Dynamic -> text.value
            is UiText.Resource -> RuntimeEnvironment.getApplication()
                .getString(text.resourceId, *text.args.toTypedArray())
        }

        fun close() {
            store.clear()
            dependencies.hardwareRepository.close()
        }

        companion object {
            suspend fun create(
                directFlashEnabled: Boolean = true,
                connected: Boolean = true,
            ): Harness {
                val app = RuntimeEnvironment.getApplication()
                val dependencies = createEqLibraryDependencies(app)
                dependencies.preferencesRepository.setOutputBehavior(OutputBehavior.Manual)
                dependencies.preferencesRepository.setExportTargetEnabled(
                    ExportDevice.SIMGOT_EW300,
                    true,
                )
                dependencies.preferencesRepository.setActiveExportTarget(ExportDevice.SIMGOT_EW300)
                dependencies.preferencesRepository.setDirectEw300FlashEnabled(directFlashEnabled)

                val repository = dependencies.hardwareRepository
                val session = field(repository, "dacSessionRepository") as DacSessionRepository
                val hid = requireNotNull(field(session.ew300Transport, "hid"))
                @Suppress("UNCHECKED_CAST")
                val state = field(hid, "mutableState") as MutableStateFlow<Kt02h20ConnectionState>
                state.value = if (connected) {
                    Kt02h20ConnectionState.Connected
                } else {
                    Kt02h20ConnectionState.Disconnected
                }

                val transport = FakeEw300Transport()
                val flasher = Ew300Flasher(
                    transport = transport,
                    gainStateStore = FakeGainStore(),
                    mutationAuthorized = { true },
                    experimentalShelfEnabled = true,
                )
                setField(repository, "ew300Flasher", flasher)

                val catalog = fixtureCatalog()
                val vm = EqLibraryViewModel.Factory { dependencies }
                    .create(EqLibraryViewModel::class.java)
                val store = ViewModelStore().apply { put("ew300-routing", vm) }
                return Harness(
                    dependencies = dependencies,
                    vm = vm,
                    transport = transport,
                    database = field(
                        dependencies.managedHeadphonesRepository,
                        "database",
                    ) as OpraEqDatabase,
                    catalog = catalog,
                    store = store,
                )
            }
        }
    }

    private class FakeEw300Transport : Ew300Transport {
        override var deviceFingerprintKey: String = "test-ew300"
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        override val registerWriteCount: Long get() = writes.size.toLong()
        override val saveCommandCount: Long get() = commitCount.toLong()
        val state = (0 until Ew300Protocol.BAND_COUNT)
            .flatMap { index ->
                val register = Ew300Protocol.bandRegister(index)
                listOf(register, register + 1)
            }
            .associateWith { ByteArray(4) }
            .toMutableMap()
            .also {
                it[Ew300Protocol.PROTOCOL_FLAGS_REGISTER] = ByteArray(4)
                it[0x24] = ByteArray(4)
                it[Ew300Protocol.GLOBAL_GAIN_REGISTER] = ByteArray(4)
            }
        val writes = mutableListOf<Int>()
        var commitCount = 0

        override suspend fun readRegister(register: Int): ByteArray? = state[register]

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writes += register
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            commitCount++
            return true
        }
    }

    private class FakeGainStore : Ew300GainStateStore {
        override fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean = false
        override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) = Unit
        override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int = 0
        override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) = Unit
    }

    private companion object {
        fun fixtureCatalog(): OpraCatalog = OpraCatalog(
            vendors = listOf(OpraVendor("fixture-vendor", "Fixture")),
            products = listOf(
                OpraProduct(
                    "fixture-product",
                    "fixture-vendor",
                    "Fixture headphone",
                    "Headphone",
                    "Over-ear",
                ),
            ),
            profiles = listOf(
                OpraEqProfile(
                    id = "fixture-profile",
                    productId = "fixture-product",
                    author = "Fixture",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = null,
                    bands = listOf(
                        OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null),
                        OpraBand("peak_dip", 2_000.0, -1.5, 1.0, null),
                    ),
                ),
            ),
        )

        fun field(instance: Any, name: String): Any? {
            var type: Class<*>? = instance.javaClass
            while (type != null) {
                runCatching {
                    val declared = type.getDeclaredField(name)
                    declared.isAccessible = true
                    return declared.get(instance)
                }
                type = type.superclass
            }
            error("Missing field $name on ${instance.javaClass.name}")
        }

        fun setField(instance: Any, name: String, value: Any?) {
            var type: Class<*>? = instance.javaClass
            while (type != null) {
                val declared = runCatching { type.getDeclaredField(name) }.getOrNull()
                if (declared != null) {
                    declared.isAccessible = true
                    declared.set(instance, value)
                    return
                }
                type = type.superclass
            }
            error("Missing field $name on ${instance.javaClass.name}")
        }
    }
}

