package com.apolloauto.player.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.apolloauto.player.media.VisualizerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class CustomThemeDraft(
    val name: String = "Custom Deck",
    val primary: Color = ApolloPrimePalette.primary,
    val secondary: Color = ApolloPrimePalette.secondary,
    val background: Color = ApolloPrimePalette.voidBlack,
    val surface: Color = ApolloPrimePalette.panel,
    val text: Color = ApolloPrimePalette.soft,
    val glowIntensity: Float = 1f
) {
    fun toPalette(): HudPalette {
        val readableText = ensureReadable(text, background)
        return HudPalette(
            id = HudThemeId.CUSTOM,
            voidBlack = background,
            panel = surface,
            grid = primary.copy(alpha = 0.2f),
            primary = primary,
            secondary = secondary,
            accent = secondary,
            soft = readableText,
            dim = readableText.copy(alpha = 0.65f),
            danger = Color(0xFFFF4D4D),
            glowA = primary,
            glowB = secondary,
            glowIntensity = glowIntensity.coerceIn(0.15f, 1.5f)
        )
    }
}

fun ensureReadable(text: Color, background: Color): Color {
    val contrast = relativeLuminance(text) - relativeLuminance(background)
    return if (kotlin.math.abs(contrast) < 0.35f) {
        if (relativeLuminance(background) < 0.5f) Color.White else Color.Black
    } else text
}

