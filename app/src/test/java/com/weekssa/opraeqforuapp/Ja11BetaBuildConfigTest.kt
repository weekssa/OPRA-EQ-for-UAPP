package com.weekssa.opraeqforuapp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Ja11BetaBuildConfigTest {
    @Test
    fun persistencePolicyAndInstallerIdentityAreScopedToThePrivateBetaVariant() {
        val isBeta = BuildConfig.BUILD_TYPE == "ja11Beta"

        assertThat(BuildConfig.JA11_SETTINGS_PERSISTENCE_BETA_ENABLED).isEqualTo(isBeta)
        assertThat(BuildConfig.APPLICATION_ID.endsWith(".ja11beta")).isEqualTo(isBeta)
        if (isBeta) {
            assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.weekssa.opraeqforuapp.ja11beta")
            assertThat(BuildConfig.VERSION_NAME).isEqualTo("0.8.2-ja11beta")
            assertThat(BuildConfig.VERSION_CODE).isEqualTo(13)
        } else {
            assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.weekssa.opraeqforuapp")
            assertThat(BuildConfig.VERSION_NAME).isEqualTo("0.8.2")
        }
    }
}
