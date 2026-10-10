package com.apolloauto.player.media

import android.media.audiofx.Visualizer
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
    DECORATIVE
}

/**
 * Wraps Android Visualizer for FFT/waveform capture with gain + smoothing
 * so UI modes stay visibly reactive to music.
 */
class AudioVisualizerController {
    private var visualizer: Visualizer? = null
    private var lastSession = 0

    private val bands = 48
    private val wavePoints = 96
    private val smoothedFft = FloatArray(bands)
    private val smoothedWave = FloatArray(wavePoints)
    private var smoothedLevel = 0f
    private var smoothedBass = 0f

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

    private val _mode = MutableStateFlow(VisualizerMode.SPECTRUM)
    val mode: StateFlow<VisualizerMode> = _mode.asStateFlow()

    fun setMode(mode: VisualizerMode) {
        _mode.value = mode
    }

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == lastSession && visualizer != null) {
            runCatching { visualizer?.enabled = true }
            _active.value = visualizer != null
            return
        }
        release()
        lastSession = audioSessionId
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
                        for (i in out.indices) {
                            smoothedWave[i] = smoothedWave[i] * 0.45f + out[i] * 0.55f
                        }
                        _waveform.value = smoothedWave.copyOf()
                        _level.value = smoothedLevel.coerceIn(0f, 1f)
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
                            val boosted = (ln(1f + avg * 0.085f) * 1.65f).coerceIn(0f, 1.4f)
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
                        var bassSum = 0f
                        val bassBands = (bands * 0.18f).toInt().coerceAtLeast(3)
                        for (i in 0 until bassBands) bassSum += smoothedFft[i]
                        val bassInstant = (bassSum / bassBands).coerceIn(0f, 1f)
                        smoothedBass = if (bassInstant > smoothedBass) {
                            smoothedBass * 0.3f + bassInstant * 0.7f
                        } else {
                            smoothedBass * 0.85f + bassInstant * 0.15f
                        }
                        _fft.value = smoothedFft.copyOf()
                        _bass.value = smoothedBass
                    }
                },
                Visualizer.getMaxCaptureRate(),
                true,
                true
            )
            viz.enabled = true
            visualizer = viz
            _active.value = true
        }.onFailure {
            _active.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { visualizer?.enabled = enabled }
        if (!enabled) {
            // Soft decay instead of hard zero so pause doesn't look dead.
            smoothedLevel *= 0.35f
            smoothedBass *= 0.35f
            for (i in smoothedFft.indices) smoothedFft[i] *= 0.4f
            _level.value = smoothedLevel
            _bass.value = smoothedBass
            _fft.value = smoothedFft.copyOf()
        }
    }

    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        lastSession = 0
        _active.value = false
    }
}
