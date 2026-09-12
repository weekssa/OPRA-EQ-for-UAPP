package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.dac.DacCapabilityCatalog
import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlSection
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacMetadataOrigin
import com.weekssa.opraeqforuapp.domain.dac.DacValidationStatus
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import java.util.Locale

@Composable
internal fun CapabilityDrivenDeviceStatus(
    deviceId: DacDeviceId,
    blackPearlQualificationState: BlackPearlQualificationUiState,
    blackPearlQualificationEnabled: Boolean,
    onReadBlackPearlQualification: () -> Unit,
) {
    val capabilities = DacCapabilityCatalog.forDevice(deviceId)
    val identity = capabilities.identity

    Text(
        text = stringResource(R.string.my_dac_device_info),
        fontWeight = FontWeight.SemiBold,
    )
    DeviceInfoLine(
        label = stringResource(R.string.my_dac_device_model),
        value = "${identity.manufacturer.value} ${identity.model.value}",
        origin = identity.model.origin,
    )
    DeviceInfoLine(
        label = stringResource(R.string.my_dac_device_usb_identity),
        value = String.format(
            Locale.US,
            "%04X:%04X",
            identity.usbVendorId,
            identity.usbProductId,
        ),
        originLabel = stringResource(R.string.my_dac_origin_known_capability),
    )
    DeviceInfoLine(
        label = stringResource(R.string.my_dac_device_validation),
        value = stringResource(
            when (identity.validationStatus) {
                DacValidationStatus.HARDWARE_QUALIFIED -> R.string.my_dac_validation_qualified_short
                DacValidationStatus.HARDWARE_VALIDATION_PENDING -> R.string.my_dac_validation_pending
            },
        ),
        originLabel = stringResource(R.string.my_dac_origin_validation_status),
    )
    identity.firmwareVersion?.let { firmware ->
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_firmware),
            value = firmware.value,
            origin = firmware.origin,
        )
    }

    DEVICE_SECTION_ORDER.forEach { section ->
        val controls = capabilities.controlsIn(section)
        if (controls.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Text(
                text = deviceSectionLabel(section),
                fontWeight = FontWeight.SemiBold,
            )
            controls.forEach { control ->
                ReadOnlyCapabilityRow(control)
            }
        }
    }

    if (capabilities.exposedControls.isEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.my_dac_no_device_controls))
    }

    if (deviceId == DacDeviceId.TRN_BLACK_PEARL) {
        BlackPearlQualificationPanel(
            state = blackPearlQualificationState,
            enabled = blackPearlQualificationEnabled,
            onRead = onReadBlackPearlQualification,
        )
    }
}

@Composable
private fun BlackPearlQualificationPanel(
    state: BlackPearlQualificationUiState,
    enabled: Boolean,
    onRead: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    Text(
        text = stringResource(R.string.my_dac_qualification_title),
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.my_dac_qualification_explanation),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = onRead,
        enabled = enabled && !state.isReading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (state.isReading) {
                    R.string.my_dac_qualification_reading
                } else {
                    R.string.my_dac_qualification_action
                },
            ),
        )
    }

    state.error?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    state.snapshot?.let { snapshot ->
        Text(
            text = stringResource(
                if (state.isCurrentSession) {
                    R.string.my_dac_qualification_current
                } else {
                    R.string.my_dac_qualification_stale
                },
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        QualificationSnapshotRows(snapshot)
    }
}

@Composable
private fun QualificationSnapshotRows(snapshot: BlackPearlDeviceQualificationSnapshot) {
    QualificationValue(
        label = stringResource(R.string.my_dac_device_firmware),
        value = snapshot.firmwareVersion,
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_filter),
        value = filterLabel(snapshot.filterCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_gain_mode),
        value = gainModeLabel(snapshot.gainModeCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_amp_topology),
        value = ampTopologyLabel(snapshot.ampTopologyCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_mic_gain),
        value = stringResource(R.string.my_dac_qualification_db, snapshot.micGainDb.toDouble()),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_balance),
        value = snapshot.signedBalanceDb?.let { balance ->
            if (balance == 0) {
                stringResource(R.string.my_dac_qualification_balance_center)
            } else {
                stringResource(R.string.my_dac_qualification_balance_db, balance)
            }
        } ?: stringResource(
            R.string.my_dac_qualification_balance_inconsistent,
            snapshot.leftBalanceDb,
            snapshot.rightBalanceDb,
        ),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_playback_gain),
        value = "${stringResource(R.string.my_dac_qualification_db, snapshot.playbackGainDb)} · " +
            stringResource(R.string.my_dac_qualification_raw, snapshot.playbackGainRaw),
    )
}

