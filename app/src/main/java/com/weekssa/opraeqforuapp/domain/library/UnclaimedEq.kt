package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand

enum class UnclaimedEqFormat(val label: String) {
    PARAMETRIC_TEXT("Parametric EQ text"),
    TONEBOOSTERS_XML("UAPP / ToneBoosters XML"),
    UNKNOWN("Unknown EQ format"),
}

enum class UnclaimedEqParseState {
    RECOVERABLE,
    INVALID,
    UNSUPPORTED,
    SOURCE_UNVERIFIED,
    ACCESS_UNAVAILABLE,
}

data class UnclaimedEqParsedContent(
    val suggestedName: String?,
    val preampGainDb: Double?,
    val bands: List<OpraBand>,
)

data class UnclaimedEqRecord(
    val documentUri: String,
    val originalFileName: String,
    val relativeDirectory: String,
    val previousProfileId: String,
    val previousProductId: String,
    val format: UnclaimedEqFormat,
    val parseState: UnclaimedEqParseState,
    val reason: String,
    val parseMessage: String?,
    val parsedContent: UnclaimedEqParsedContent?,
    val exportedAtMillis: Long,
) {
    val isRecoverable: Boolean
        get() = parseState == UnclaimedEqParseState.RECOVERABLE && parsedContent != null
}
