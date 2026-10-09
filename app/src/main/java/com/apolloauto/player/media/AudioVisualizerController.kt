package com.apolloauto.player.media

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisualizerMode {
    SPECTRUM,
    WAVEFORM,
    CIRCULAR,
    LEVEL,
    MINIMAL,
    DECORATIVE
}

/**
 * Wraps Android Visualizer for FFT/waveform capture.
 * When capture is unavailable, [mode] may fall back to DECORATIVE and UI must label it as such.
 */
class AudioVisualizerController {
    private var visualizer: Visualizer? = null
    private var lastSession = 0

    private val _fft = MutableStateFlow(FloatArray(32))
    val fft: StateFlow<FloatArray> = _fft.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(64))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _active = MutableStateFlow(false)
    val capturing: StateFlow<Boolean> = _active.asStateFlow()

    private val _mode = MutableStateFlow(VisualizerMode.SPECTRUM)
    val mode: StateFlow<VisualizerMode> = _mode.asStateFlow()

    fun setMode(mode: VisualizerMode) {
        _mode.value = mode
    }

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == lastSession && visualizer != null) return
        release()
        lastSession = audioSessionId
        runCatching {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = range[1].coerceAtMost(512)
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null) return
                        val out = FloatArray(64)
                        val step = (waveform.size / out.size).coerceAtLeast(1)
                        var peak = 0f
                        for (i in out.indices) {
                            val v = ((waveform[i * step].toInt() and 0xFF) - 128) / 128f
                            out[i] = v
                            peak = maxOf(peak, kotlin.math.abs(v))
                        }
                        _waveform.value = out
                        _level.value = peak
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || fft.size < 4) return
                        val bands = 32
                        val out = FloatArray(bands)
                        val usable = fft.size / 2
                        val bucket = (usable / bands).coerceAtLeast(1)
                        for (i in 0 until bands) {
                            var sum = 0f
                            val base = i * bucket
                            for (j in 0 until bucket) {
                                val idx = (base + j) * 2
                                if (idx + 1 >= fft.size) break
                                val re = fft[idx].toInt()
                                val im = fft[idx + 1].toInt()
                                sum += kotlin.math.sqrt((re * re + im * im).toFloat())
                            }
                            out[i] = (sum / bucket / 128f).coerceIn(0f, 1f)
                        }
                        _fft.value = out
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            )
            viz.enabled = true
            visualizer = viz
            _active.value = true
        }.onFailure {
            _active.value = false
            if (_mode.value != VisualizerMode.DECORATIVE) {
                // keep selected mode but UI should show unavailable badge
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { visualizer?.enabled = enabled }
        if (!enabled) {
            _level.value = 0f
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
