package com.weekssa.opraeqforuapp.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState

/** Shows the approved opt-in navigation prompt without taking over the user's current workflow. */
@Composable
internal fun MyDacRecognitionPromptEffect(
    recognitionState: DacRecognitionState,
    isMyDacOpen: Boolean,
    snackbarHostState: SnackbarHostState,
    detectedMessage: String,
    openActionLabel: String,
    isAnyDacConnected: Boolean = false,
    suppressWhileOperationRunning: Boolean = false,
    onOpenMyDac: () -> Unit,
) {
    var previousPresentDeviceNames by rememberSaveable {
        mutableStateOf(
            ArrayList(recognitionState.presentDeviceIds.map { deviceId -> deviceId.name }.sorted()),
        )
    }
    val currentPresentDeviceNames = recognitionState.presentDeviceIds
        .map { deviceId -> deviceId.name }
        .sorted()

    LaunchedEffect(isMyDacOpen, isAnyDacConnected) {
        if (isMyDacOpen || isAnyDacConnected) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(currentPresentDeviceNames, isMyDacOpen, isAnyDacConnected, suppressWhileOperationRunning) {
        val newlyPresent = newlyPresentDac(
            previousPresentDeviceNames = previousPresentDeviceNames,
            currentPresentDeviceIds = recognitionState.presentDeviceIds,
        )
        previousPresentDeviceNames = ArrayList(currentPresentDeviceNames)

        if (
            newlyPresent != null &&
            !isMyDacOpen &&
            !isAnyDacConnected &&
            !suppressWhileOperationRunning
        ) {
            val result = snackbarHostState.showSnackbar(
                message = detectedMessage,
                actionLabel = openActionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onOpenMyDac()
            }
        }
    }
}
