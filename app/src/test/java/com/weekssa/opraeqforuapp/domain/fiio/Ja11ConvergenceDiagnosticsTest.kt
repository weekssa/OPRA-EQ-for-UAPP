package com.weekssa.opraeqforuapp.domain.fiio

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.BuildConfig
import java.util.concurrent.Executors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class Ja11ConvergenceDiagnosticsTest {
    @Test
    fun recorderBindingIsEnabledOnlyInDiagnosticBuild() {
        assertThat(BuildConfig.JA11_CONVERGENCE_DIAGNOSTICS_ENABLED).isEqualTo(BuildConfig.BUILD_TYPE == "ja11Diagnostic")
    }

    @Test
    fun boundedRecorderRetainsLastEventsAndReportsDroppedEvidence() {
        val recorder = Ja11ConvergenceDiagnostics(capacity = 2)
        repeat(4) { recorder.record(event(it.toLong())) }
        val snapshot = recorder.snapshot()
        assertThat(snapshot.droppedRecords).isEqualTo(2)
        assertThat(snapshot.records.map { it.sequence }).containsExactly(3L, 4L).inOrder()
        assertThat(snapshot.records.map { it.event.instanceId }).containsExactly(2L, 3L).inOrder()
    }

    @Test
    fun concurrentEventsKeepOneUnambiguousSequence() {
        val recorder = Ja11ConvergenceDiagnostics()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val jobs = (0 until 4).map {
                executor.submit { repeat(100) { recorder.record(event(recorder.allocateId())) } }
            }
            jobs.forEach { it.get() }
            val snapshot = recorder.snapshot()
            assertThat(snapshot.records.map { it.sequence }).containsExactlyElementsIn((1L..400L).toList()).inOrder()
            assertThat(snapshot.records.map { it.event.instanceId }.distinct()).hasSize(400)
        } finally { executor.shutdownNow() }
    }

    @Test
    fun serializedTraceContainsOnlyStructuralState() {
        val recorder = Ja11ConvergenceDiagnostics()
        recorder.record(event(1).copy(generation = 2, bundlePresent = false, attempt = 1))
        val record = Json.parseToJsonElement(recorder.toJson()).jsonObject
            .getValue("records").jsonArray.single().jsonObject.getValue("event").jsonObject
        assertThat(record.keys).containsExactly("component", "instanceId", "phase", "attempt", "generation", "bundlePresent")
    }

    @Test
    fun arbitraryTextCannotEnterOperationIdField() {
        val recorder = Ja11ConvergenceDiagnostics()
        recorder.record(event(1).copy(operationId = "private serial or EQ text"))
        assertThat(recorder.toJson()).doesNotContain("private serial")
        assertThat(recorder.snapshot().records.single().event.operationId).isNull()
        val id = "01234567-89ab-cdef-0123-456789abcdef"
        recorder.record(event(2).copy(operationId = id))
        assertThat(recorder.snapshot().records.last().event.operationId).isEqualTo(id)
    }

    private fun event(id: Long) = Ja11ConvergenceEvent(
        component = Ja11ConvergenceComponent.HARDWARE_REPOSITORY,
        instanceId = id,
        phase = Ja11ConvergencePhase.READ_NO_BUNDLE,
    )
}
