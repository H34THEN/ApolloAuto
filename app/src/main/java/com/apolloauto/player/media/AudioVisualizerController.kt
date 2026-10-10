package com.apolloauto.player.media

import android.media.audiofx.Visualizer
import android.os.SystemClock
import android.util.Log
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisualizerMode {
    SPECTRUM,
    BARS,
    WAVEFORM,
    CIRCULAR,
    RINGS,
    LEVEL,
    MINIMAL,
    DECORATIVE,
    HYPERDRIVE,
    NEBULA,
    RAVE_CORE,
    SIGNAL_OCEAN,
    PLASMA_HEART,
    FRACTURE,
    /** audiospectr-inspired symmetric wavebars */
    WAVEBARS,
    /** audiospectr-inspired mirrored neon spectrum */
    MIRROR_BARS,
    /** audiospectr-inspired bass pulse rings */
    PULSE,
    /** audiospectr-inspired radial particles */
    PARTICLES,
    /** MusicVisualizerV2-inspired radial HSB bars */
    BASS_NATION,
    /** MusicVisualizerV2-inspired floating FFT bubbles */
    BUBBLE_BASS,
    /** MusicVisualizerV2-inspired radial spokes + core */
    CIRCULAR_PULSE,
    /** MusicVisualizerV2-inspired zigzag color spectrum */
    COLOR_SIGNAL,
    /** MusicVisualizerV2-inspired mirrored dual spectrum */
    DUAL_SIGNAL,
    /** MusicVisualizerV2-inspired sine-warped spectrum */
    MIXED_WAVE,
    /** MusicVisualizerV2-inspired reactive shape grid */
    SHAPE_WAVE,
    /** @deprecated Retained for prefs; resolves to [SPACE_WARP]. */
    STAR_FIELD,
    /** @deprecated Retained for prefs; resolves to [SPACE_WARP]. */
    STARS2,
    /** Cosmic reactive hyperspace warp (replaces Stars / Stars2). */
    SPACE_WARP,
    /** Original DNA helix (MusicDNA-inspired aesthetic, not a source port) */
    DNA_HELIX,
    /** DNA ribbon spectrum fill */
    DNA_WAVE,
    /** Soft beat tunnel (MilkDrop/BeatDrop-inspired, Compose-only) */
    BEAT_TUNNEL,
    /** Cascading beat columns */
    BEAT_CASCADE,
    /** XR3Player-inspired stereo oscilloscope */
    XR3_OSCILLO,
    /** XR3Player-inspired radial circle-with-lines */
    XR3_CIRCLE,
    /** XR3Player-inspired polyspiral / rosette */
    XR3_POLY,
    /** XR3Player-inspired classic spectrum bars + peaks */
    XR3_BARS,
    /** XR3Player-inspired dual VU meters */
    XR3_VU,
    /** XR3Player-inspired Sierpinski triangles */
    XR3_SIERPINSKI,
    /** XR3Player-inspired Julia-set point field */
    XR3_JULIA,
    /** XR3Player-inspired wireframe 3D sphere sprite */
    XR3_SPRITE,
    // --- Trance Reactor TR01–TR08 ---
    CHROMATIC_PHI,
    ECTOPLASM,
    CIRC_INFINITY,
    PRISM_RAIN,
    SYNTH_SMOKE,
    NOTE_SWARM,
    HARMONIC_MANDALA,
    LIQUID_SPECTRUM,
    // --- Experimental Labs TR09–TR13 ---
    AUDIBLE_SPIRALS,
    FLUCTUS_FLOW,
    WAVEFORM_VOID,
    OBSIDIAN_REACTOR,
    VISSONANCE_MATRIX,
    /** Rainbow E1D1C karaoke — letters pulse with the beat (Car Mode / Immersive fun). */
    LYRICS_RAINBOW,
    /** Goofy user-authored scrolling marquee instead of a geometry visualizer. */
    PARTY_MARQUEE,
    // --- projectM-visualizer/projectm (Compose recreation of classic looks) ---
    PROJECTM_PLASMA,
    PROJECTM_SCOPE,
    // --- milkdrop2077/MilkDrop3 ---
    MILKDROP_WAVE,
    MILKDROP_ROTOR,
    // --- gauravk95/audio-visualizer-android (Apache-2.0 algorithm ports) ---
    AV_BAR,
    AV_BLAST,
    AV_BLOB,
    AV_WAVE,
    AV_CIRCLE_BARS,
    AV_HIFI,
    // --- bradleybauer/music_visualizer ---
    BAUER_FOUNTAIN,
    BAUER_FFT,
    BAUER_SPEC,
    // --- harjot-oberai/MusicDNA (CC BY-NC-SA; non-commercial) ---
    DNA_CIRCLE,
    DNA_SPIRAL,
    // --- apollo techno / trance topographic + trippy ---
    TOPO_CONTOUR,
    WIRE_TERRAIN,
    NEON_TOPO,
    PSY_KALEIDO,
    ACID_TUNNEL
}

