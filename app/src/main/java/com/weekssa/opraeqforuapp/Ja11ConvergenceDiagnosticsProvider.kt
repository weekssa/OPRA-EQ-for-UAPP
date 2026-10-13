package com.weekssa.opraeqforuapp

import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergenceDiagnostics

/** Implemented only by the diagnostic source set's Application. */
internal interface Ja11ConvergenceDiagnosticsProvider {
    val convergenceDiagnostics: Ja11ConvergenceDiagnostics
}
