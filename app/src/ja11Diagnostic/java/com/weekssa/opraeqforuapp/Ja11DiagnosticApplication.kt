package com.weekssa.opraeqforuapp

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
internal class Ja11DiagnosticApplication : Application(), Application.ActivityLifecycleCallbacks {
    @Volatile
    private var mainActivityReference: WeakReference<MainActivity>? = null
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var flashObservationJob: Job? = null

    private val dismissedFlashOperationIds = mutableSetOf<String>()
    private val mutableRetainedFlashReport = MutableStateFlow<FiioJa11OperationTrace?>(null)
    internal val retainedFlashReport: StateFlow<FiioJa11OperationTrace?> =
        mutableRetainedFlashReport.asStateFlow()

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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) {
        if (activity !is MainActivity || activity.isFinishing || activity.isDestroyed) return
        mainActivityReference = WeakReference(activity)
        flashObservationJob?.cancel()
        val viewModel = ViewModelProvider(activity)[EqLibraryViewModel::class.java]
        flashObservationJob = observationScope.launch {
            viewModel.uiState.collect { state ->
                completedJa11FlashTrace(state)?.let(::retainCompletedFlash)
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity && mainActivityReference?.get() === activity) {
            flashObservationJob?.cancel()
            flashObservationJob = null
            mainActivityReference = null
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
