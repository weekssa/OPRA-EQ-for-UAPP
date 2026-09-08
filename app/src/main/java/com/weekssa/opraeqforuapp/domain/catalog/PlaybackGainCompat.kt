package com.weekssa.opraeqforuapp.domain.catalog

/**
 * Package-level form used by output modules that explicitly import the playback-gain rule.
 * The OpraEqProfile member remains the authoritative API; this extension intentionally mirrors it.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun OpraEqProfile.effectivePlaybackPreampDb(): Double? = preampGainDb ?: eqLibrarySafetyHeadroomDb
