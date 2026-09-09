package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState

data class MyDacOpenRequest(
    val requestId: Long,
    val deviceId: DacDeviceId,
) {
    init {
        require(requestId > 0L) { "My DAC open request ID must be positive." }
    }
}

internal fun canFulfillMyDacOpenRequest(
    request: MyDacOpenRequest?,
    recognitionState: DacRecognitionState,
): Boolean = request != null && request.deviceId in recognitionState.recognizedDeviceIds
