package com.weekssa.opraeqforuapp.domain.ew300

/** Removes the unit-unique USB serial only from owner-shareable reports; authorization keeps the full fingerprint. */
internal fun String?.redactedForEw300Report(): String? = this?.split('|')?.joinToString("|") { component ->
    val separator = component.indexOf('=')
    if (separator > 0 && component.substring(0, separator).equals("serial", ignoreCase = true)) {
        "${component.substring(0, separator)}=[redacted]"
    } else {
        component
    }
}
