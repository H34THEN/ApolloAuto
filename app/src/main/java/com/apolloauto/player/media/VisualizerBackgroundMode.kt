package com.apolloauto.player.media

/**
 * User-facing visualizer background modes.
 * SCENE_DEFINED is an internal AUTO resolution, not a fourth settings chip.
 */
enum class VisualizerBackgroundMode {
    AUTO,
    BLACK,
    ALBUM_ART
}

enum class SceneBackgroundPreference {
    BLACK,
    ALBUM_ART,
    SCENE_DEFINED
}

object VisualizerBackgroundPolicy {
    fun preferenceFor(mode: VisualizerMode): SceneBackgroundPreference = when (mode) {
        // Rainbow / spectral instruments — AUTO → BLACK
        VisualizerMode.SPECTRUM,
        VisualizerMode.BARS,
        VisualizerMode.WAVEFORM,
        VisualizerMode.CIRCULAR,
        VisualizerMode.RINGS,
        VisualizerMode.LEVEL,
        VisualizerMode.MINIMAL,
        VisualizerMode.WAVEBARS,
        VisualizerMode.MIRROR_BARS,
        VisualizerMode.PULSE,
        VisualizerMode.PARTICLES,
        VisualizerMode.BASS_NATION,
        VisualizerMode.BUBBLE_BASS,
        VisualizerMode.CIRCULAR_PULSE,
        VisualizerMode.COLOR_SIGNAL,
        VisualizerMode.DUAL_SIGNAL,
        VisualizerMode.MIXED_WAVE,
        VisualizerMode.SHAPE_WAVE,
        VisualizerMode.STAR_FIELD,
        VisualizerMode.STARS2,
        VisualizerMode.DNA_HELIX,
        VisualizerMode.DNA_WAVE,
        VisualizerMode.BEAT_TUNNEL,
        VisualizerMode.BEAT_CASCADE,
        VisualizerMode.XR3_OSCILLO,
        VisualizerMode.XR3_CIRCLE,
        VisualizerMode.XR3_POLY,
        VisualizerMode.XR3_BARS,
        VisualizerMode.XR3_VU,
        VisualizerMode.XR3_SIERPINSKI,
        VisualizerMode.XR3_JULIA,
        VisualizerMode.XR3_SPRITE,
        VisualizerMode.CHROMATIC_PHI,
        VisualizerMode.ECTOPLASM,
        VisualizerMode.CIRC_INFINITY,
        VisualizerMode.PRISM_RAIN,
        VisualizerMode.NOTE_SWARM,
        VisualizerMode.HARMONIC_MANDALA,
        VisualizerMode.LIQUID_SPECTRUM,
        VisualizerMode.AUDIBLE_SPIRALS,
        VisualizerMode.VISSONANCE_MATRIX -> SceneBackgroundPreference.BLACK

        // Atmospheric worlds — AUTO keeps scene-drawn void (not album art)
        VisualizerMode.HYPERDRIVE,
        VisualizerMode.NEBULA,
        VisualizerMode.RAVE_CORE,
        VisualizerMode.SIGNAL_OCEAN,
        VisualizerMode.PLASMA_HEART,
        VisualizerMode.FRACTURE,
        VisualizerMode.DECORATIVE,
        VisualizerMode.SYNTH_SMOKE,
        VisualizerMode.FLUCTUS_FLOW,
        VisualizerMode.WAVEFORM_VOID,
        VisualizerMode.OBSIDIAN_REACTOR -> SceneBackgroundPreference.SCENE_DEFINED
    }

    /**
     * Effective layer strategy for the unified stack.
     * User BLACK/ALBUM_ART always win; AUTO follows scene preference.
     */
    fun resolve(
        userMode: VisualizerBackgroundMode,
        scene: VisualizerMode
    ): EffectiveBackground {
        return when (userMode) {
            VisualizerBackgroundMode.BLACK -> EffectiveBackground(
                showAlbumArt = false,
                clearSceneBackdrop = true,
                preference = SceneBackgroundPreference.BLACK,
                note = "User BLACK — scene atmospheric fills suppressed"
            )
            VisualizerBackgroundMode.ALBUM_ART -> EffectiveBackground(
                showAlbumArt = true,
                clearSceneBackdrop = true,
                preference = SceneBackgroundPreference.ALBUM_ART,
                note = "User ALBUM_ART — geometry composites over cover"
            )
            VisualizerBackgroundMode.AUTO -> when (val pref = preferenceFor(scene)) {
                SceneBackgroundPreference.BLACK -> EffectiveBackground(
                    showAlbumArt = false,
                    clearSceneBackdrop = true,
                    preference = pref,
                    note = "AUTO → BLACK for rainbow/spectral contrast"
                )
                SceneBackgroundPreference.ALBUM_ART -> EffectiveBackground(
                    showAlbumArt = true,
                    clearSceneBackdrop = true,
                    preference = pref,
                    note = "AUTO → ALBUM_ART"
                )
                SceneBackgroundPreference.SCENE_DEFINED -> EffectiveBackground(
                    showAlbumArt = false,
                    clearSceneBackdrop = false,
                    preference = pref,
                    note = "AUTO → scene atmospheric world (no album art)"
                )
            }
        }
    }
}

data class EffectiveBackground(
    val showAlbumArt: Boolean,
    /** When true, scenes skip opaque void fills so art/black shows through. */
    val clearSceneBackdrop: Boolean,
    val preference: SceneBackgroundPreference,
    val note: String
)
