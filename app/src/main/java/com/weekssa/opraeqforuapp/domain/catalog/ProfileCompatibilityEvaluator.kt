package com.weekssa.opraeqforuapp.domain.catalog

import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory

data class ProfileCompatibilityAssessment(
    val category: ProfileCompatibility,
    val reason: String? = null,
)

/**
 * Device-independent gate for whether a catalog row is a usable parametric-EQ source record.
 *
 * This deliberately does not apply any output's frequency/gain/Q/filter-count/preamp limits.
 * A valid canonical curve stays selectable and visible even when the active output later reports
 * Not representable.
 */
fun OpraEqProfile.isUsableParametricSource(): Boolean {
    if (profileType != "parametric_eq") return false
    val sourceBands = bands ?: return false
    if (sourceBands.isEmpty()) return false
    return sourceBands.all { band ->
        !band.type.isNullOrBlank() &&
            band.frequency?.let { it.isFinite() && it > 0.0 } == true &&
            band.gainDb?.isFinite() != false &&
            band.q?.isFinite() != false &&
            band.slope?.isFinite() != false
    }
}

/**
 * Generic catalog/source usability. This is the assessment used by Browse/My EQ selection logic.
 *
 * Output-specific fidelity belongs in assessDeviceExportability; choosing an output must never hide
 * or make an otherwise usable canonical curve unselectable.
 */
fun OpraEqProfile.assessCompatibility(): ProfileCompatibilityAssessment =
    if (isUsableParametricSource()) {
        ProfileCompatibilityAssessment(ProfileCompatibility.FullyCompatible)
    } else {
        notCompatible(
            "This catalog row is not a usable parametric EQ source. EQ Library keeps the source visible but cannot select it until the source data is valid.",
        )
    }

/** UAPP/ToneBoosters-specific compatibility retained only for that output's export details. */
fun OpraEqProfile.assessUappCompatibility(): ProfileCompatibilityAssessment {
    if (profileType != "parametric_eq") {
        return notCompatible("This profile is not a parametric EQ profile.")
    }

    val playbackPreamp = effectivePlaybackPreampDb()
        ?: return notCompatible(
            "This profile has no source preamp and no EQ Library-generated safety headroom for UAPP/ToneBoosters playback.",
        )
    if (playbackPreamp !in GAIN_RANGE) {
        val origin = if (usesEqLibrarySafetyHeadroom()) {
            "EQ Library-generated safety headroom"
        } else {
            "source preamp"
        }
        return notCompatible("The $origin is outside the proven UAPP/ToneBoosters range of -20 dB to +20 dB.")
    }

    val profileBands = bands
        ?: return notCompatible("The profile is missing its parametric EQ band list.")

    profileBands.forEachIndexed { index, band ->
        val bandNumber = index + 1
        val type = band.type
            ?: return notCompatible("Band $bandNumber is missing its filter type.")
        if (type !in SUPPORTED_FILTER_TYPES) {
            return notCompatible(
                "Band $bandNumber uses $type, which the established UAPP/ToneBoosters conversion does not map safely.",
            )
        }

        val frequency = band.frequency
            ?: return notCompatible("Band $bandNumber is missing its frequency.")
        if (frequency !in FREQUENCY_RANGE) {
            return notCompatible("Band $bandNumber is outside the proven 16 Hz to 20 kHz conversion range.")
        }

        val gain = band.gainDb ?: 0.0
        if (gain !in GAIN_RANGE) {
            return notCompatible("Band $bandNumber gain is outside the proven -20 dB to +20 dB conversion range.")
        }

        val q = band.q
            ?: return notCompatible("Band $bandNumber is missing its Q value.")
        if (q !in Q_RANGE) {
            return notCompatible("Band $bandNumber Q is outside the proven 0.1 to 10 conversion range.")
        }
    }

    return if (profileBands.size > MAX_UAPP_BANDS) {
        ProfileCompatibilityAssessment(
            category = ProfileCompatibility.CompatibleWithLimitation,
            reason = "This source has ${profileBands.size} priority-sorted bands. The current UAPP/ToneBoosters target supports 10, so only the first 10 priority bands will be used for that device export.",
        )
    } else {
        ProfileCompatibilityAssessment(ProfileCompatibility.FullyCompatible)
    }
}

fun ProfileCompatibility.visibilityCategory(): ProfileVisibilityCategory = when (this) {
    ProfileCompatibility.FullyCompatible -> ProfileVisibilityCategory.FullyCompatible
    ProfileCompatibility.CompatibleWithLimitation -> ProfileVisibilityCategory.CompatibleWithLimitation
    ProfileCompatibility.NotCompatible -> ProfileVisibilityCategory.NotCompatible
}

private fun notCompatible(reason: String) = ProfileCompatibilityAssessment(
    category = ProfileCompatibility.NotCompatible,
    reason = reason,
)

private val FREQUENCY_RANGE = 16.0..20_000.0
private val GAIN_RANGE = -20.0..20.0
private val Q_RANGE = 0.1..10.0
private const val MAX_UAPP_BANDS = 10
private val SUPPORTED_FILTER_TYPES = setOf("peak_dip", "low_shelf", "high_shelf")
