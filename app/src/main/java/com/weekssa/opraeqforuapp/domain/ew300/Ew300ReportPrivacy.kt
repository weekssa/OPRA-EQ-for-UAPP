package com.weekssa.opraeqforuapp.domain.ew300

/** Removes the unit-unique USB serial from owner-shareable reports, never from authorization. */
internal fun String?.redactedForEw300Report(fingerprintKey: String? = null): String? {
    if (this == null) return null
    val serial = fingerprintKey
        ?.split('|')
        ?.firstOrNull { it.substringBefore('=').equals("serial", ignoreCase = true) }
        ?.substringAfter('=', "")
        ?.takeIf { it.isNotBlank() }
    val valueWithoutKnownSerial = if (serial == null) this else replace(serial, "[redacted]")
    val serialAssignment = Regex("(?i)(serial\\s*[=:]\\s*)[^|,;\\s]+")
    return serialAssignment.replace(valueWithoutKnownSerial) { match ->
        "${match.groupValues[1]}[redacted]"
    }
}
