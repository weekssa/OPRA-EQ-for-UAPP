package com.weekssa.opraeqforuapp.data.kt02h20

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FiioJa11DeviceIdentityTest {
    @Test
    fun physicalIdentityIgnoresPidAndSelectedHidInterfaceAcrossRestart() {
        val micOn =
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=3"
        val micOff =
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=2"

        assertThat(fiioJa11PhysicalIdentityKey(micOn))
            .isEqualTo("vid=2972|manufacturer=FiiO|product=JA11|serial=fixture-only")
        assertThat(fiioJa11PhysicalIdentityKey(micOff))
            .isEqualTo(fiioJa11PhysicalIdentityKey(micOn))
    }

    @Test
    fun physicalIdentityAlsoSurvivesUacPidChange() {
        val uac1 =
            "vid=2972|pid=101|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=3"
        val uac2 =
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-only|interface=3"

        assertThat(fiioJa11PhysicalIdentityKey(uac1))
            .isEqualTo(fiioJa11PhysicalIdentityKey(uac2))
    }

    @Test
    fun physicalIdentityStillRejectsAnotherSerialManufacturerOrProduct() {
        val original =
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-one|interface=3"
        val otherSerial =
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=fixture-two|interface=2"
        val otherManufacturer =
            "vid=2972|pid=102|manufacturer=Other|product=JA11|serial=fixture-one|interface=2"
        val otherProduct =
            "vid=2972|pid=102|manufacturer=FiiO|product=Other|serial=fixture-one|interface=2"

        assertThat(fiioJa11PhysicalIdentityKey(original))
            .isNotEqualTo(fiioJa11PhysicalIdentityKey(otherSerial))
        assertThat(fiioJa11PhysicalIdentityKey(original))
            .isNotEqualTo(fiioJa11PhysicalIdentityKey(otherManufacturer))
        assertThat(fiioJa11PhysicalIdentityKey(original))
            .isNotEqualTo(fiioJa11PhysicalIdentityKey(otherProduct))
    }

    @Test
    fun physicalIdentityFailsClosedWhenSerialIsMissingBlankOrAmbiguous() {
        assertThat(fiioJa11PhysicalIdentityKey("vid=2972|pid=102|manufacturer=FiiO|product=JA11|interface=3"))
            .isNull()
        assertThat(fiioJa11PhysicalIdentityKey(
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=|interface=3",
        )).isNull()
        assertThat(fiioJa11PhysicalIdentityKey(
            "vid=2972|pid=102|manufacturer=FiiO|product=JA11|serial=one|serial=two|interface=3",
        )).isNull()
    }
}