/**
 * Wraps Android Visualizer for FFT/waveform capture with gain + smoothing
 * so UI modes stay visibly reactive to music.
 */
class AudioVisualizerController {
    private var visualizer: Visualizer? = null
    private var lastSession = 0
    val sceneRuntime = VisualizerSceneRuntime()

    private val bands = 48
    private val wavePoints = 96
    private val smoothedFft = FloatArray(bands)
    private val smoothedWave = FloatArray(wavePoints)
    private var prevFft: FloatArray? = null
    private var smoothedLevel = 0f
    private var smoothedSubBass = 0f
    private var smoothedBass = 0f
    private var smoothedLowMid = 0f
    private var smoothedUpperMid = 0f
    private var smoothedPresence = 0f
    private var smoothedTreble = 0f
    private var smoothedMid = 0f
    private var smoothedHigh = 0f
    private var smoothedRms = 0f
    private var smoothedPeak = 0f
    private var smoothedFlux = 0f
    private var smoothedOnset = 0f
    private var smoothedSilence = 1f
    private var prevEnergy = 0f
    private var wavePeak = 0f
    private var lastFrameUptimeMs = 0L
    private var frameCountWindow = 0
    private var frameWindowStartedAt = 0L
    private var analysisFps = 0f
    private var attachCount = 0
    private var captureInterruptCount = 0
    private var playbackPaused = false
    @Volatile var debugLogging: Boolean = false
        set(value) {
            field = value
            sceneRuntime.debugEnabled = value
        }

