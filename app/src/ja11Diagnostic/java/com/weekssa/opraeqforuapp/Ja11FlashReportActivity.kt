package com.weekssa.opraeqforuapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.EqLibraryUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme

class Ja11FlashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnosticApplication = application as Ja11DiagnosticApplication

        setContent {
            val retainedTrace by diagnosticApplication.retainedFlashReport.collectAsStateWithLifecycle()
            OpraEqTheme(themeMode = ThemeMode.System) {
                Ja11FlashReportContent(
                    trace = retainedTrace,
                    onDismiss = {
                        retainedTrace?.operationId?.let(diagnosticApplication::dismissCompletedFlash)
                        finish()
                    },
                    onShareReadable = {
                        retainedTrace?.let { shareReport(it, Ja11FlashReportFormat.READABLE) }
                    },
                    onShareTechnicalJson = {
                        retainedTrace?.let { shareReport(it, Ja11FlashReportFormat.TECHNICAL_JSON) }
                    },
                )
            }
        }
    }

    private fun shareReport(trace: FiioJa11OperationTrace, format: Ja11FlashReportFormat) {
        startActivity(Intent.createChooser(ja11FlashShareIntent(trace, format), "Share FiiO JA11 report"))
    }
}

internal fun completedJa11FlashTrace(state: EqLibraryUiState): FiioJa11OperationTrace? {
    val completedTrace = (state.fiioJa11OperationStatus as? FiioJa11OperationStatus.Completed)
        ?.trace ?: return null
    return state.fiioJa11OperationTrace?.takeIf { currentTrace ->
        currentTrace.operation == "FLASH" && completedTrace.operation == "FLASH" &&
            currentTrace.operationId == completedTrace.operationId
    }
}

internal enum class Ja11FlashReportFormat {
    READABLE,
    TECHNICAL_JSON,
}

internal fun ja11FlashShareIntent(
    trace: FiioJa11OperationTrace,
    format: Ja11FlashReportFormat,
): Intent = Intent(Intent.ACTION_SEND).apply {
    type = when (format) {
        Ja11FlashReportFormat.READABLE -> "text/plain"
        Ja11FlashReportFormat.TECHNICAL_JSON -> "application/json"
    }
    putExtra(
        Intent.EXTRA_SUBJECT,
        when (format) {
            Ja11FlashReportFormat.READABLE -> "FiiO JA11 operation report"
            Ja11FlashReportFormat.TECHNICAL_JSON -> "FiiO JA11 operation report JSON"
        },
    )
    putExtra(
        Intent.EXTRA_TEXT,
        when (format) {
            Ja11FlashReportFormat.READABLE -> trace.toReadableText()
            Ja11FlashReportFormat.TECHNICAL_JSON -> trace.toJson()
        },
    )
}

@Composable
internal fun Ja11FlashReportContent(
    trace: FiioJa11OperationTrace?,
    onDismiss: () -> Unit,
    onShareReadable: () -> Unit,
    onShareTechnicalJson: () -> Unit,
) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("FiiO JA11 Flash report", style = MaterialTheme.typography.headlineSmall)
            if (trace == null) {
                Text("No completed JA11 Flash report is available in this running app session.")
                TextButton(onClick = onDismiss) { Text("Close") }
            } else {
                Text("This report stays available until you dismiss it.")
                Text("Operation ID: ${trace.operationId}")
                Text("Outcome: ${trace.outcome}")
                Text("State known: ${trace.stateKnown}")
                Text("Save commands: ${trace.saveCommandCount}")
                Text("Final comparison: ${trace.comparisonPhase ?: "Unavailable"}")
                Text("Transport events: ${trace.events.size}")
                Button(onClick = onShareReadable) { Text("Share operation report") }
                Button(onClick = onShareTechnicalJson) { Text("Share technical report (JSON)") }
                TextButton(onClick = onDismiss) { Text("Dismiss report") }
            }
        }
    }
}
