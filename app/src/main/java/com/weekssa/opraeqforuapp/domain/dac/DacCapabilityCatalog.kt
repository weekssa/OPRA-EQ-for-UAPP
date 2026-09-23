package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls

data class DacCapabilitySet(
    val identity: DacDeviceIdentity,
    val exposedControls: List<DacControlDescriptor>,
) {
    init {
        require(exposedControls.map { it.id }.distinct().size == exposedControls.size) {
            "Exposed DAC control IDs must be unique."
        }
    }

    fun controlsIn(section: DacControlSection): List<DacControlDescriptor> =
        exposedControls.filter { it.section == section }
}

/** Static model-specific product/capability knowledge. */
object DacCapabilityCatalog {
    fun forDevice(deviceId: DacDeviceId): DacCapabilitySet = when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> DacCapabilitySet(
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("TRN", DacMetadataOrigin.STATIC_KNOWN),
                model = DacMetadataValue("Black Pearl", DacMetadataOrigin.STATIC_KNOWN),
                usbVendorId = 0x3302,
                usbProductId = 0x43E8,
                validationStatus = DacValidationStatus.HARDWARE_QUALIFIED,
            ),
            // Black Pearl production DEVICE write exposure remains separately hardware-gated.
            exposedControls = emptyList(),
        )

        DacDeviceId.FIIO_JA11 -> DacCapabilitySet(
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("FiiO", DacMetadataOrigin.STATIC_KNOWN),
                model = DacMetadataValue("JA11", DacMetadataOrigin.STATIC_KNOWN),
                usbVendorId = 0x2972,
                // Canonical static identity uses UAC 2.0. Runtime DEVICE state reports the actual
                // UAC 1.0 (0101) or UAC 2.0 (0102) PID from the current physical session.
                usbProductId = 0x0102,
                validationStatus = DacValidationStatus.HARDWARE_VALIDATION_PENDING,
            ),
            exposedControls = FiioJa11DeviceControls.descriptors,
        )

        DacDeviceId.SIMGOT_EW300 -> DacCapabilitySet(
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("LE XIAN", DacMetadataOrigin.USB_REPORTED),
                model = DacMetadataValue("SIMGOT EW300 DSP", DacMetadataOrigin.USB_REPORTED),
                usbVendorId = 0x31B2,
                usbProductId = 0x0111,
                validationStatus = DacValidationStatus.HARDWARE_QUALIFIED,
            ),
            exposedControls = emptyList(),
        )

        DacDeviceId.JCALLY_JM12_STOCK -> DacCapabilitySet(
            // Historical/internal compatibility identity only. Current recognition never returns
            // this device and no current product surface may expose it.
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("Unsupported legacy device", DacMetadataOrigin.STATIC_KNOWN),
                model = DacMetadataValue("Legacy", DacMetadataOrigin.STATIC_KNOWN),
                usbVendorId = 0x31B2,
                usbProductId = 0x0111,
                validationStatus = DacValidationStatus.HARDWARE_VALIDATION_PENDING,
            ),
            exposedControls = emptyList(),
        )
    }
}
