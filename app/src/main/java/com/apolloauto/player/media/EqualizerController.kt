package com.apolloauto.player.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class EqBand(
    val index: Int,
    val centerHz: Int,
    val levelMillibels: Short,
    val minLevel: Short,
    val maxLevel: Short
)

data class EqPreset(
    val id: String,
    val name: String,
    /** Relative band gains in millibels for up to 10 bands; adapted to device band count. */
    val bandGainsMb: List<Int>,
    val bassStrength: Int = 0,
    val builtIn: Boolean = true
)

data class EqState(
    val enabled: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val bassStrength: Int = 0,
    val attached: Boolean = false,
    val activePresetId: String = "flat",
    val presets: List<EqPreset> = emptyList()
)

class EqualizerController(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var lastSessionId: Int = 0

    private val _state = MutableStateFlow(
        EqState(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            activePresetId = prefs.getString(KEY_PRESET, "flat") ?: "flat",
            presets = builtInPresets() + loadCustomPresets()
        )
    )
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || (audioSessionId == lastSessionId && equalizer != null)) return
        releaseEffectsOnly()
        lastSessionId = audioSessionId
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val min = eq.bandLevelRange[0]
            val max = eq.bandLevelRange[1]
            eq.enabled = prefs.getBoolean(KEY_ENABLED, false)
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
            runCatching { bass.setStrength(strength.toShort()) }
            bassBoost = bass
            publish()
        }.onFailure {
            _state.value = _state.value.copy(attached = false, bands = emptyList())
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
        prefs.edit()
            .putInt(bandKey(index), clamped.toInt())
            .putString(KEY_PRESET, "custom")
            .apply()
        publish(activePresetId = "custom")
    }

    fun setBass(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        runCatching { bassBoost?.setStrength(value.toShort()) }
        prefs.edit().putInt(KEY_BASS, value).apply()
        publish()
    }

    fun applyPreset(presetId: String) {
        val preset = _state.value.presets.find { it.id == presetId } ?: return
        val eq = equalizer ?: run {
            prefs.edit().putString(KEY_PRESET, presetId).apply()
            publish(activePresetId = presetId)
            return
        }
        val min = eq.bandLevelRange[0]
        val max = eq.bandLevelRange[1]
        val count = eq.numberOfBands.toInt()
        val gains = resampleGains(preset.bandGainsMb, count)
        for (i in 0 until count) {
            val level = gains[i].toShort().coerceIn(min, max)
            eq.setBandLevel(i.toShort(), level)
            prefs.edit().putInt(bandKey(i), level.toInt()).apply()
        }
        setBass(preset.bassStrength)
        prefs.edit().putString(KEY_PRESET, presetId).apply()
        if (!eq.enabled) setEnabled(true)
        publish(activePresetId = presetId)
    }

    fun resetFlat() = applyPreset("flat")

    fun saveCustomPreset(name: String) {
        val eq = equalizer ?: return
        val gains = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
        val preset = EqPreset(
            id = "user_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom ${ _state.value.presets.count { !it.builtIn } + 1 }" },
            bandGainsMb = gains,
            bassStrength = _state.value.bassStrength,
            builtIn = false
        )
        val customs = loadCustomPresets() + preset
        persistCustomPresets(customs)
        prefs.edit().putString(KEY_PRESET, preset.id).apply()
        publish(activePresetId = preset.id, presets = builtInPresets() + customs)
    }

    fun deleteCustomPreset(id: String) {
        val customs = loadCustomPresets().filterNot { it.id == id }
        persistCustomPresets(customs)
        val active = if (_state.value.activePresetId == id) "flat" else _state.value.activePresetId
        prefs.edit().putString(KEY_PRESET, active).apply()
        publish(activePresetId = active, presets = builtInPresets() + customs)
    }

    fun release() {
        releaseEffectsOnly()
        lastSessionId = 0
        _state.value = _state.value.copy(attached = false, bands = emptyList())
    }

    private fun releaseEffectsOnly() {
        equalizer?.release()
        bassBoost?.release()
        equalizer = null
        bassBoost = null
    }

    private fun publish(
        activePresetId: String = prefs.getString(KEY_PRESET, "flat") ?: "flat",
        presets: List<EqPreset> = _state.value.presets.ifEmpty { builtInPresets() + loadCustomPresets() }
    ) {
        val eq = equalizer
        if (eq == null) {
            _state.value = EqState(
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                attached = false,
                activePresetId = activePresetId,
                presets = presets,
                bassStrength = prefs.getInt(KEY_BASS, 0)
            )
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
            attached = true,
            activePresetId = activePresetId,
            presets = presets
        )
    }

    private fun resampleGains(source: List<Int>, targetBands: Int): List<Int> {
        if (source.isEmpty() || targetBands <= 0) return List(targetBands) { 0 }
        if (source.size == targetBands) return source
        return List(targetBands) { i ->
            val pos = i.toFloat() / (targetBands - 1).coerceAtLeast(1)
            val srcPos = pos * (source.size - 1)
            val lo = srcPos.toInt().coerceIn(0, source.lastIndex)
            val hi = (lo + 1).coerceAtMost(source.lastIndex)
            val t = srcPos - lo
            ((source[lo] * (1 - t)) + (source[hi] * t)).toInt()
        }
    }

    private fun loadCustomPresets(): List<EqPreset> {
        val raw = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val gainsArr = obj.getJSONArray("gains")
                    val gains = buildList {
                        for (g in 0 until gainsArr.length()) add(gainsArr.getInt(g))
                    }
                    add(
                        EqPreset(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            bandGainsMb = gains,
                            bassStrength = obj.optInt("bass", 0),
                            builtIn = false
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistCustomPresets(list: List<EqPreset>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("bass", p.bassStrength)
                    .put("gains", JSONArray(p.bandGainsMb))
            )
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, arr.toString()).apply()
    }

    private fun bandKey(index: Int) = "band_$index"

    companion object {
        private const val PREFS = "apollo_eq"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BASS = "bass"
        private const val KEY_PRESET = "preset"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"

        fun builtInPresets(): List<EqPreset> = listOf(
            EqPreset("flat", "Flat", List(10) { 0 }, 0),
            EqPreset("void", "VOID", listOf(-80, -40, 0, 60, 120, 80, 40, 0, -60, -120), 80),
            EqPreset("neon", "NEON", listOf(120, 80, 40, 0, 150, 280, 320, 260, 180, 100), 220),
            EqPreset("bass_ritual", "BASS RITUAL", listOf(700, 550, 320, 120, -40, 0, 80, 140, 100, 40), 900),
            EqPreset("bass_boost", "Bass Boost", listOf(600, 450, 250, 80, 0, 0, 0, 0, 0, 0), 700),
            EqPreset("vocal", "Vocal Clarity", listOf(-150, -80, 0, 200, 350, 400, 250, 80, 0, -50), 100),
            EqPreset("rock", "Rock", listOf(350, 250, 120, 0, -80, 80, 220, 300, 280, 200), 350),
            EqPreset("electronic", "Electronic", listOf(400, 300, 100, 0, -100, 150, 250, 320, 280, 220), 500),
            EqPreset("classical", "Classical", listOf(0, 0, 0, 0, 0, 0, -80, -120, -150, -180), 0),
            EqPreset("acoustic", "Acoustic", listOf(200, 150, 80, 50, 0, 80, 150, 180, 120, 60), 150),
            EqPreset("car_audio", "Car Audio", listOf(450, 350, 180, 50, 0, 80, 160, 220, 180, 100), 600)
        )
    }
}
