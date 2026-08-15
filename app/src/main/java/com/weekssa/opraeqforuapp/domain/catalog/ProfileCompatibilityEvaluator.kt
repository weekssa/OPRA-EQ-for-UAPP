package com.weekssa.opraeqforuapp.domain.catalog

import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory

data class ProfileCompatibilityAssessment(
    val category: ProfileCompatibility,
    val reason: String? = null,
)

fun OpraEqProfile.assessCompatibility(): ProfileCompatibilityAssessment {
    if (profileType != "parametric_eq") {
        return notCompatible("This OPRA profile is not a parametric EQ profile.")
    }

    val preamp = preampGainDb
        ?: return notCompatible("The OPRA profile is missing its overall gain value.")
    if (preamp !in GAIN_RANGE) {
        return notCompatible("The OPRA preamp is outside the proven UAPP/ToneBoosters range of -20 dB to +20 dB.")
    }

    val profileBands = bands
        ?: return notCompatible("The OPRA profile is missing its parametric EQ band list.")

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
            reason = "OPRA has ${profileBands.size} priority-sorted bands. UAPP/ToneBoosters is limited to 10, so only the first 10 priority bands will be used.",
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
