package com.weekssa.opraeqforuapp.domain.dac

/**
 * Read-only USB recognition state for supported DAC identities.
 *
 * [presentDeviceIds] is current physical presence. [recognizedDeviceIds] is session-sticky and only
 * grows for the lifetime of the ViewModel-scoped DAC session owner, which keeps My DAC navigation
 * stable after disconnect without pretending the hardware is still present.
 */
data class DacRecognitionState(
    val presentDeviceIds: Set<DacDeviceId> = emptySet(),
    val recognizedDeviceIds: Set<DacDeviceId> = emptySet(),
) {
    init {
        require(recognizedDeviceIds.containsAll(presentDeviceIds)) {
            "Every currently present supported DAC must also be recognized for this app session."
        }
    }

    val hasRecognizedDevice: Boolean
        get() = recognizedDeviceIds.isNotEmpty()

    fun withPresentDevices(present: Set<DacDeviceId>): DacRecognitionState = DacRecognitionState(
        presentDeviceIds = present,
        recognizedDeviceIds = recognizedDeviceIds + present,
    )
}
