package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseCurve
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Presentation of a domain-evaluated device-native PEQ response.
 *
 * The compact usage remains read-only. The editor may opt into tap-only marker selection through
 * [onBandSelected]. There is deliberately no drag gesture and no hardware callback; response/DSP
 * authority remains in [HardwareEqResponseEvaluator], and a tap can only select a local band.
 */
@Composable
internal fun DacEqResponseGraph(
    curve: HardwareEqResponseCurve,
    filters: List<HardwareEqFilter>,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
    selectedBandIndex: Int? = null,
    onBandSelected: ((Int) -> Unit)? = null,
    expanded: Boolean = false,
) {
    val curveColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.primary
    val selectedMarkerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val selectedMarkerRingColor = MaterialTheme.colorScheme.primaryContainer
    val zeroReferenceColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val verticalBoundDb = maxOf(
        MIN_DISPLAY_BOUND_DB,
        ceil(curve.peakAbsoluteGainDb / DISPLAY_BOUND_STEP_DB) * DISPLAY_BOUND_STEP_DB,
    )
    val activeFilters = filters.filter(HardwareEqFilter::isAcousticallyActive)
    val graphHeight = if (expanded) EXPANDED_GRAPH_HEIGHT else GRAPH_HEIGHT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(graphHeight)
                .then(
                    if (onBandSelected == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(activeFilters, curve, verticalBoundDb) {
                            detectTapGestures { tap ->
                                if (activeFilters.isEmpty()) return@detectTapGestures
                                val widthPx = size.width.toFloat()
                                val heightPx = size.height.toFloat()
                                val nearest = activeFilters.minByOrNull { filter ->
                                    val markerGainDb = curve.gainDbAt(filter.frequencyHz) ?: 0.0
                                    val marker = Offset(
                                        x = (logarithmicFraction(filter.frequencyHz) * widthPx).toFloat(),
                                        y = yForGain(markerGainDb, verticalBoundDb, heightPx),
                                    )
                                    distance(marker, tap)
                                }
                                nearest?.let { filter -> onBandSelected(filter.index) }
                            }
                        }
                    },
                ),
        ) {
            fun xFor(frequencyHz: Double): Float {
                val fraction = logarithmicFraction(frequencyHz)
                return (fraction * size.width).toFloat()
            }

            fun yFor(gainDb: Double): Float = yForGain(gainDb, verticalBoundDb, size.height)

            GRID_FREQUENCIES_HZ.forEach { frequencyHz ->
                val x = xFor(frequencyHz)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = GRID_STROKE_WIDTH.toPx(),
                )
            }

            val zeroY = yFor(0.0)
            drawLine(
                color = zeroReferenceColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = ZERO_STROKE_WIDTH.toPx(),
            )

            val responsePath = Path()
            curve.points.forEachIndexed { index, point ->
                val x = xFor(point.frequencyHz)
                val y = yFor(point.gainDb)
                if (index == 0) responsePath.moveTo(x, y) else responsePath.lineTo(x, y)
            }
            drawPath(
                path = responsePath,
                color = curveColor,
                style = Stroke(
                    width = CURVE_STROKE_WIDTH.toPx(),
                    cap = StrokeCap.Round,
                ),
            )

            activeFilters.forEach { filter ->
                val markerGainDb = curve.gainDbAt(filter.frequencyHz) ?: return@forEach
                val center = Offset(
                    x = xFor(filter.frequencyHz),
                    y = yFor(markerGainDb),
                )
                if (filter.index == selectedBandIndex) {
                    drawCircle(
                        color = selectedMarkerRingColor,
                        radius = SELECTED_MARKER_RING_RADIUS.toPx(),
                        center = center,
                    )
                    drawCircle(
                        color = selectedMarkerColor,
                        radius = SELECTED_MARKER_RADIUS.toPx(),
                        center = center,
                    )
                } else {
                    drawCircle(
                        color = markerColor,
                        radius = MARKER_RADIUS.toPx(),
                        center = center,
                    )
                }
            }
        }

        FrequencyLabels()
    }
}

@Composable
private fun FrequencyLabels() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(LABEL_HEIGHT),
    ) {
        FREQUENCY_LABELS.forEach { label ->
            val fraction = logarithmicFraction(label.frequencyHz).toFloat()
            Box(
                modifier = Modifier
                    .width(LABEL_WIDTH)
                    .offset(x = (maxWidth - LABEL_WIDTH) * fraction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun logarithmicFraction(frequencyHz: Double): Double {
    val minimum = HardwareEqResponseEvaluator.MIN_FREQUENCY_HZ
    val maximum = HardwareEqResponseEvaluator.MAX_FREQUENCY_HZ
    val bounded = frequencyHz.coerceIn(minimum, maximum)
    return (ln(bounded / minimum) / ln(maximum / minimum)).coerceIn(0.0, 1.0)
}

private fun yForGain(gainDb: Double, verticalBoundDb: Double, heightPx: Float): Float {
    val fraction = ((verticalBoundDb - gainDb) / (verticalBoundDb * 2.0)).coerceIn(0.0, 1.0)
    return (fraction * heightPx).toFloat()
}

private fun distance(left: Offset, right: Offset): Float = sqrt(
    (left.x - right.x).pow(2) + (left.y - right.y).pow(2),
)

private data class FrequencyLabel(
    val frequencyHz: Double,
    val text: String,
)

private val FREQUENCY_LABELS = listOf(
    FrequencyLabel(20.0, "20"),
    FrequencyLabel(100.0, "100"),
    FrequencyLabel(1_000.0, "1k"),
    FrequencyLabel(10_000.0, "10k"),
)

private val GRID_FREQUENCIES_HZ = FREQUENCY_LABELS.map(FrequencyLabel::frequencyHz)

private val GRAPH_HEIGHT = 160.dp
private val EXPANDED_GRAPH_HEIGHT = 220.dp
private val LABEL_HEIGHT = 24.dp
private val LABEL_WIDTH = 40.dp
private val GRID_STROKE_WIDTH = 1.dp
private val ZERO_STROKE_WIDTH = 1.5.dp
private val CURVE_STROKE_WIDTH = 2.5.dp
private val MARKER_RADIUS = 4.dp
private val SELECTED_MARKER_RADIUS = 5.dp
private val SELECTED_MARKER_RING_RADIUS = 9.dp
private const val MIN_DISPLAY_BOUND_DB = 6.0
private const val DISPLAY_BOUND_STEP_DB = 3.0
