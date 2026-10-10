package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerSceneRuntimeTest {
    @Test
    fun sameModeReselect_doesNotCountAsUnexpectedReset() {
        val rt = VisualizerSceneRuntime()
        rt.onModeSelected(VisualizerMode.SPECTRUM, "boot")
        rt.onModeSelected(VisualizerMode.SPECTRUM, "reselect")
        rt.onModeSelected(VisualizerMode.SPECTRUM, "metadata_refresh")
        val d = rt.diagnostics.value
        assertEquals(1, d.createCount)
        assertEquals(0, d.unexpectedResetCount)
        assertEquals(0, d.modeChangeCount)
    }

    @Test
    fun intentionalModeChange_incrementsModeChangeNotUnexpectedReset() {
        val rt = VisualizerSceneRuntime()
        rt.onModeSelected(VisualizerMode.SPECTRUM, "boot")
        rt.onModeSelected(VisualizerMode.AUDIBLE_SPIRALS, "autodj_interval")
        val d = rt.diagnostics.value
        assertEquals(1, d.modeChangeCount)
        assertEquals(1, d.autoDjTransitionCount)
        assertEquals(0, d.unexpectedResetCount)
        assertTrue(d.initCount >= 2)
    }
}
