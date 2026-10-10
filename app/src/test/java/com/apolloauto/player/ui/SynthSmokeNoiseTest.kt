package com.apolloauto.player.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures smoke helpers stay in a valid density range (still-frame cloud field).
 * Full GPU/Canvas verification is device-side.
 */
class SynthSmokeNoiseTest {
    @Test
    fun smokeControls_defaultsAreSane() {
        val c = SmokeControls()
        assertTrue(c.density in 0.4f..1.6f)
        assertTrue(c.turbulence in 0.2f..2f)
        assertTrue(c.flowSpeed in 0.2f..2.5f)
        assertTrue(c.bassPressure in 0.2f..2f)
        assertTrue(c.softness in 0.3f..1.8f)
        assertTrue(c.glow in 0.3f..1.8f)
        assertTrue(c.saturation in 0.3f..1.6f)
        assertTrue(c.plumeSize in 0.4f..1.8f)
        assertTrue(c.audioSensitivity in 0.3f..2.2f)
        assertTrue(c.densityContrast in 0.6f..2.2f)
        assertTrue(c.chromaticVibration in 0f..2f)
    }

    @Test
    fun smokePresets_coverRequiredPaletteNames() {
        val titles = SmokeColorPreset.entries.map { it.title }.toSet()
        assertTrue(titles.contains("Neon Vapor"))
        assertTrue(titles.contains("Toxic Dreams"))
        assertTrue(titles.contains("Hades Ash"))
        assertTrue(titles.contains("Rainbow Ether"))
        assertTrue(titles.contains("Ghost Smoke"))
        assertTrue(titles.contains("Blacklight Plume"))
    }
}
