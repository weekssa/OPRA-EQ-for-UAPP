package com.weekssa.opraeqforuapp.domain.kt02h20

/**
 * JA11 mutation pacing derived from the independently documented device command interval.
 *
 * A completed USB transfer only proves that Android accepted the report. The device needs a
 * bounded settle interval before the next command can safely be issued or verified.
 */
internal object FiioJa11Timing {
    const val MUTATION_SETTLE_MILLIS = 200L

    fun settleMillisForMutation(report: ByteArray): Long {
        require(report.size > 5) { "JA11 mutation report is missing its command byte." }
        return MUTATION_SETTLE_MILLIS
    }
}
