package com.weekssa.opraeqforuapp.domain.dac

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

/**
 * Static product/capability knowledge for supported DAC identities.
 *
 * This catalog deliberately exposes no additional DEVICE controls until their exact semantics pass
 * EQ Library's own hardware qualification. Candidate protocol knowledge is not production exposure.
 */
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
            exposedControls = emptyList(),
        )

        DacDeviceId.FIIO_JA11 -> DacCapabilitySet(
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("FiiO", DacMetadataOrigin.STATIC_KNOWN),
                model = DacMetadataValue("JA11", DacMetadataOrigin.STATIC_KNOWN),
                usbVendorId = 0x2972,
                usbProductId = 0x0102,
                validationStatus = DacValidationStatus.HARDWARE_VALIDATION_PENDING,
            ),
            exposedControls = emptyList(),
        )

        DacDeviceId.JCALLY_JM12_STOCK -> DacCapabilitySet(
            identity = DacDeviceIdentity(
                deviceId = deviceId,
                manufacturer = DacMetadataValue("JCALLY", DacMetadataOrigin.STATIC_KNOWN),
                model = DacMetadataValue("JM12", DacMetadataOrigin.STATIC_KNOWN),
                usbVendorId = 0x31B2,
                usbProductId = 0x0111,
                validationStatus = DacValidationStatus.HARDWARE_VALIDATION_PENDING,
            ),
            exposedControls = emptyList(),
        )
    }
}
