package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph

@Composable
fun MyDacScreen(
    recognitionState: DacRecognitionState,
    blackPearlConnectionState: BlackPearlConnectionState,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    jcallyJm12ConnectionState: Kt02h20ConnectionState,
    blackPearlHardwareEqState: HardwareEqSnapshotState,
    blackPearlHardwareEqMatch: HardwareEqMatchResolution?,
    onConnectDac: (DacDeviceId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recognized = recognitionState.recognizedDeviceIds.sortedBy(DacDeviceId::ordinal)
    val present = recognitionState.presentDeviceIds.sortedBy(DacDeviceId::ordinal)
    var selectedDeviceName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(recognized, present, selectedDeviceName) {
        val selected = selectedDeviceName?.let { name ->
            runCatching { DacDeviceId.valueOf(name) }.getOrNull()
        }
        if (selected !in recognized) {
            selectedDeviceName = when {
                recognized.size == 1 -> recognized.single().name
                present.size == 1 -> present.single().name
                else -> null
            }
        }
    }

    val selectedDevice = selectedDeviceName?.let { name ->
        runCatching { DacDeviceId.valueOf(name) }.getOrNull()
    }?.takeIf(recognized::contains)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selectedDevice == null) {
            Text(
                text = stringResource(R.string.my_dac_multiple_devices),
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.my_dac_choose_device))
            recognized.forEach { deviceId ->
                TextButton(
                    onClick = { selectedDeviceName = deviceId.name },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(deviceLabel(deviceId))
                }
            }
            return@Column
        }

        Text(
            text = deviceLabel(selectedDevice),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            connectionStatus(
                deviceId = selectedDevice,
                presentNow = selectedDevice in recognitionState.presentDeviceIds,
                blackPearl = blackPearlConnectionState,
                fiioJa11 = fiioJa11ConnectionState,
                jcallyJm12 = jcallyJm12ConnectionState,
            ),
        )

        if (
            shouldOfferMyDacConnect(
                deviceId = selectedDevice,
                recognitionState = recognitionState,
                blackPearl = blackPearlConnectionState,
                fiioJa11 = fiioJa11ConnectionState,
                jcallyJm12 = jcallyJm12ConnectionState,
            )
        ) {
            Text(stringResource(R.string.my_dac_connect_explanation))
            Button(
                onClick = { onConnectDac(selectedDevice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (
                            hasMyDacConnectionError(
                                deviceId = selectedDevice,
                                blackPearl = blackPearlConnectionState,
                                fiioJa11 = fiioJa11ConnectionState,
                                jcallyJm12 = jcallyJm12ConnectionState,
                            )
                        ) {
                            R.string.my_dac_action_retry_connect
                        } else {
                            R.string.my_dac_action_connect
                        },
                    ),
                )
            }
        }

        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(stringResource(R.string.my_dac_tab_eq)) },
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text(stringResource(R.string.my_dac_tab_device)) },
            )
        }

        when (selectedTabIndex) {
            0 -> when (selectedDevice) {
                DacDeviceId.TRN_BLACK_PEARL -> BlackPearlEqStatus(
                    snapshotState = blackPearlHardwareEqState,
                    matchResolution = blackPearlHardwareEqMatch,
                )
                DacDeviceId.FIIO_JA11,
                DacDeviceId.JCALLY_JM12_STOCK,
                -> Text(stringResource(R.string.my_dac_hardware_pending_eq))
            }

            else -> DeviceStatus(selectedDevice)
        }
    }
}

internal fun shouldOfferMyDacConnect(
    deviceId: DacDeviceId,
    recognitionState: DacRecognitionState,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    jcallyJm12: Kt02h20ConnectionState,
): Boolean {
    if (deviceId !in recognitionState.presentDeviceIds) return false
    return when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL ->
            blackPearl is BlackPearlConnectionState.Disconnected ||
                blackPearl is BlackPearlConnectionState.Error
        DacDeviceId.FIIO_JA11 ->
            fiioJa11 is Kt02h20ConnectionState.Disconnected ||
                fiioJa11 is Kt02h20ConnectionState.Error
        DacDeviceId.JCALLY_JM12_STOCK ->
            jcallyJm12 is Kt02h20ConnectionState.Disconnected ||
                jcallyJm12 is Kt02h20ConnectionState.Error
    }
}

internal fun hasMyDacConnectionError(
    deviceId: DacDeviceId,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    jcallyJm12: Kt02h20ConnectionState,
): Boolean = when (deviceId) {
    DacDeviceId.TRN_BLACK_PEARL -> blackPearl is BlackPearlConnectionState.Error
    DacDeviceId.FIIO_JA11 -> fiioJa11 is Kt02h20ConnectionState.Error
    DacDeviceId.JCALLY_JM12_STOCK -> jcallyJm12 is Kt02h20ConnectionState.Error
}

