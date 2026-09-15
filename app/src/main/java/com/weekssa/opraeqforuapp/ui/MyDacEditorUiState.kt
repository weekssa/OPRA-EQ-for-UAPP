package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy

enum class MyDacEditorStage {
    CLOSED,
    EDIT,
    ALL_BANDS,
    REVIEW,
}

enum class MyDacEditorError {
    NOT_CONNECTED,
    READ_FAILED,
    WRONG_DEVICE,
}

enum class MyDacEditorApplyStatus {
    IDLE,
    APPLYING,
    CONFIRMATION_REQUIRED,
    VERIFIED,
    FAILED,
}

/**
 * ViewModel-owned workflow state for the approved My DAC manual editor.
 *
 * The hardware working copy is intentionally not Saveable UI state. It is derived from a fresh
 * verified read and remains local until explicit Review -> Apply. After any attempted hardware Apply,
 * the editor closes so its old baseline can never be reused; the repository republishes fresh hardware
 * truth and this state retains only concise success/failure feedback.
 */
data class MyDacEditorUiState(
    val isOpening: Boolean = false,
    val stage: MyDacEditorStage = MyDacEditorStage.CLOSED,
    val workingCopy: HardwareEqEditWorkingCopy? = null,
    val selectedBandIndex: Int? = null,
    val error: MyDacEditorError? = null,
    val applyStatus: MyDacEditorApplyStatus = MyDacEditorApplyStatus.IDLE,
    val applyFailureReason: String? = null,
) {
    init {
        require(stage == MyDacEditorStage.CLOSED || workingCopy != null) {
            "An open My DAC editor stage requires a local working copy."
        }
        require(!isOpening || stage == MyDacEditorStage.CLOSED) {
            "Editor cannot be opening and already open at the same time."
        }
        require(
            selectedBandIndex == null ||
                workingCopy?.filters?.any { filter -> filter.index == selectedBandIndex } == true,
        ) {
            "Selected editor band must exist in the local working copy."
        }
        require(applyStatus != MyDacEditorApplyStatus.APPLYING || stage == MyDacEditorStage.REVIEW) {
            "Editor can only apply from Review."
        }
        require(applyStatus != MyDacEditorApplyStatus.CONFIRMATION_REQUIRED || stage == MyDacEditorStage.REVIEW) {
            "Editor caution confirmation can only appear from Review."
        }
        require(applyStatus != MyDacEditorApplyStatus.FAILED || !applyFailureReason.isNullOrBlank()) {
            "Failed editor Apply requires a reason."
        }
    }

    val isOpen: Boolean
        get() = stage != MyDacEditorStage.CLOSED

    val selectedBand
        get() = selectedBandIndex?.let { index ->
            workingCopy?.filters?.firstOrNull { filter -> filter.index == index }
        }
}