@Composable
private fun QualificationValue(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun filterLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.FILTER_FAST_LL -> R.string.my_dac_qualification_filter_fast_ll
        BlackPearlDeviceControlReadCodec.FILTER_FAST_PC -> R.string.my_dac_qualification_filter_fast_pc
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_LL -> R.string.my_dac_qualification_filter_slow_ll
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_PC -> R.string.my_dac_qualification_filter_slow_pc
        BlackPearlDeviceControlReadCodec.FILTER_NOS -> R.string.my_dac_qualification_filter_nos
        else -> error("Validated Black Pearl filter code unexpectedly missing.")
    },
)

@Composable
private fun gainModeLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.GAIN_MODE_LOW -> R.string.my_dac_qualification_gain_low
        BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH -> R.string.my_dac_qualification_gain_high
        else -> error("Validated Black Pearl gain mode unexpectedly missing.")
    },
)

@Composable
private fun ampTopologyLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_H -> R.string.my_dac_qualification_amp_class_h
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB -> R.string.my_dac_qualification_amp_class_ab
        else -> error("Validated Black Pearl amp topology unexpectedly missing.")
    },
)

@Composable
private fun DeviceInfoLine(
    label: String,
    value: String,
    origin: DacMetadataOrigin? = null,
    originLabel: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = originLabel ?: originLabel(origin),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadOnlyCapabilityRow(control: DacControlDescriptor) {
    // Increment 11 intentionally has no exposed additional controls. Keeping this renderer generic
    // means a later physically qualified descriptor can appear without protocol-specific Compose.
    Text(
        text = control.id.value,
        modifier = Modifier.padding(vertical = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun originLabel(origin: DacMetadataOrigin?): String = stringResource(
    when (origin) {
        DacMetadataOrigin.USB_REPORTED -> R.string.my_dac_origin_usb_reported
        DacMetadataOrigin.DEVICE_REPORTED -> R.string.my_dac_origin_device_reported
        DacMetadataOrigin.STATIC_KNOWN,
        null,
        -> R.string.my_dac_origin_known_capability
    },
)

@Composable
private fun deviceSectionLabel(section: DacControlSection): String = stringResource(
    when (section) {
        DacControlSection.PLAYBACK -> R.string.my_dac_section_playback
        DacControlSection.DAC_FILTER -> R.string.my_dac_section_dac_filter
        DacControlSection.OUTPUT -> R.string.my_dac_section_output
        DacControlSection.MICROPHONE_INPUT -> R.string.my_dac_section_microphone
        DacControlSection.USB_SYSTEM -> R.string.my_dac_section_usb_system
        DacControlSection.ADVANCED -> R.string.my_dac_section_advanced
        DacControlSection.DEVICE_INFORMATION -> R.string.my_dac_device_info
    },
)

private val DEVICE_SECTION_ORDER = listOf(
    DacControlSection.PLAYBACK,
    DacControlSection.DAC_FILTER,
    DacControlSection.OUTPUT,
    DacControlSection.MICROPHONE_INPUT,
    DacControlSection.USB_SYSTEM,
    DacControlSection.ADVANCED,
)