@Composable
private fun BlackPearlEqStatus(
    snapshotState: HardwareEqSnapshotState,
    matchResolution: HardwareEqMatchResolution?,
) {
    if (snapshotState.isReading) {
        Text(stringResource(R.string.my_dac_reading_eq))
    }

    val bundle = snapshotState.bundle
    if (bundle == null) {
        Text(stringResource(R.string.my_dac_no_verified_eq))
        return
    }

    Text(
        text = stringResource(
            if (snapshotState.freshness == DacStateFreshness.CURRENT) {
                R.string.my_dac_current_hardware
            } else {
                R.string.my_dac_last_read
            },
        ),
    )

    val match = matchResolution?.match
    Text(
        text = when (match) {
            HardwareEqMatch.Flat -> stringResource(R.string.my_dac_eq_flat)
            is HardwareEqMatch.Exact -> stringResource(R.string.my_dac_eq_exact)
            is AmbiguousExactHardwareEqMatch -> stringResource(R.string.my_dac_eq_ambiguous)
            is HardwareEqMatch.ModifiedKnown -> stringResource(R.string.my_dac_eq_modified)
            HardwareEqMatch.Unknown, null -> stringResource(R.string.my_dac_eq_unknown)
        },
        fontWeight = FontWeight.SemiBold,
    )

    when (match) {
        is HardwareEqMatch.Exact -> {
            Text(match.savedEq.displayName)
            val representation = matchResolution?.representation(match.savedEq.savedEqKey)
            if (representation != null) {
                val fidelity = stringResource(
                    if (representation.fidelity == DevicePresetFidelity.EXACT) {
                        R.string.output_status_exact
                    } else {
                        R.string.output_status_optimized
                    },
                )
                Text(
                    stringResource(
                        R.string.my_dac_adaptation,
                        fidelity,
                        representation.adaptationSummary,
                    ),
                )
            }
        }

        HardwareEqMatch.Unknown, null -> Text(stringResource(R.string.my_dac_eq_unknown_detail))
        else -> Unit
    }

    val responseCurve = remember(bundle.snapshot.filters) {
        HardwareEqResponseEvaluator.evaluate(bundle.snapshot.filters)
    }
    Text(
        text = stringResource(R.string.my_dac_eq_response),
        fontWeight = FontWeight.SemiBold,
    )
    if (responseCurve == null) {
        Text(stringResource(R.string.my_dac_response_unavailable))
    } else {
        val activeBandCount = bundle.snapshot.filters.count { filter -> filter.isAcousticallyActive() }
        DacEqResponseGraph(
            curve = responseCurve,
            filters = bundle.snapshot.filters,
            accessibilityDescription = stringResource(
                R.string.my_dac_response_graph_accessibility,
                activeBandCount,
                responseCurve.minimumGainDb,
                responseCurve.maximumGainDb,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    HorizontalDivider()
    Text(stringResource(R.string.my_dac_filter_count, bundle.snapshot.filters.size))
    bundle.snapshot.activeSlot?.let { slot ->
        Text(stringResource(R.string.my_dac_active_slot, slot))
    }
    bundle.snapshot.playbackGainDb?.let { gainDb ->
        Text(stringResource(R.string.my_dac_playback_gain, gainDb))
    }
}

@Composable
private fun DeviceStatus(deviceId: DacDeviceId) {
    Text(
        text = stringResource(R.string.my_dac_device_info),
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        stringResource(
            if (deviceId == DacDeviceId.TRN_BLACK_PEARL) {
                R.string.my_dac_validation_qualified
            } else {
                R.string.my_dac_validation_pending
            },
        ),
    )
    HorizontalDivider()
    Text(stringResource(R.string.my_dac_no_device_controls))
}

@Composable
private fun deviceLabel(deviceId: DacDeviceId): String = stringResource(
    when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> R.string.dac_trn_black_pearl
        DacDeviceId.FIIO_JA11 -> R.string.dac_fiio_ja11
        DacDeviceId.JCALLY_JM12_STOCK -> R.string.dac_jcally_jm12
    },
)

@Composable
private fun connectionStatus(
    deviceId: DacDeviceId,
    presentNow: Boolean,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    jcallyJm12: Kt02h20ConnectionState,
): String = when (deviceId) {
    DacDeviceId.TRN_BLACK_PEARL -> when (blackPearl) {
        BlackPearlConnectionState.Connected -> stringResource(R.string.my_dac_connected)
        BlackPearlConnectionState.Connecting -> stringResource(R.string.my_dac_connecting)
        BlackPearlConnectionState.Disconnected -> stringResource(
            if (presentNow) R.string.my_dac_detected_not_open else R.string.my_dac_disconnected,
        )
        is BlackPearlConnectionState.Error -> blackPearl.message
    }

    DacDeviceId.FIIO_JA11 -> ktConnectionStatus(fiioJa11, presentNow)
    DacDeviceId.JCALLY_JM12_STOCK -> ktConnectionStatus(jcallyJm12, presentNow)
}

@Composable
private fun ktConnectionStatus(
    state: Kt02h20ConnectionState,
    presentNow: Boolean,
): String = when (state) {
    Kt02h20ConnectionState.Connected -> stringResource(R.string.my_dac_connected)
    Kt02h20ConnectionState.Connecting -> stringResource(R.string.my_dac_connecting)
    Kt02h20ConnectionState.Disconnected -> stringResource(
        if (presentNow) R.string.my_dac_detected_not_open else R.string.my_dac_disconnected,
    )
    is Kt02h20ConnectionState.Error -> state.message
}