private fun relativeLuminance(c: Color): Float {
    fun chan(v: Float): Float =
        if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val r = chan(c.red)
    val g = chan(c.green)
    val b = chan(c.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeId = MutableStateFlow(loadThemeId())
    val themeId: StateFlow<HudThemeId> = _themeId.asStateFlow()

    private val _custom = MutableStateFlow(loadCustom())
    val custom: StateFlow<CustomThemeDraft> = _custom.asStateFlow()

    private val _glow = MutableStateFlow(prefs.getFloat(KEY_GLOW, 1f))
    val glowIntensity: StateFlow<Float> = _glow.asStateFlow()

    private val _motion = MutableStateFlow(
        runCatching { MotionProfile.valueOf(prefs.getString(KEY_MOTION, MotionProfile.FULL.name)!!) }
            .getOrDefault(MotionProfile.FULL)
    )
    val motionProfile: StateFlow<MotionProfile> = _motion.asStateFlow()

    private val _interfaceStyle = MutableStateFlow(
        runCatching {
            InterfaceStyle.valueOf(prefs.getString(KEY_INTERFACE, InterfaceStyle.CYBERPUNK_HUD.name)!!)
        }.getOrDefault(InterfaceStyle.CYBERPUNK_HUD)
    )
    val interfaceStyle: StateFlow<InterfaceStyle> = _interfaceStyle.asStateFlow()

    private val _playerLayout = MutableStateFlow(
        runCatching {
            PlayerLayoutMode.valueOf(prefs.getString(KEY_PLAYER_LAYOUT, PlayerLayoutMode.TACTICAL.name)!!)
        }.getOrDefault(PlayerLayoutMode.TACTICAL)
    )
    val playerLayout: StateFlow<PlayerLayoutMode> = _playerLayout.asStateFlow()

    private val _hudFont = MutableStateFlow(
        runCatching {
            HudFontOption.valueOf(prefs.getString(KEY_FONT, HudFontOption.E1D1C_TEXT.name)!!)
        }.getOrDefault(HudFontOption.E1D1C_TEXT)
    )
    val hudFont: StateFlow<HudFontOption> = _hudFont.asStateFlow()

    private val _visualizerMode = MutableStateFlow(
        runCatching {
            VisualizerMode.valueOf(prefs.getString(KEY_VIZ_MODE, VisualizerMode.SPECTRUM.name)!!)
        }.getOrDefault(VisualizerMode.SPECTRUM)
    )
    val visualizerMode: StateFlow<VisualizerMode> = _visualizerMode.asStateFlow()

    private val _vizRandomizer = MutableStateFlow(prefs.getBoolean(KEY_VIZ_RANDOM, false))
    val vizRandomizer: StateFlow<Boolean> = _vizRandomizer.asStateFlow()

    private val _enabledVisualizerModes = MutableStateFlow(loadEnabledModes())
    val enabledVisualizerModes: StateFlow<Set<VisualizerMode>> = _enabledVisualizerModes.asStateFlow()

    private val _driveMode = MutableStateFlow(prefs.getBoolean(KEY_DRIVE_MODE, false))
    val driveMode: StateFlow<Boolean> = _driveMode.asStateFlow()

    private val _beatFlash = MutableStateFlow(prefs.getFloat(KEY_BEAT_FLASH, 0.35f))
    val beatFlash: StateFlow<Float> = _beatFlash.asStateFlow()

    private val _artColorLock = MutableStateFlow(prefs.getBoolean(KEY_ART_COLOR_LOCK, false))
    val artColorLock: StateFlow<Boolean> = _artColorLock.asStateFlow()

    private val _cinematicViz = MutableStateFlow(prefs.getBoolean(KEY_CINEMATIC, false))
    val cinematicViz: StateFlow<Boolean> = _cinematicViz.asStateFlow()

    private val _phoneGain = MutableStateFlow(prefs.getFloat(KEY_PHONE_GAIN, 1f))
    val phoneGain: StateFlow<Float> = _phoneGain.asStateFlow()

    private val _usbGain = MutableStateFlow(prefs.getFloat(KEY_USB_GAIN, 1f))
    val usbGain: StateFlow<Float> = _usbGain.asStateFlow()

    private val _crossfadeMs = MutableStateFlow(prefs.getInt(KEY_CROSSFADE, 0))
    val crossfadeMs: StateFlow<Int> = _crossfadeMs.asStateFlow()

    private val _companionLine = MutableStateFlow("Deck standing by.")
    val companionLine: StateFlow<String> = _companionLine.asStateFlow()

    fun activePalette(): HudPalette =
        paletteFor(_themeId.value, _custom.value.toPalette(), _glow.value)

    fun setTheme(id: HudThemeId) {
        _themeId.value = id
        prefs.edit().putString(KEY_THEME, id.name).apply()
    }

    fun setGlow(value: Float) {
        val v = value.coerceIn(0.15f, 1.5f)
        _glow.value = v
        prefs.edit().putFloat(KEY_GLOW, v).apply()
    }

    fun setMotion(profile: MotionProfile) {
        _motion.value = profile
        prefs.edit().putString(KEY_MOTION, profile.name).apply()
    }

    fun setInterfaceStyle(style: InterfaceStyle) {
        _interfaceStyle.value = style
        prefs.edit().putString(KEY_INTERFACE, style.name).apply()
    }

    fun setPlayerLayout(mode: PlayerLayoutMode) {
        _playerLayout.value = mode
        prefs.edit().putString(KEY_PLAYER_LAYOUT, mode.name).apply()
    }

    fun setHudFont(option: HudFontOption) {
        _hudFont.value = option
        prefs.edit().putString(KEY_FONT, option.name).apply()
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        _visualizerMode.value = mode
        prefs.edit().putString(KEY_VIZ_MODE, mode.name).apply()
    }

    fun setVizRandomizer(enabled: Boolean) {
        _vizRandomizer.value = enabled
        prefs.edit().putBoolean(KEY_VIZ_RANDOM, enabled).apply()
    }

    fun toggleVizRandomizer(): Boolean {
        val next = !_vizRandomizer.value
        setVizRandomizer(next)
        return next
    }

    fun isVisualizerModeEnabled(mode: VisualizerMode): Boolean =
        mode in _enabledVisualizerModes.value

    fun setVisualizerModeEnabled(mode: VisualizerMode, enabled: Boolean) {
        val current = _enabledVisualizerModes.value.toMutableSet()
        if (enabled) {
            current += mode
        } else {
            if (current.size <= 1 && mode in current) return
            current -= mode
            if (_visualizerMode.value == mode) {
                val fallback = current.firstOrNull() ?: VisualizerMode.SPECTRUM
                setVisualizerMode(fallback)
            }
        }
        persistEnabledModes(current)
    }

    fun setEnabledVisualizerModes(modes: Set<VisualizerMode>) {
        val next = modes.ifEmpty { setOf(VisualizerMode.SPECTRUM) }
        persistEnabledModes(next)
        if (_visualizerMode.value !in next) {
            setVisualizerMode(next.first())
        }
    }

    fun enableAllVisualizerModes() {
        setEnabledVisualizerModes(VisualizerMode.entries.toSet())
    }

    fun setDriveMode(enabled: Boolean) {
        _driveMode.value = enabled
        prefs.edit().putBoolean(KEY_DRIVE_MODE, enabled).apply()
        if (enabled) {
            setVizRandomizer(true)
            setMotion(MotionProfile.REDUCED)
        }
    }

    fun toggleDriveMode(): Boolean {
        val next = !_driveMode.value
        setDriveMode(next)
        return next
    }

    fun setBeatFlash(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _beatFlash.value = v
        prefs.edit().putFloat(KEY_BEAT_FLASH, v).apply()
    }

    fun setArtColorLock(locked: Boolean) {
        _artColorLock.value = locked
        prefs.edit().putBoolean(KEY_ART_COLOR_LOCK, locked).apply()
    }

    fun setCinematicViz(enabled: Boolean) {
        _cinematicViz.value = enabled
        prefs.edit().putBoolean(KEY_CINEMATIC, enabled).apply()
    }

    fun setPhoneGain(value: Float) {
        val v = value.coerceIn(0.5f, 1.5f)
        _phoneGain.value = v
        prefs.edit().putFloat(KEY_PHONE_GAIN, v).apply()
    }

    fun setUsbGain(value: Float) {
        val v = value.coerceIn(0.5f, 1.5f)
        _usbGain.value = v
        prefs.edit().putFloat(KEY_USB_GAIN, v).apply()
    }

    fun setCrossfadeMs(ms: Int) {
        val v = ms.coerceIn(0, 3000)
        _crossfadeMs.value = v
        prefs.edit().putInt(KEY_CROSSFADE, v).apply()
    }

    fun speak(line: String) {
        _companionLine.value = line
    }

    fun enableVisualizerCategory(predicate: (VisualizerMode) -> Boolean) {
        val matched = VisualizerMode.entries.filter(predicate).toSet()
        setEnabledVisualizerModes(matched.ifEmpty { setOf(VisualizerMode.SPECTRUM) })
    }

    fun updateCustom(draft: CustomThemeDraft) {
        _custom.value = draft
        persistCustom(draft)
    }

    fun saveCustomAndApply(draft: CustomThemeDraft) {
        updateCustom(draft)
        setGlow(draft.glowIntensity)
        setTheme(HudThemeId.CUSTOM)
    }

    fun restoreCustomDefaults() {
        val defaults = CustomThemeDraft()
        updateCustom(defaults)
    }

    private fun loadThemeId(): HudThemeId {
        val raw = prefs.getString(KEY_THEME, HudThemeId.APOLLO_PRIME.name)!!
        // migrate old ids
        return when (raw) {
            "NEON_GRID" -> HudThemeId.APOLLO_PRIME
            "TOXIC_ACID" -> HudThemeId.GHOST_PROTOCOL
            "ICE_PROTOCOL" -> HudThemeId.APOLLO_PRIME
            "VOID_ROSE" -> HudThemeId.NEON_REVENANT
            else -> runCatching { HudThemeId.valueOf(raw) }.getOrDefault(HudThemeId.APOLLO_PRIME)
        }
    }

    private fun loadCustom(): CustomThemeDraft {
        val json = prefs.getString(KEY_CUSTOM, null) ?: return CustomThemeDraft()
        return runCatching {
            val obj = JSONObject(json)
            CustomThemeDraft(
                name = obj.optString("name", "Custom Deck"),
                primary = Color(obj.getInt("primary")),
                secondary = Color(obj.getInt("secondary")),
                background = Color(obj.getInt("background")),
                surface = Color(obj.getInt("surface")),
                text = Color(obj.getInt("text")),
                glowIntensity = obj.optDouble("glow", 1.0).toFloat()
            )
        }.getOrDefault(CustomThemeDraft())
    }

    private fun persistCustom(draft: CustomThemeDraft) {
        val obj = JSONObject()
            .put("name", draft.name)
            .put("primary", draft.primary.toArgb())
            .put("secondary", draft.secondary.toArgb())
            .put("background", draft.background.toArgb())
            .put("surface", draft.surface.toArgb())
            .put("text", draft.text.toArgb())
            .put("glow", draft.glowIntensity.toDouble())
        prefs.edit().putString(KEY_CUSTOM, obj.toString()).apply()
    }

    private fun loadEnabledModes(): Set<VisualizerMode> {
        val all = VisualizerMode.entries.toSet()
        val knownRaw = prefs.getString(KEY_VIZ_KNOWN, null)
        val previouslyKnown = knownRaw
            ?.split(',')
            ?.mapNotNull { name -> runCatching { VisualizerMode.valueOf(name.trim()) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        val newlyAdded = all.filter { it !in previouslyKnown }.toSet()

        val raw = prefs.getString(KEY_VIZ_ENABLED, null)
        val base = if (raw.isNullOrBlank()) {
            all
        } else {
            raw.split(',')
                .mapNotNull { name -> runCatching { VisualizerMode.valueOf(name.trim()) }.getOrNull() }
                .toSet()
                .ifEmpty { all }
        }
        val merged = base + newlyAdded
        prefs.edit()
            .putString(KEY_VIZ_KNOWN, all.joinToString(",") { it.name })
            .putString(KEY_VIZ_ENABLED, merged.joinToString(",") { it.name })
            .apply()
        return merged
    }

    private fun persistEnabledModes(modes: Set<VisualizerMode>) {
        _enabledVisualizerModes.value = modes
        prefs.edit()
            .putString(KEY_VIZ_ENABLED, modes.joinToString(",") { it.name })
            .putString(KEY_VIZ_KNOWN, VisualizerMode.entries.joinToString(",") { it.name })
            .apply()
    }

    companion object {
        private const val PREFS = "apollo_theme"
        private const val KEY_THEME = "hud_theme"
        private const val KEY_CUSTOM = "custom_json"
        private const val KEY_GLOW = "glow"
        private const val KEY_MOTION = "motion"
        private const val KEY_INTERFACE = "interface_style"
        private const val KEY_PLAYER_LAYOUT = "player_layout"
        private const val KEY_FONT = "hud_font"
        private const val KEY_VIZ_MODE = "visualizer_mode"
        private const val KEY_VIZ_RANDOM = "viz_randomizer"
        private const val KEY_VIZ_ENABLED = "viz_enabled_modes"
        private const val KEY_VIZ_KNOWN = "viz_known_modes"
        private const val KEY_DRIVE_MODE = "drive_mode"
        private const val KEY_BEAT_FLASH = "beat_flash"
        private const val KEY_ART_COLOR_LOCK = "art_color_lock"
        private const val KEY_CINEMATIC = "cinematic_viz"
        private const val KEY_PHONE_GAIN = "phone_gain"
        private const val KEY_USB_GAIN = "usb_gain"
        private const val KEY_CROSSFADE = "crossfade_ms"
    }
}
