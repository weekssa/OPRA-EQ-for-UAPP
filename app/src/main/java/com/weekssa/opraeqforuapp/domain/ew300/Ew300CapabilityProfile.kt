package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Versioned product authorization for the EW300 behavior qualified by the signed candidate.
 *
 * This is deliberately narrower than a VID/PID match.  The signed candidate is qualified for one
 * exact unit fingerprint; other units remain read-only until separately qualified and approved.
 */
object Ew300CapabilityProfile {
    const val VERSION = "ew300-production-v1"
    const val MANUFACTURER = "LE XIAN"
    const val PRODUCT = "SIMGOT EW300 DSP"
    const val VENDOR_ID_HEX = "31b2"
    const val PRODUCT_ID_HEX = "111"
    const val HID_INTERFACE = "3"
    const val DEVICE_REVISION = "1.01"
    const val QUALIFIED_FINGERPRINT =
        "vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|deviceRevision=1.01|interface=3"

    fun authorizesMutation(fingerprintKey: String?): Boolean {
        if (fingerprintKey.isNullOrBlank()) return false
        val fields = fingerprintKey.split('|')
            .mapNotNull { field ->
                val separator = field.indexOf('=')
                if (separator <= 0) null else field.substring(0, separator) to field.substring(separator + 1)
            }
            .toMap()
        return fingerprintKey == QUALIFIED_FINGERPRINT &&
            fields["vid"]?.lowercase() == VENDOR_ID_HEX &&
            fields["pid"]?.lowercase() == PRODUCT_ID_HEX &&
            fields["manufacturer"]?.equals(MANUFACTURER, ignoreCase = true) == true &&
            fields["product"]?.equals(PRODUCT, ignoreCase = true) == true &&
            fields["deviceRevision"] == DEVICE_REVISION &&
            fields["interface"] == HID_INTERFACE &&
            !fields["serial"].isNullOrBlank()
    }
}
