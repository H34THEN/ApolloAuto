package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualResponseEngineTest {
    @Test
    fun expand_liftsMidSignals() {
        val mid = VisualResponseMapper.expand(0.25f)
        assertTrue(mid > 0.2f)
        assertTrue(VisualResponseMapper.expand(0f) == 0f)
        assertTrue(VisualResponseMapper.expand(1f) <= 1f)
    }

    @Test
    fun kickPressure_favorsBassAndOnset() {
        val bassy = VisualResponseMapper.bandDrive(
            AudioFeatures(subBass = 0.8f, bass = 0.9f, onset = 0.7f),
            VisualIntensity.REACTIVE
        )
        val trebly = VisualResponseMapper.bandDrive(
            AudioFeatures(treble = 0.9f, presence = 0.8f),
            VisualIntensity.REACTIVE
        )
        assertTrue(VisualResponseMapper.kickPressure(bassy) > VisualResponseMapper.kickPressure(trebly))
        assertTrue(VisualResponseMapper.sparkle(trebly) > VisualResponseMapper.sparkle(bassy))
    }

    @Test
    fun envelope_attacksFasterThanRelease() {
        val env = ResponseEnvelope(attack = 0.8f, release = 0.2f)
        val up = env.step(0.1f, 1f)
        val down = env.step(1f, 0.1f)
        assertTrue(up > 0.7f)
        assertTrue(down > 0.7f) // slow release keeps energy
    }

    @Test
    fun spiralViewport_fillUsesMaxSide() {
        assertEquals(1920f, spiralViewportDim(1920f, 1080f, SpiralFillMode.FILL))
        assertEquals(1080f, spiralViewportDim(1920f, 1080f, SpiralFillMode.FIT))
    }

    @Test
    fun safePulse_capsWhenPhotosensitive() {
        assertTrue(VisualResponseMapper.safePulse(1f, photosensitive = true) <= 0.4f)
        assertEquals(1f, VisualResponseMapper.safePulse(1f, photosensitive = false), 0.001f)
    }

    @Test
    fun liquidSpiralPoly_featureShapes_areDistinct() {
        // Regression anchors: strong scenes consume multi-band geometry inputs.
        val f = AudioFeatures(
            subBass = 0.7f, bass = 0.8f, lowMid = 0.5f, upperMid = 0.4f,
            treble = 0.6f, rms = 0.55f, onset = 0.5f
        )
        val d = VisualResponseMapper.bandDrive(f, VisualIntensity.BALANCED)
        assertTrue(d.bass > 0.3f && d.treble > 0.2f && d.onset > 0.2f)
    }
}
