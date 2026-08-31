package com.weekssa.opraeqforuapp.domain.export

/**
 * Legacy OPRA bridge records keep band type nullable so malformed source data can remain visible.
 * Text exporters reject a missing type rather than guessing one.
 */
internal fun parametricType(type: String?): String? = when (type) {
    "peak_dip" -> "PK"
    "low_shelf" -> "LSC"
    "high_shelf" -> "HSC"
    else -> null
}
