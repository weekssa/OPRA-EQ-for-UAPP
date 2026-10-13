package com.weekssa.opraeqforuapp

import android.app.Application
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceDiagnostics
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = ConvergenceProviderTestApplication::class)
class Ja11ConvergenceBindingTest {
    @Test
    fun compositionRootIgnoresProviderInReleaseAndDebugButBindsDiagnostic() {
        val application = RuntimeEnvironment.getApplication() as ConvergenceProviderTestApplication
        val dependencies = createEqLibraryDependencies(application)
        try {
            if (BuildConfig.BUILD_TYPE == "ja11Diagnostic") {
                assertThat(dependencies.hardwareRepository.convergenceDiagnostics)
                    .isSameInstanceAs(application.convergenceDiagnostics)
            } else {
                assertThat(dependencies.hardwareRepository.convergenceDiagnostics).isNull()
            }
        } finally { dependencies.hardwareRepository.close() }
    }
}

class ConvergenceProviderTestApplication : Application(), Ja11ConvergenceDiagnosticsProvider {
    override val convergenceDiagnostics = Ja11ConvergenceDiagnostics()
}
