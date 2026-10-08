package com.weekssa.opraeqforuapp.data.kt02h20

/**
 * Returns the JA11 physical identity shared across expected USB re-enumeration.
 * PID and selected HID interface can both change while the same device restarts.
 */
internal fun fiioJa11PhysicalIdentityKey(fingerprintKey: String?): String? {
    val fields = fingerprintKey?.split('|') ?: return null
    val serialFields = fields.filter { it.substringBefore('=') == "serial" }
    if (serialFields.size != 1 || serialFields.single().substringAfter('=', "").isBlank()) return null
    return fields
        .filterNot { field ->
            val name = field.substringBefore('=')
            name == "pid" || name == "interface"
        }
        .joinToString("|")
        .takeIf(String::isNotBlank)
}
