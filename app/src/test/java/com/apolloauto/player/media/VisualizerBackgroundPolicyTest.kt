package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerBackgroundPolicyTest {
    @Test
    fun auto_prefersBlackForRainbowScenes() {
        val modes = listOf(
            VisualizerMode.SPECTRUM,
            VisualizerMode.CHROMATIC_PHI,
            VisualizerMode.AUDIBLE_SPIRALS,
            VisualizerMode.CIRC_INFINITY,
            VisualizerMode.ECTOPLASM
        )
        modes.forEach { mode ->
            val eff = VisualizerBackgroundPolicy.resolve(VisualizerBackgroundMode.AUTO, mode)
            assertEquals(SceneBackgroundPreference.BLACK, eff.preference)
            assertFalse(eff.showAlbumArt)
            assertTrue(eff.clearSceneBackdrop)
        }
    }

    @Test
    fun auto_usesSceneDefinedForAtmosphericWorlds() {
        val modes = listOf(
            VisualizerMode.NEBULA,
            VisualizerMode.OBSIDIAN_REACTOR,
            VisualizerMode.FLUCTUS_FLOW,
            VisualizerMode.SYNTH_SMOKE
        )
        modes.forEach { mode ->
            val eff = VisualizerBackgroundPolicy.resolve(VisualizerBackgroundMode.AUTO, mode)
            assertEquals(SceneBackgroundPreference.SCENE_DEFINED, eff.preference)
            assertFalse(eff.showAlbumArt)
        }
    }

    @Test
    fun userBlack_overridesAtmosphericAuto() {
        val eff = VisualizerBackgroundPolicy.resolve(
            VisualizerBackgroundMode.BLACK,
            VisualizerMode.NEBULA
        )
        assertFalse(eff.showAlbumArt)
        assertTrue(eff.clearSceneBackdrop)
    }

    @Test
    fun userAlbumArt_forcesArtEvenOnSpirals() {
        val eff = VisualizerBackgroundPolicy.resolve(
            VisualizerBackgroundMode.ALBUM_ART,
            VisualizerMode.AUDIBLE_SPIRALS
        )
        assertTrue(eff.showAlbumArt)
        assertTrue(eff.clearSceneBackdrop)
    }

    @Test
    fun userScene_keepsAtmosphereWithoutAlbumArt() {
        val eff = VisualizerBackgroundPolicy.resolve(
            VisualizerBackgroundMode.SCENE,
            VisualizerMode.SYNTH_SMOKE
        )
        assertFalse(eff.showAlbumArt)
        assertFalse(eff.clearSceneBackdrop)
        assertEquals(SceneBackgroundPreference.SCENE_DEFINED, eff.preference)
    }

    @Test
    fun sceneOverride_beatsGlobalAlbum() {
        val eff = VisualizerBackgroundPolicy.resolve(
            VisualizerBackgroundMode.ALBUM_ART,
            VisualizerMode.CHROMATIC_PHI,
            sceneOverride = VisualizerBackgroundMode.BLACK
        )
        assertFalse(eff.showAlbumArt)
        assertTrue(eff.clearSceneBackdrop)
    }
}
