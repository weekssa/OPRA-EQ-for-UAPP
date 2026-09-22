package com.weekssa.opraeqforuapp.domain.ew300

/** Result of the exact post-Save restoration transaction used by the signed validation candidate. */
sealed interface Ew300RestorationResult {
    data object Verified : Ew300RestorationResult
    data class NoBaseline(val reason: String) : Ew300RestorationResult
    data class DeviceUnavailable(val reason: String) : Ew300RestorationResult
    data class NotSuitable(val reason: String) : Ew300RestorationResult
    data class TransferFailed(val reason: String) : Ew300RestorationResult
    data class VerificationFailed(val reason: String) : Ew300RestorationResult
}
