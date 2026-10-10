package com.apolloauto.player.media

/**
 * User-facing visualizer background modes.
 * SCENE requests the renderer's native atmosphere (no album art, no forced clear).
 */
enum class VisualizerBackgroundMode {
    AUTO,
    BLACK,
    ALBUM_ART,
    SCENE;

    /** Compact HUD / chip label. */
    val shortLabel: String
        get() = when (this) {
            AUTO -> "AUTO"
            BLACK -> "BLACK"
            ALBUM_ART -> "ALBUM"
            SCENE -> "SCENE"
        }
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
        VisualizerMode.SPACE_WARP,
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
        VisualizerMode.VISSONANCE_MATRIX,
        VisualizerMode.LYRICS_RAINBOW,
        VisualizerMode.PARTY_MARQUEE,
        VisualizerMode.PROJECTM_PLASMA,
        VisualizerMode.PROJECTM_SCOPE,
        VisualizerMode.MILKDROP_WAVE,
        VisualizerMode.MILKDROP_ROTOR,
        VisualizerMode.AV_BAR,
        VisualizerMode.AV_BLAST,
        VisualizerMode.AV_BLOB,
        VisualizerMode.AV_WAVE,
        VisualizerMode.AV_CIRCLE_BARS,
        VisualizerMode.AV_HIFI,
        VisualizerMode.BAUER_FOUNTAIN,
        VisualizerMode.BAUER_FFT,
        VisualizerMode.BAUER_SPEC,
        VisualizerMode.DNA_CIRCLE,
        VisualizerMode.DNA_SPIRAL,
        VisualizerMode.TOPO_CONTOUR,
        VisualizerMode.WIRE_TERRAIN,
        VisualizerMode.NEON_TOPO,
        VisualizerMode.PSY_KALEIDO,
        VisualizerMode.ACID_TUNNEL,
        VisualizerMode.ST01_GRAF_EQ,
        VisualizerMode.ST02_LUNCHBREAK,
        VisualizerMode.ST03_CYBER_FUJI,
        VisualizerMode.ML_BLACK_HOLE,
        VisualizerMode.ML_EVENT_HORIZON,
        VisualizerMode.ML_XOR_PLASMA,
        VisualizerMode.ML_INFERNAL_MESH,
        VisualizerMode.ML_ASTRAL_COLLAPSE,
        VisualizerMode.ML_NEON_RITUAL,
        VisualizerMode.ML_LIQUID_WARP,
        VisualizerMode.ML_GLITCH_CORE,
        VisualizerMode.ML_CHAOS_TUNNEL,
        VisualizerMode.ML_FRACTAL_BITE -> SceneBackgroundPreference.BLACK

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
     * Precedence: scene override → global user mode → AUTO recommendation.
     */
    fun resolve(
        userMode: VisualizerBackgroundMode,
        scene: VisualizerMode,
        sceneOverride: VisualizerBackgroundMode? = null
    ): EffectiveBackground {
        val mode = sceneOverride ?: userMode
        return when (mode) {
            VisualizerBackgroundMode.BLACK -> EffectiveBackground(
                showAlbumArt = false,
                clearSceneBackdrop = true,
                preference = SceneBackgroundPreference.BLACK,
                note = "BLACK — pure #000000; scene fills suppressed"
            )
            VisualizerBackgroundMode.ALBUM_ART -> EffectiveBackground(
                showAlbumArt = true,
                clearSceneBackdrop = true,
                preference = SceneBackgroundPreference.ALBUM_ART,
                note = "ALBUM — geometry composites over blurred cover"
            )
            VisualizerBackgroundMode.SCENE -> EffectiveBackground(
                showAlbumArt = false,
                clearSceneBackdrop = false,
                preference = SceneBackgroundPreference.SCENE_DEFINED,
                note = "SCENE — native atmosphere (black for simple signal modes)"
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
                    note = "AUTO → ALBUM"
                )
                SceneBackgroundPreference.SCENE_DEFINED -> EffectiveBackground(
                    showAlbumArt = false,
                    clearSceneBackdrop = false,
                    preference = pref,
                    note = "AUTO → scene atmospheric world"
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