    private val _fft = MutableStateFlow(FloatArray(bands))
    val fft: StateFlow<FloatArray> = _fft.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(wavePoints))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _bass = MutableStateFlow(0f)
    val bass: StateFlow<Float> = _bass.asStateFlow()

    private val _active = MutableStateFlow(false)
    val capturing: StateFlow<Boolean> = _active.asStateFlow()

    private val _captureSource = MutableStateFlow(CaptureSource.DECORATIVE)
    val captureSource: StateFlow<CaptureSource> = _captureSource.asStateFlow()

    private val _features = MutableStateFlow(AudioFeatures())
    val features: StateFlow<AudioFeatures> = _features.asStateFlow()

    private val _mode = MutableStateFlow(VisualizerMode.SPECTRUM)
    val mode: StateFlow<VisualizerMode> = _mode.asStateFlow()

    private val _captureDiag = MutableStateFlow(CaptureDiagnostics())
    val captureDiagnostics: StateFlow<CaptureDiagnostics> = _captureDiag.asStateFlow()

    fun setMode(mode: VisualizerMode, reason: String = "user") {
        val resolved = resolveVisualizerMode(mode)
        _mode.value = resolved
        sceneRuntime.onModeSelected(resolved, reason)
        // Mode changes must NEVER rebuild the Visualizer capture session.
    }

    fun audioSessionId(): Int = lastSession

    fun frameAgeMs(): Long {
        if (lastFrameUptimeMs == 0L) return Long.MAX_VALUE
        return SystemClock.elapsedRealtime() - lastFrameUptimeMs
    }

    fun isFrameStale(thresholdMs: Long = 750L): Boolean =
        _active.value && frameAgeMs() > thresholdMs

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == lastSession && visualizer != null) {
            runCatching { visualizer?.enabled = true }
            _active.value = visualizer != null
            if (_active.value) _captureSource.value = CaptureSource.PLAYBACK_SESSION
            publishCaptureDiag()
            return
        }
        // Only rebuild when the Media3 session id actually changes.
        releaseCaptureOnly()
        lastSession = audioSessionId
        attachCount++
        runCatching {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = range[1].coerceAtMost(512)
            viz.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null || waveform.isEmpty()) return
                        val out = FloatArray(wavePoints)
                        val step = (waveform.size / out.size).coerceAtLeast(1)
                        var peak = 0f
                        var energy = 0f
                        for (i in out.indices) {
                            val v = ((waveform[i * step].toInt() and 0xFF) - 128) / 128f
                            out[i] = v
                            val abs = kotlin.math.abs(v)
                            peak = max(peak, abs)
                            energy += abs * abs
                        }
                        val rms = sqrt(energy / out.size)
                        val instant = (peak * 0.55f + rms * 1.35f).coerceIn(0f, 1f)
                        // Attack fast, release slower so bars punch with the beat.
                        smoothedLevel = if (instant > smoothedLevel) {
                            smoothedLevel * 0.35f + instant * 0.65f
                        } else {
                            smoothedLevel * 0.82f + instant * 0.18f
                        }
                        wavePeak = smoothToward(wavePeak, peak.coerceIn(0f, 1f), attack = 0.7f, release = 0.2f)
                        for (i in out.indices) {
                            smoothedWave[i] = smoothedWave[i] * 0.45f + out[i] * 0.55f
                        }
                        _waveform.value = smoothedWave.copyOf()
                        _level.value = smoothedLevel.coerceIn(0f, 1f)
                        markFrame()
                        publishFeatures(rms = rms.coerceIn(0f, 1f))
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || fft.size < 4) return
                        val n = fft.size
                        val magnitudes = FloatArray(n / 2)
                        // DC
                        magnitudes[0] = kotlin.math.abs(fft[0].toInt().toFloat())
                        for (k in 1 until magnitudes.size - 1) {
                            val re = fft[k * 2].toInt().toFloat()
                            val im = fft[k * 2 + 1].toInt().toFloat()
                            magnitudes[k] = sqrt(re * re + im * im)
                        }
                        magnitudes[magnitudes.lastIndex] = kotlin.math.abs(fft[1].toInt().toFloat())

                        val out = FloatArray(bands)
                        // Log-ish buckets so bass/mids/highs all move visibly.
                        for (i in 0 until bands) {
                            val t0 = i / bands.toFloat()
                            val t1 = (i + 1) / bands.toFloat()
                            val start = (magnitudes.size * (t0 * t0)).toInt().coerceIn(0, magnitudes.lastIndex)
                            val end = (magnitudes.size * (t1 * t1)).toInt().coerceIn(start + 1, magnitudes.size)
                            var sum = 0f
                            var count = 0
                            for (j in start until end) {
                                sum += magnitudes[j]
                                count++
                            }
                            val avg = if (count > 0) sum / count else 0f
                            // Compress + amplify quiet streams so bars react to real tracks.
                            val boosted = (ln(1f + avg * 0.11f) * 1.9f).coerceIn(0f, 1.5f)
                            out[i] = boosted.coerceIn(0f, 1f)
                        }
                        for (i in out.indices) {
                            val target = out[i]
                            smoothedFft[i] = if (target > smoothedFft[i]) {
                                smoothedFft[i] * 0.4f + target * 0.6f
                            } else {
                                smoothedFft[i] * 0.78f + target * 0.22f
                            }
                        }
                        val extracted = SpectralBandExtractor.extract(smoothedFft)
                        smoothedSubBass = smoothToward(smoothedSubBass, extracted.subBass, 0.65f, 0.18f)
                        smoothedBass = smoothToward(smoothedBass, extracted.bass, 0.7f, 0.15f)
                        smoothedLowMid = smoothToward(smoothedLowMid, extracted.lowMid, 0.55f, 0.2f)
                        smoothedUpperMid = smoothToward(smoothedUpperMid, extracted.upperMid, 0.55f, 0.2f)
                        smoothedPresence = smoothToward(smoothedPresence, extracted.presence, 0.5f, 0.2f)
                        smoothedTreble = smoothToward(smoothedTreble, extracted.treble, 0.5f, 0.2f)
                        smoothedMid = smoothToward(smoothedMid, extracted.mid, 0.55f, 0.22f)
                        smoothedHigh = smoothToward(smoothedHigh, extracted.high, 0.5f, 0.2f)
                        val fluxInstant = SpectralBandExtractor.flux(prevFft, smoothedFft)
                        smoothedFlux = smoothToward(smoothedFlux, fluxInstant, 0.7f, 0.22f)
                        prevFft = smoothedFft.copyOf()

                        _fft.value = smoothedFft.copyOf()
                        _bass.value = smoothedBass
                        markFrame()
                        publishFeatures()
                    }
                },
                Visualizer.getMaxCaptureRate(),
                true,
                true
            )
            viz.enabled = true
            visualizer = viz
            _active.value = true
            _captureSource.value = CaptureSource.PLAYBACK_SESSION
            if (debugLogging) Log.d(TAG, "capture attached session=$audioSessionId attachCount=$attachCount")
            publishCaptureDiag()
        }.onFailure {
            captureInterruptCount++
            _active.value = false
            _captureSource.value = CaptureSource.DECORATIVE
            if (debugLogging) Log.w(TAG, "capture attach failed session=$audioSessionId", it)
            publishCaptureDiag()
        }
    }

    /**
     * Pause/resume reactivity without tearing down the Visualizer object.
     * Hard-disabling capture every pause caused intermittent stale/frozen analysis.
     */
    fun setEnabled(enabled: Boolean) {
        playbackPaused = !enabled
        if (enabled) {
            runCatching { visualizer?.enabled = true }
            if (visualizer != null) {
                _active.value = true
                _captureSource.value = CaptureSource.PLAYBACK_SESSION
            }
        } else {
            // Keep capture attached; soft-decay features for visual continuity.
            softDecay(0.55f)
            _level.value = smoothedLevel
            _bass.value = smoothedBass
            _fft.value = smoothedFft.copyOf()
            publishFeatures()
        }
        publishCaptureDiag()
    }

    /** Attempt recovery if frames go stale while playback claims to be live. */
    fun recoverIfStale(audioSessionId: Int, staleMs: Long = 900L) {
        if (audioSessionId == 0) return
        if (!isFrameStale(staleMs)) return
        captureInterruptCount++
        if (debugLogging) Log.w(TAG, "stale frame age=${frameAgeMs()}ms — reattach session=$audioSessionId")
        if (audioSessionId == lastSession && visualizer != null) {
            runCatching {
                visualizer?.enabled = false
                visualizer?.enabled = true
            }
        } else {
            attach(audioSessionId)
        }
        publishCaptureDiag()
    }

    fun release() {
        releaseCaptureOnly()
        lastSession = 0
    }

    private fun releaseCaptureOnly() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        _active.value = false
        _captureSource.value = CaptureSource.DECORATIVE
        publishCaptureDiag()
    }

    private fun markFrame() {
        val now = SystemClock.elapsedRealtime()
        lastFrameUptimeMs = now
        if (frameWindowStartedAt == 0L) frameWindowStartedAt = now
        frameCountWindow++
        val elapsed = now - frameWindowStartedAt
        if (elapsed >= 1000L) {
            analysisFps = frameCountWindow * 1000f / elapsed
            frameCountWindow = 0
            frameWindowStartedAt = now
        }
        publishCaptureDiag()
    }

    private fun softDecay(factor: Float) {
        smoothedLevel *= factor
        smoothedSubBass *= factor
        smoothedBass *= factor
        smoothedLowMid *= factor
        smoothedUpperMid *= factor
        smoothedPresence *= factor
        smoothedTreble *= factor
        smoothedMid *= factor
        smoothedHigh *= factor
        smoothedRms *= factor
        smoothedPeak *= factor
        smoothedFlux *= factor
        for (i in smoothedFft.indices) smoothedFft[i] *= factor
    }

    private fun publishCaptureDiag() {
        _captureDiag.value = CaptureDiagnostics(
            captureSource = _captureSource.value,
            audioSessionId = lastSession,
            capturing = _active.value,
            playbackPaused = playbackPaused,
            lastFrameAgeMs = frameAgeMs().let { if (it == Long.MAX_VALUE) -1L else it },
            analysisFps = analysisFps,
            attachCount = attachCount,
            interruptCount = captureInterruptCount,
            stale = isFrameStale()
        )
    }

    private fun smoothToward(current: Float, target: Float, attack: Float, release: Float): Float =
        if (target > current) {
            current * (1f - attack) + target * attack
        } else {
            current * (1f - release) + target * release
        }

    private fun publishFeatures(rms: Float? = null) {
        rms?.let { smoothedRms = smoothToward(smoothedRms, it, attack = 0.6f, release = 0.25f) }
        smoothedPeak = smoothToward(smoothedPeak, max(wavePeak, smoothedLevel), attack = 0.65f, release = 0.2f)
        val energy = (
            smoothedLevel * 0.35f +
                smoothedBass * 0.25f +
                smoothedRms * 0.2f +
                smoothedFlux * 0.2f
            ).coerceIn(0f, 1f)
        val spike = (energy - prevEnergy).coerceAtLeast(0f)
        prevEnergy = energy * 0.92f + prevEnergy * 0.08f
        val onsetTarget = (spike * 2.0f + smoothedFlux * 0.55f).coerceIn(0f, 1f)
        smoothedOnset = smoothToward(smoothedOnset, onsetTarget, attack = 0.72f, release = 0.18f)
        val silenceTarget = (1f - energy * 1.15f).coerceIn(0f, 1f)
        smoothedSilence = smoothToward(smoothedSilence, silenceTarget, attack = 0.12f, release = 0.08f)
        _features.value = AudioFeatures(
            subBass = smoothedSubBass.coerceIn(0f, 1f),
            bass = smoothedBass.coerceIn(0f, 1f),
            lowMid = smoothedLowMid.coerceIn(0f, 1f),
            upperMid = smoothedUpperMid.coerceIn(0f, 1f),
            presence = smoothedPresence.coerceIn(0f, 1f),
            treble = smoothedTreble.coerceIn(0f, 1f),
            mid = smoothedMid.coerceIn(0f, 1f),
            high = smoothedHigh.coerceIn(0f, 1f),
            rms = smoothedRms.coerceIn(0f, 1f),
            peak = smoothedPeak.coerceIn(0f, 1f),
            spectralFlux = smoothedFlux.coerceIn(0f, 1f),
            onset = smoothedOnset.coerceIn(0f, 1f),
            silence = smoothedSilence.coerceIn(0f, 1f)
        )
        publishCaptureDiag()
    }

    companion object {
        private const val TAG = "ApolloVizCapture"
    }
}

data class CaptureDiagnostics(
    val captureSource: CaptureSource = CaptureSource.DECORATIVE,
    val audioSessionId: Int = 0,
    val capturing: Boolean = false,
    val playbackPaused: Boolean = false,
    val lastFrameAgeMs: Long = -1L,
    val analysisFps: Float = 0f,
    val attachCount: Int = 0,
    val interruptCount: Int = 0,
    val stale: Boolean = false
)
