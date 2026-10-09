package com.apolloauto.player.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBand(
    val index: Int,
    val centerHz: Int,
    val levelMillibels: Short,
    val minLevel: Short,
    val maxLevel: Short
)

data class EqState(
    val enabled: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val bassStrength: Int = 0,
    val attached: Boolean = false
)

class EqualizerController(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _state = MutableStateFlow(EqState(enabled = prefs.getBoolean(KEY_ENABLED, false)))
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = prefs.getBoolean(KEY_ENABLED, false)
            val min = eq.bandLevelRange[0]
            val max = eq.bandLevelRange[1]
            for (i in 0 until eq.numberOfBands) {
                val saved = prefs.getInt(bandKey(i), Int.MIN_VALUE)
                if (saved != Int.MIN_VALUE) {
                    eq.setBandLevel(i.toShort(), saved.toShort().coerceIn(min, max))
                }
            }
            equalizer = eq

            val bass = BassBoost(0, audioSessionId)
            bass.enabled = eq.enabled
            val strength = prefs.getInt(KEY_BASS, 0).coerceIn(0, 1000)
            bass.setStrength(strength.toShort())
            bassBoost = bass
            publish()
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        publish()
    }

    fun setBand(index: Int, levelMillibels: Short) {
        val eq = equalizer ?: return
        val min = eq.bandLevelRange[0]
        val max = eq.bandLevelRange[1]
        val clamped = levelMillibels.coerceIn(min, max)
        eq.setBandLevel(index.toShort(), clamped)
        prefs.edit().putInt(bandKey(index), clamped.toInt()).apply()
        publish()
    }

    fun setBass(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        bassBoost?.setStrength(value.toShort())
        prefs.edit().putInt(KEY_BASS, value).apply()
        publish()
    }

    fun reset() {
        val eq = equalizer ?: return
        for (i in 0 until eq.numberOfBands) {
            eq.setBandLevel(i.toShort(), 0)
            prefs.edit().remove(bandKey(i)).apply()
        }
        setBass(0)
        publish()
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        equalizer = null
        bassBoost = null
        _state.value = _state.value.copy(attached = false, bands = emptyList())
    }

    private fun publish() {
        val eq = equalizer
        if (eq == null) {
            _state.value = EqState(enabled = prefs.getBoolean(KEY_ENABLED, false), attached = false)
            return
        }
        val min = eq.bandLevelRange[0]
        val max = eq.bandLevelRange[1]
        val bands = (0 until eq.numberOfBands).map { i ->
            EqBand(
                index = i,
                centerHz = eq.getCenterFreq(i.toShort()) / 1000,
                levelMillibels = eq.getBandLevel(i.toShort()),
                minLevel = min,
                maxLevel = max
            )
        }
        _state.value = EqState(
            enabled = eq.enabled,
            bands = bands,
            bassStrength = bassBoost?.roundedStrength?.toInt() ?: prefs.getInt(KEY_BASS, 0),
            attached = true
        )
    }

    private fun bandKey(index: Int) = "band_$index"

    companion object {
        private const val PREFS = "apollo_eq"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BASS = "bass"
    }
}
