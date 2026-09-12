package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.dac.DacHeadroomStatus
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifference
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifferenceField
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditIssue
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditIssueSeverity
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorError
import com.weekssa.opraeqforuapp.ui.MyDacEditorStage
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import java.util.Locale

@Composable
internal fun BlackPearlEqEditorScreen(
    state: MyDacEditorUiState,
    onRetryOpen: () -> Unit,
    onClose: () -> Unit,
    onSelectBand: (Int) -> Unit,
    onShowAllBands: () -> Unit,
    onShowReview: () -> Unit,
    onUpdateBand: (Int, EqFilterType, Double, Double, Double) -> Unit,
    onUseSafeGain: () -> Unit,
    onResetEdits: () -> Unit,
    onApply: (Boolean) -> Unit,
) {
    when {
        state.isOpening -> {
            Text(
                text = stringResource(R.string.my_dac_editor_opening),
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.my_dac_editor_local_only))
            return
        }

        !state.isOpen -> {
            state.error?.let { error ->
                Text(
                    text = editorErrorText(error),
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onRetryOpen, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.my_dac_action_retry_connect))
                }
            }
            return
        }
    }

    val working = requireNotNull(state.workingCopy)
    EditorHeader(
        working = working,
        onClose = onClose,
        closeEnabled = state.applyStatus != MyDacEditorApplyStatus.APPLYING,
    )

    when (state.stage) {
        MyDacEditorStage.EDIT -> EditorMain(
            state = state,
            onSelectBand = onSelectBand,
            onShowAllBands = onShowAllBands,
            onShowReview = onShowReview,
            onUpdateBand = onUpdateBand,
            onUseSafeGain = onUseSafeGain,
            onResetEdits = onResetEdits,
        )

        MyDacEditorStage.ALL_BANDS -> AllBands(
            working = working,
            selectedBandIndex = state.selectedBandIndex,
            onSelectBand = onSelectBand,
            onBack = { state.selectedBandIndex?.let(onSelectBand) },
        )

        MyDacEditorStage.REVIEW -> ReviewChanges(
            state = state,
            onBack = { state.selectedBandIndex?.let(onSelectBand) },
            onApply = onApply,
        )

        MyDacEditorStage.CLOSED -> Unit
    }
}

@Composable
private fun EditorHeader(
    working: HardwareEqEditWorkingCopy,
    onClose: () -> Unit,
    closeEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.my_dac_editor_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    R.string.my_dac_editor_subtitle,
                    working.baselineSnapshot.activeSlot ?: 0,
                ),
            )
        }
        TextButton(onClick = onClose, enabled = closeEnabled) {
            Text(stringResource(R.string.action_close))
        }
    }
    Text(
        text = stringResource(R.string.my_dac_editor_local_only),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EditorMain(
    state: MyDacEditorUiState,
    onSelectBand: (Int) -> Unit,
    onShowAllBands: () -> Unit,
    onShowReview: () -> Unit,
    onUpdateBand: (Int, EqFilterType, Double, Double, Double) -> Unit,
    onUseSafeGain: () -> Unit,
    onResetEdits: () -> Unit,
) {
    val working = requireNotNull(state.workingCopy)
    val curve = working.responseCurve
    if (curve == null) {
        Text(
            text = stringResource(R.string.my_dac_response_unavailable),
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        Text(
            text = stringResource(R.string.my_dac_eq_response),
            fontWeight = FontWeight.SemiBold,
        )
        DacEqResponseGraph(
            curve = curve,
            filters = working.filters,
            selectedBandIndex = state.selectedBandIndex,
            onBandSelected = onSelectBand,
            expanded = true,
            accessibilityDescription = stringResource(
                R.string.my_dac_editor_graph_accessibility,
                working.filters.count { it.enabled && kotlin.math.abs(it.gainDb) > 1e-9 },
                curve.minimumGainDb,
                curve.maximumGainDb,
            ),
        )
    }

    Text(
        text = stringResource(R.string.my_dac_editor_select_band),
        fontWeight = FontWeight.SemiBold,
    )
    BandChips(
        filters = working.filters,
        selectedBandIndex = state.selectedBandIndex,
        onSelectBand = onSelectBand,
    )

    state.selectedBand?.let { selected ->
        SelectedBandEditor(
            filter = selected,
            onUpdate = onUpdateBand,
        )
    }

    HeadroomCard(working = working, onUseSafeGain = onUseSafeGain)
    EditorIssues(working)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onShowAllBands, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.my_dac_editor_all_bands))
        }
        TextButton(
            onClick = onResetEdits,
            enabled = working.hasChanges,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.my_dac_editor_reset_edits))
        }
    }

    Button(
        onClick = onShowReview,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.my_dac_editor_review_changes))
    }
}

