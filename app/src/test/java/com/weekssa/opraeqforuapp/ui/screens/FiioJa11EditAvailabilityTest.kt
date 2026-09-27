package com.weekssa.opraeqforuapp.ui.screens

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import org.junit.Test

class FiioJa11EditAvailabilityTest {
    @Test
    fun editorIsEnabledOnlyForFreshVerifiedUserOneOnTheCurrentSession() {
        assertThat(availability()).isTrue()
        assertThat(availability(program = FiioJa11Protocol.EqProgram.OFF)).isFalse()
        assertThat(availability(program = FiioJa11Protocol.EqProgram.VOCAL)).isFalse()
        assertThat(availability(program = FiioJa11Protocol.EqProgram.CLASSIC)).isFalse()
        assertThat(availability(program = FiioJa11Protocol.EqProgram.BASS)).isFalse()
        assertThat(availability(freshness = DacStateFreshness.LAST_READ_STALE)).isFalse()
        assertThat(availability(withSnapshot = false)).isFalse()
        assertThat(availability(connected = false)).isFalse()
        assertThat(availability(permissionRequired = true)).isFalse()
        assertThat(availability(currentSession = false)).isFalse()
        assertThat(availability(reading = true)).isFalse()
        assertThat(availability(busyOperation = true)).isFalse()
    }

    @Test
    fun editorIsDisabledWhenTheDeviceAndEqSnapshotsBelongToDifferentSessions() {
        assertThat(availability(deviceSessionGeneration = 2L)).isFalse()
    }

    private fun availability(
        connected: Boolean = true,
        program: FiioJa11Protocol.EqProgram = FiioJa11Protocol.EqProgram.USER_1,
        freshness: DacStateFreshness = DacStateFreshness.CURRENT,
        withSnapshot: Boolean = true,
        permissionRequired: Boolean = false,
        currentSession: Boolean = true,
        reading: Boolean = false,
        busyOperation: Boolean = false,
        deviceSessionGeneration: Long = 1L,
    ): Boolean {
        val deviceSnapshot = deviceSnapshot(program, deviceSessionGeneration)
        val eqBundle = HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
                FiioJa11Protocol.Band("peak_dip", 100.0 * (index + 1), 0.0, 1.0)
            },
            globalEqGainDb = 0.0,
            sessionGeneration = 1L,
            verifiedAtEpochMillis = 1L,
            activeProgram = FiioJa11Protocol.EqProgram.USER_1,
        )
        val hardwareState = if (!withSnapshot) {
            HardwareEqSnapshotState()
        } else {
            HardwareEqSnapshotState(
                bundle = eqBundle,
                freshness = freshness,
                isReading = reading,
            )
        }
        return fiioJa11EditActionEnabled(
            connectionState = if (permissionRequired) {
                Kt02h20ConnectionState.PermissionRequired("USB permission required")
            } else if (connected) {
                Kt02h20ConnectionState.Connected
            } else {
                Kt02h20ConnectionState.Disconnected
            },
            hardwareEqState = hardwareState,
            deviceState = FiioJa11DeviceUiState(
                snapshot = deviceSnapshot,
                isCurrentSession = currentSession,
            ),
            operationStatus = if (busyOperation) {
                FiioJa11OperationStatus.Running("operation-1", "EDITOR_APPLY")
            } else {
                FiioJa11OperationStatus.Idle
            },
        )
    }

    private fun deviceSnapshot(
        program: FiioJa11Protocol.EqProgram,
        sessionGeneration: Long,
    ) = FiioJa11DeviceSnapshot(
        sessionGeneration = sessionGeneration,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "1.0",
        sampleRateLabel = "96 kHz",
        outputVolume = 20,
        headsetControlEnabled = false,
        eqProgram = program,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )
}
