package com.weekssa.opraeqforuapp.ui

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.createEqLibraryDependencies
import com.weekssa.opraeqforuapp.data.dac.FiioJa11ControlRepository
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsRequest
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DeviceControlSource
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FiioJa11DefaultsUiStateTest {
    @Test
    fun defaultsFailureIsPublishedAndClearsSavingState() = runBlocking {
        val dependencies = createEqLibraryDependencies(RuntimeEnvironment.getApplication())
        val failingRepository = FiioJa11ControlRepository(UnavailableSource())
        val vm = EqLibraryViewModel.Factory {
            dependencies.copy(fiioJa11ControlRepository = failingRepository)
        }.create(EqLibraryViewModel::class.java)
        val store = ViewModelStore().apply { put("defaults-failure", vm) }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field(vm, "mutableFiioJa11DeviceState") as MutableStateFlow<FiioJa11DeviceUiState>
        val baseline = snapshot()
        stateFlow.value = FiioJa11DeviceUiState(snapshot = baseline, isCurrentSession = true)

        try {
            vm.setFiioJa11Defaults(
                FiioJa11DefaultsRequest(
                    reviewedSnapshot = baseline,
                    requestedVolume = 59,
                    requestedProgram = baseline.eqProgram,
                    targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                ),
            )

            withTimeout(5_000) {
                while (stateFlow.value.isSavingDefaults || stateFlow.value.error == null) {
                    shadowOf(Looper.getMainLooper()).idle()
                    delay(5)
                }
            }

            val failed = stateFlow.value
            assertThat(failed.isBusy).isFalse()
            assertThat(failed.isCurrentSession).isFalse()
            assertThat(failed.snapshot).isEqualTo(baseline)
            assertThat(failed.error).contains("Defaults were not verified")
            assertThat(failed.error).doesNotContain("REVIEW_CURRENTNESS")
            assertThat(failed.defaultsReport?.stage).isNotNull()
        } finally {
            store.clear()
            dependencies.hardwareRepository.close()
        }
    }

    private fun field(owner: Any, name: String): Any =
        requireNotNull(owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner))

    private fun snapshot(): FiioJa11DeviceSnapshot = FiioJa11DeviceSnapshot(
        sessionGeneration = 7L,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "2.20",
        sampleRateLabel = "384 kHz",
        outputVolume = 58,
        headsetControlEnabled = true,
        eqProgram = FiioJa11Protocol.EqProgram.USER_1,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )

    private class UnavailableSource : FiioJa11DeviceControlSource {
        override val sessionGeneration = 7L
        override val detachGeneration = 0L
        override val connectedDeviceSerial: String? = null
        override val supportedCandidateCount = 0
        override val connectedProductId: Int? = null

        override fun isSessionCurrent(sessionGeneration: Long) = true

        override suspend fun awaitExpectedReplacementSession(
            previousGeneration: Long,
            previousDetachGeneration: Long,
            previousSerial: String?,
            timeoutMillis: Long,
        ) = false

        override suspend fun readOutputVolume(): Int? = null
        override suspend fun readSampleRateLabel(): String? = null
        override suspend fun readFirmwareVersion(): String? = null
        override suspend fun readHeadsetControlEnabled(): Boolean? = null
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = null
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = null
        override suspend fun writeOutputVolume(level: Int) = false
        override suspend fun writeHeadsetControlEnabled(enabled: Boolean) = false
        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram) = false
        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode) = false
    }
}