@Composable
private fun BandChips(
    filters: List<HardwareEqFilter>,
    selectedBandIndex: Int?,
    onSelectBand: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.sortedBy(HardwareEqFilter::index).forEach { filter ->
            FilterChip(
                selected = filter.index == selectedBandIndex,
                onClick = { onSelectBand(filter.index) },
                label = { Text((filter.index + 1).toString()) },
            )
        }
    }
}

@Composable
private fun SelectedBandEditor(
    filter: HardwareEqFilter,
    onUpdate: (Int, EqFilterType, Double, Double, Double) -> Unit,
) {
    var frequencyText by remember(filter.index, filter.frequencyHz) {
        mutableStateOf(editableNumber(filter.frequencyHz))
    }
    var gainText by remember(filter.index, filter.gainDb) {
        mutableStateOf(editableNumber(filter.gainDb))
    }
    var qText by remember(filter.index, filter.q) {
        mutableStateOf(editableNumber(filter.q))
    }

    fun publishIfValid(type: EqFilterType = filter.type) {
        val frequency = frequencyText.toFinitePositiveDoubleOrNull() ?: return
        val gain = gainText.toFiniteDoubleOrNull() ?: return
        val q = qText.toFinitePositiveDoubleOrNull() ?: return
        onUpdate(filter.index, type, frequency, gain, q)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.my_dac_editor_band, filter.index + 1),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    R.string.my_dac_editor_selected_band_summary,
                    filterTypeLabel(filter.type),
                    filter.frequencyHz,
                    filter.gainDb,
                    filter.q,
                ),
            )

            Text(
                text = stringResource(R.string.my_dac_editor_type),
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    EqFilterType.PEAK,
                    EqFilterType.LOW_SHELF,
                    EqFilterType.HIGH_SHELF,
                ).forEach { candidate ->
                    FilterChip(
                        selected = filter.type == candidate,
                        onClick = { publishIfValid(candidate) },
                        label = { Text(filterTypeLabel(candidate)) },
                    )
                }
            }

            OutlinedTextField(
                value = frequencyText,
                onValueChange = { value ->
                    frequencyText = value
                    publishIfValid()
                },
                label = { Text(stringResource(R.string.my_dac_editor_frequency)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = gainText,
                onValueChange = { value ->
                    gainText = value
                    publishIfValid()
                },
                label = { Text(stringResource(R.string.my_dac_editor_gain)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = qText,
                onValueChange = { value ->
                    qText = value
                    publishIfValid()
                },
                label = { Text(stringResource(R.string.my_dac_editor_q)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeadroomCard(
    working: HardwareEqEditWorkingCopy,
    onUseSafeGain: () -> Unit,
) {
    val assessment = working.headroomAssessment ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.my_dac_editor_headroom_title),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when (assessment.status) {
                    DacHeadroomStatus.SAFE -> stringResource(R.string.my_dac_editor_headroom_safe)
                    DacHeadroomStatus.ADJUSTMENT_REQUIRED ->
                        stringResource(R.string.my_dac_editor_headroom_adjustment_required)
                    DacHeadroomStatus.DEVICE_LIMITED ->
                        stringResource(R.string.my_dac_editor_headroom_device_limited)
                },
                color = if (assessment.status == DacHeadroomStatus.SAFE) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(stringResource(R.string.my_dac_editor_headroom_required, assessment.requiredGainDb))
            val planned = working.plannedHeadroomGainDb
            Text(
                if (planned == null) {
                    stringResource(R.string.my_dac_editor_headroom_none)
                } else {
                    stringResource(R.string.my_dac_editor_headroom_planned, planned)
                },
            )
            Text(
                text = stringResource(R.string.my_dac_editor_headroom_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (assessment.status == DacHeadroomStatus.ADJUSTMENT_REQUIRED) {
                Button(onClick = onUseSafeGain, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.my_dac_editor_use_safe_gain))
                }
            }
        }
    }
}

@Composable
private fun EditorIssues(working: HardwareEqEditWorkingCopy) {
    if (working.issues.isEmpty()) return

    working.issues.filter { issue -> issue.severity == HardwareEqEditIssueSeverity.BLOCKING }.forEach { issue ->
        Text(
            text = editIssueText(issue),
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (working.cautions.isNotEmpty()) {
        Text(
            text = stringResource(R.string.my_dac_editor_caution_title),
            fontWeight = FontWeight.SemiBold,
        )
        working.cautions.forEach { issue ->
            Text(editIssueText(issue))
        }
    }
}

@Composable
private fun AllBands(
    working: HardwareEqEditWorkingCopy,
    selectedBandIndex: Int?,
    onSelectBand: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.my_dac_editor_all_bands),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onBack, enabled = selectedBandIndex != null) {
            Text(stringResource(R.string.my_dac_editor_back_to_editor))
        }
    }

    working.filters.sortedBy(HardwareEqFilter::index).forEach { filter ->
        TextButton(
            onClick = { onSelectBand(filter.index) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    R.string.my_dac_editor_band_summary,
                    filter.index + 1,
                    filterTypeLabel(filter.type),
                    filter.frequencyHz,
                    filter.gainDb,
                    filter.q,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun ReviewChanges(
    state: MyDacEditorUiState,
    onBack: () -> Unit,
    onApply: (Boolean) -> Unit,
) {
    val working = requireNotNull(state.workingCopy)
    val isApplying = state.applyStatus == MyDacEditorApplyStatus.APPLYING
    val confirmationRequired = state.applyStatus == MyDacEditorApplyStatus.CONFIRMATION_REQUIRED
    val canApply = working.hasChanges &&
        !working.hasBlockingIssues &&
        working.headroomAssessment?.status == DacHeadroomStatus.SAFE &&
        !isApplying

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.my_dac_editor_review_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onBack, enabled = !isApplying) {
            Text(stringResource(R.string.my_dac_editor_back_to_editor))
        }
    }

    val changeCount = working.differences.size + if (working.headroomPlanChanged) 1 else 0
    if (changeCount == 0) {
        Text(stringResource(R.string.my_dac_editor_no_changes))
    } else {
        Text(stringResource(R.string.my_dac_editor_change_count, changeCount))
        working.differences.forEach { difference ->
            Text(reviewDifferenceText(difference))
        }
        if (working.headroomPlanChanged) {
            Text(
                stringResource(
                    R.string.my_dac_editor_headroom_change,
                    working.baselineHeadroomGainDb ?: 0.0,
                    working.plannedHeadroomGainDb ?: 0.0,
                ),
            )
        }
    }

    EditorIssues(working)
    if (working.hasBlockingIssues) {
        Text(
            text = stringResource(R.string.my_dac_editor_blocked),
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (confirmationRequired) {
        Text(
            text = stringResource(R.string.my_dac_editor_apply_caution_confirmation),
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (isApplying) {
        Text(
            text = stringResource(R.string.my_dac_editor_applying),
            fontWeight = FontWeight.SemiBold,
        )
    }

    Button(
        onClick = { onApply(confirmationRequired) },
        enabled = canApply,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (confirmationRequired) {
                    R.string.my_dac_editor_apply_anyway
                } else {
                    R.string.my_dac_editor_apply
                },
            ),
        )
    }
    Text(
        text = stringResource(R.string.my_dac_editor_apply_explanation),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun editorErrorText(error: MyDacEditorError): String = stringResource(
    when (error) {
        MyDacEditorError.NOT_CONNECTED -> R.string.my_dac_editor_requires_connection
        MyDacEditorError.READ_FAILED -> R.string.my_dac_editor_read_failed
        MyDacEditorError.WRONG_DEVICE -> R.string.my_dac_editor_wrong_device
    },
)

@Composable
private fun filterTypeLabel(type: EqFilterType): String = when (type) {
    EqFilterType.PEAK -> stringResource(R.string.my_dac_editor_type_peak)
    EqFilterType.LOW_SHELF -> stringResource(R.string.my_dac_editor_type_low_shelf)
    EqFilterType.HIGH_SHELF -> stringResource(R.string.my_dac_editor_type_high_shelf)
    else -> type.name
}

@Composable
private fun reviewDifferenceText(difference: HardwareEqDifference): String {
    val band = difference.bandIndex?.plus(1) ?: 0
    return stringResource(
        R.string.my_dac_editor_change_line,
        band,
        differenceFieldLabel(difference.field),
        difference.expectedValue,
        difference.actualValue,
    )
}

@Composable
private fun differenceFieldLabel(field: HardwareEqDifferenceField): String = stringResource(
    when (field) {
        HardwareEqDifferenceField.ENABLED -> R.string.my_dac_editor_field_enabled
        HardwareEqDifferenceField.FILTER_TYPE -> R.string.my_dac_editor_field_filter_type
        HardwareEqDifferenceField.FREQUENCY_HZ -> R.string.my_dac_editor_field_frequency
        HardwareEqDifferenceField.GAIN_DB -> R.string.my_dac_editor_field_gain
        HardwareEqDifferenceField.Q -> R.string.my_dac_editor_field_q
        HardwareEqDifferenceField.DEDICATED_EQ_PREAMP_DB -> R.string.my_dac_editor_field_preamp
    },
)

@Composable
private fun editIssueText(issue: HardwareEqEditIssue): String = when (issue) {
    is HardwareEqEditIssue.BandCountMismatch ->
        stringResource(R.string.my_dac_editor_band_count_blocked)
    is HardwareEqEditIssue.UnsupportedFilterType -> stringResource(
        R.string.my_dac_editor_unsupported_type,
        issue.bandIndex + 1,
    )
    is HardwareEqEditIssue.OutOfRange -> stringResource(
        R.string.my_dac_editor_out_of_range,
        issue.bandIndex + 1,
        differenceFieldLabel(issue.field),
    )
    is HardwareEqEditIssue.NotRepresentableAtStep -> stringResource(
        R.string.my_dac_editor_step_invalid,
        issue.bandIndex + 1,
        differenceFieldLabel(issue.field),
    )
    is HardwareEqEditIssue.GainOutsideNormalRange -> stringResource(
        R.string.my_dac_editor_outside_normal_gain,
        issue.bandIndex + 1,
        issue.normalRange.minimum,
        issue.normalRange.maximum,
    )
    HardwareEqEditIssue.ResponseUnavailable ->
        stringResource(R.string.my_dac_editor_response_blocked)
}

private fun editableNumber(value: Double): String = when {
    value == value.toLong().toDouble() -> value.toLong().toString()
    else -> String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
}

private fun String.toFiniteDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf(Double::isFinite)

private fun String.toFinitePositiveDoubleOrNull(): Double? =
    toFiniteDoubleOrNull()?.takeIf { value -> value > 0.0 }
