package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.dac.DacCapabilityCatalog
import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlSection
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacMetadataOrigin
import com.weekssa.opraeqforuapp.domain.dac.DacValidationStatus
import java.util.Locale

@Composable
internal fun CapabilityDrivenDeviceStatus(deviceId: DacDeviceId) {
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
}

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
