package com.weekssa.opraeqforuapp

import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceDiagnostics
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceEvent
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceComponent
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergencePhase
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import java.io.File
import java.util.WeakHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceSnapshot
import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.ui.EqLibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** Diagnostic-build-only access to the already-running application state and last Flash report. */
internal class Ja11DiagnosticApplication : Application(), Application.ActivityLifecycleCallbacks,
    Ja11ConvergenceDiagnosticsProvider {
    override val convergenceDiagnostics = Ja11ConvergenceDiagnostics()
    private val activityIds = WeakHashMap<Activity, Long>()

    private fun recordActivity(activity: Activity, phase: Ja11ConvergencePhase, viewModelId: Long? = null) {
        if (activity !is MainActivity) return
        val id = activityIds.getOrPut(activity) { convergenceDiagnostics.allocateId() }
        convergenceDiagnostics.record(Ja11ConvergenceEvent(
            Ja11ConvergenceComponent.ACTIVITY, id, phase, relatedInstanceId = viewModelId,
        ))
    }

    internal fun exportConvergenceTrace(): File = File(filesDir, "ja11-convergence-trace.json").apply {
        writeText(convergenceReportJson())
    }
    @Volatile
    private var mainActivityReference: WeakReference<MainActivity>? = null
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var flashObservationJob: Job? = null

    private val dismissedFlashOperationIds = mutableSetOf<String>()
    private val mutableRetainedFlashReport = MutableStateFlow<FiioJa11OperationTrace?>(null)
    internal val retainedFlashReport: StateFlow<FiioJa11OperationTrace?> =
        mutableRetainedFlashReport.asStateFlow()

    internal fun convergenceReportJson(): String = Json { prettyPrint = true }.encodeToString(
        Ja11ConvergenceReport(
            sourceSha = BuildConfig.CANDIDATE_SOURCE_SHA, packageId = BuildConfig.APPLICATION_ID,
            versionName = BuildConfig.VERSION_NAME, versionCode = BuildConfig.VERSION_CODE,
            snapshot = convergenceDiagnostics.snapshot(),
        ),
    )

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    @Synchronized
    internal fun retainCompletedFlash(trace: FiioJa11OperationTrace) {
        if (trace.operation != "FLASH") return
        if (trace.operationId in dismissedFlashOperationIds) return
        if (mutableRetainedFlashReport.value != null) return
        mutableRetainedFlashReport.value = trace
    }

    @Synchronized
    internal fun dismissCompletedFlash(operationId: String) {
        dismissedFlashOperationIds += operationId
        if (mutableRetainedFlashReport.value?.operationId == operationId) {
            mutableRetainedFlashReport.value = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        recordActivity(activity, Ja11ConvergencePhase.CREATED)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is MainActivity || activity.isFinishing || activity.isDestroyed) return
        mainActivityReference = WeakReference(activity)
        flashObservationJob?.cancel()
        val viewModel = ViewModelProvider(activity)[EqLibraryViewModel::class.java]
        recordActivity(activity, Ja11ConvergencePhase.RESUMED, viewModel.diagnosticInstanceId)
        flashObservationJob = observationScope.launch {
            viewModel.uiState.collect { state ->
                completedJa11FlashTrace(state)?.let(::retainCompletedFlash)
                val eq = state.fiioJa11HardwareEqState
                val operationId = when (val status = state.fiioJa11OperationStatus) {
                    is com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus.Running -> status.operationId
                    is com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus.Completed -> status.trace.operationId
                    com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus.Idle -> null
                }
                convergenceDiagnostics.record(Ja11ConvergenceEvent(
                    Ja11ConvergenceComponent.VIEW_MODEL, viewModel.diagnosticInstanceId,
                    Ja11ConvergencePhase.UI_ADOPTION,
                    operationId = operationId,
                    generation = state.fiioJa11DeviceState.snapshot?.sessionGeneration,
                    bundleGeneration = eq.bundle?.snapshot?.sessionGeneration,
                    bundlePresent = eq.bundle != null, bundleCurrent = eq.freshness == DacStateFreshness.CURRENT,
                    reading = eq.isReading, readFailed = eq.readFailed,
                    deviceCurrent = state.fiioJa11DeviceState.isCurrentSession,
                ))
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        recordActivity(activity, Ja11ConvergencePhase.DESTROYED)
        if (activity is MainActivity && mainActivityReference?.get() === activity) {
            flashObservationJob?.cancel()
            flashObservationJob = null
            mainActivityReference = null
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) { recordActivity(activity, Ja11ConvergencePhase.PAUSED) }
    override fun onActivityStopped(activity: Activity) { recordActivity(activity, Ja11ConvergencePhase.STOPPED) }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

@Serializable
internal data class Ja11ConvergenceReport(
    val sourceSha: String,
    val packageId: String,
    val versionName: String,
    val versionCode: Int,
    val snapshot: Ja11ConvergenceSnapshot,
)
