package com.apolloauto.player.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.media.SleepTimerController
import com.apolloauto.player.media.shortLabel
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.CustomThemeDraft
import com.apolloauto.player.theme.HudFontOption
import com.apolloauto.player.theme.HudThemeId
import com.apolloauto.player.theme.InterfaceStyle
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.theme.MotionProfile
import com.apolloauto.player.theme.fontFamilyFor
import com.apolloauto.player.theme.parseHexColor
import com.apolloauto.player.theme.toHexRgb
import com.apolloauto.player.ui.hud.ApolloHudButton
import com.apolloauto.player.ui.hud.ApolloHudPanel
import com.apolloauto.player.ui.hud.ApolloSectionHeader
import com.apolloauto.player.ui.hud.ApolloSciFiPanel
import com.apolloauto.player.ui.hud.useKenneyChrome

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloCyberTheme {
                SettingsScreen(onClose = { finish() })
            }
        }
    }
}

private enum class SettingsTab { DISPLAY, LIBRARY, AUDIO, VISUALIZER, PLAYBACK, ECHOHUNT }

@Composable
private fun SettingsScreen(onClose: () -> Unit) {
    val p = LocalHudPalette.current
    var tab by remember { mutableStateOf(SettingsTab.DISPLAY) }

    CyberBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("COMMAND CENTER", color = p.primary, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text("Settings", color = p.soft, fontSize = 28.sp)
                }
                Text(
                    "CLOSE",
                    color = p.dim,
                    modifier = Modifier
                        .border(1.dp, p.dim.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTab.entries.forEach { t ->
                    val selected = tab == t
                    Text(
                        t.name,
                        color = if (selected) p.soft else p.dim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .background(if (selected) p.accent.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { tab = t }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    SettingsTab.DISPLAY -> DisplaySettings()
                    SettingsTab.LIBRARY -> LibrarySettingsPanel()
                    SettingsTab.AUDIO -> AudioSettings()
                    SettingsTab.VISUALIZER -> VisualizerSettingsPanel()
                    SettingsTab.PLAYBACK -> PlaybackSettings()
                    SettingsTab.ECHOHUNT -> EchoHuntSettingsPanel()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DisplaySettings() {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val themeId by app.themeStore.themeId.collectAsState()
    val custom by app.themeStore.custom.collectAsState()
    val glow by app.themeStore.glowIntensity.collectAsState()
    val motion by app.themeStore.motionProfile.collectAsState()
    val interfaceStyle by app.themeStore.interfaceStyle.collectAsState()
    val hudFont by app.themeStore.hudFont.collectAsState()
    val kenney = useKenneyChrome(interfaceStyle)
    var draft by remember(custom) { mutableStateOf(custom) }
    var hexPrimary by remember(draft.primary) { mutableStateOf(draft.primary.toHexRgb()) }

    val Panel: @Composable (String, @Composable () -> Unit) -> Unit = { title, content ->
        if (kenney) {
            ApolloSciFiPanel(title = title) { content() }
        } else {
            ApolloHudPanel(title = title) { content() }
        }
    }

    Panel("INTERFACE STYLE") {
        InterfaceStyle.entries.forEach { style ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (interfaceStyle == style) p.accent else p.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { app.themeStore.setInterfaceStyle(style) }
                    .padding(10.dp)
            ) {
                Text(style.label, color = p.soft, modifier = Modifier.weight(1f))
                if (interfaceStyle == style) Text("ACTIVE", color = p.accent, fontSize = 10.sp)
            }
        }
        Text(
            "Classic disables Kenney chrome while keeping themes, EQ, playlists, and timers.",
            color = p.dim,
            fontSize = 12.sp
        )
    }

    Panel("HUD FONT") {
        HudFontOption.entries.forEach { option ->
            val previewFace = fontFamilyFor(option)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (hudFont == option) p.accent else p.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (hudFont == option) p.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { app.themeStore.setHudFont(option) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        option.label.uppercase(),
                        color = p.soft,
                        fontFamily = previewFace,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Text(option.subtitle, color = p.dim, fontSize = 12.sp)
                    Text(
                        "APOLLO II · AUDIO CMD",
                        color = p.primary,
                        fontFamily = previewFace,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp
                    )
                }
                if (hudFont == option) Text("ACTIVE", color = p.accent, fontSize = 10.sp)
            }
        }
        Text(
            "Selected font applies to all app text. Use System Sans if a face is missing glyphs.",
            color = p.dim,
            fontSize = 12.sp
        )
    }

    Panel("THEME PRESETS") {
        HudThemeId.entries.filter { it != HudThemeId.CUSTOM }.forEach { id ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (themeId == id) p.accent else p.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (themeId == id) p.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { app.themeStore.setTheme(id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(id.label, color = p.soft)
                    Text(id.subtitle, color = p.dim, fontSize = 12.sp)
                }
                if (themeId == id) Text("ACTIVE", color = p.accent, fontSize = 10.sp)
            }
        }
    }

    Panel("CUSTOM THEME EDITOR") {
        Text("Name", color = p.dim, fontSize = 11.sp)
        HexField(draft.name, onChange = { draft = draft.copy(name = it) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Primary accent (hex)", color = p.dim, fontSize = 11.sp)
        HexField(hexPrimary, onChange = {
            hexPrimary = it
            draft = draft.copy(primary = parseHexColor(it, draft.primary))
        }, modifier = Modifier.fillMaxWidth())
        ColorSwatchRow(
            primary = draft.primary,
            secondary = draft.secondary,
            background = draft.background,
            surface = draft.surface
        )
        Text("Hue shift primary", color = p.dim, fontSize = 11.sp)
        Slider(
            value = hueOf(draft.primary),
            onValueChange = { h ->
                val c = colorFromHue(draft.primary, h)
                draft = draft.copy(primary = c)
                hexPrimary = c.toHexRgb()
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary)
        )
        Text("Secondary hue", color = p.dim, fontSize = 11.sp)
        Slider(
            value = hueOf(draft.secondary),
            onValueChange = { h ->
                draft = draft.copy(secondary = colorFromHue(draft.secondary, h))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = p.secondary, activeTrackColor = p.secondary)
        )
        Text("Glow intensity", color = p.dim, fontSize = 11.sp)
        Slider(
            value = draft.glowIntensity,
            onValueChange = { draft = draft.copy(glowIntensity = it) },
            valueRange = 0.15f..1.5f,
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("APPLY CUSTOM") {
                app.themeStore.saveCustomAndApply(draft)
            }
            ActionChip("RESTORE DEFAULTS") {
                draft = CustomThemeDraft()
                hexPrimary = draft.primary.toHexRgb()
                app.themeStore.restoreCustomDefaults()
            }
        }
    }

    Panel("MOTION / GLOW") {
        Text("Global glow", color = p.dim, fontSize = 11.sp)
        Slider(
            value = glow,
            onValueChange = { app.themeStore.setGlow(it) },
            valueRange = 0.15f..1.5f,
            colors = SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary)
        )
        ApolloSectionHeader("Animation profile")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MotionProfile.entries.forEach { profile ->
                val selected = motion == profile
                Text(
                    profile.name,
                    color = if (selected) p.soft else p.dim,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                        .clickable { app.themeStore.setMotion(profile) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioSettings() {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val eq by app.equalizerController.state.collectAsState()
    val capturing by app.visualizerController.capturing.collectAsState()
    val captureSource by app.visualizerController.captureSource.collectAsState()
    val interfaceStyle by app.themeStore.interfaceStyle.collectAsState()
    val kenney = useKenneyChrome(interfaceStyle)
    var customName by remember { mutableStateOf("") }

    val Panel: @Composable (String, @Composable () -> Unit) -> Unit = { title, content ->
        if (kenney) ApolloSciFiPanel(title = title) { content() }
        else ApolloHudPanel(title = title) { content() }
    }

    Panel("SIGNAL SCOPE") {
        Text(
            if (capturing) captureSource.shortLabel() else "${captureSource.shortLabel()} · start playback for live FFT",
            color = if (capturing) p.accent else p.dim,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Visualizer modes, randomizer, and customizer → VISUALIZER tab",
            color = p.dim,
            fontSize = 12.sp
        )
    }

    Panel("EQUALIZER") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (eq.attached) "Linked to audio session" else "Start playback to attach EQ",
                    color = p.dim,
                    fontSize = 13.sp
                )
                Text(
                    if (eq.enabled) "ENGAGED" else "BYPASSED",
                    color = if (eq.enabled) p.accent else p.dim,
                    fontSize = 15.sp
                )
            }
            Switch(
                checked = eq.enabled,
                onCheckedChange = { app.equalizerController.setEnabled(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = p.soft)
            )
        }
        Spacer(Modifier.height(8.dp))
        eq.presets.forEach { preset ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (eq.activePresetId == preset.id) p.secondary else p.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { app.equalizerController.applyPreset(preset.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(preset.name, color = p.soft, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (!preset.builtIn) {
                    Text(
                        "DEL",
                        color = p.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { app.equalizerController.deleteCustomPreset(preset.id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        eq.bands.forEach { band ->
            Text("${band.centerHz} Hz  ${band.levelMillibels} mB", color = p.dim, fontSize = 12.sp)
            Slider(
                value = band.levelMillibels.toFloat(),
                onValueChange = { app.equalizerController.setBand(band.index, it.toInt().toShort()) },
                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                enabled = eq.attached,
                colors = SliderDefaults.colors(thumbColor = p.primary, activeTrackColor = p.primary)
            )
        }
        Text("Bass boost", color = p.dim, fontSize = 12.sp)
        Slider(
            value = eq.bassStrength.toFloat(),
            onValueChange = { app.equalizerController.setBass(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = eq.attached,
            colors = SliderDefaults.colors(thumbColor = p.secondary, activeTrackColor = p.secondary)
        )
        Spacer(Modifier.height(8.dp))
        Text("Preset name", color = p.dim, fontSize = 12.sp)
        HexField(customName, onChange = { customName = it }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        if (kenney) {
            ApolloHudButton(
                "SAVE PRESET",
                onClick = {
                    app.equalizerController.saveCustomPreset(customName)
                    customName = ""
                },
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            ApolloHudButton(
                "RESET FLAT",
                onClick = { app.equalizerController.resetFlat() },
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp,
                fontSize = 14.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip("SAVE PRESET", Modifier.fillMaxWidth()) {
                    app.equalizerController.saveCustomPreset(customName)
                    customName = ""
                }
                ActionChip("RESET FLAT", Modifier.fillMaxWidth()) {
                    app.equalizerController.resetFlat()
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettings() {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val sleep by app.sleepTimerController.state.collectAsState()
    var customMinutes by remember { mutableStateOf("20") }

    ApolloHudPanel(title = "SLEEP TIMER") {
        Text(
            if (sleep.running) "ACTIVE · ${formatMs(sleep.remainingMs)} · ${sleep.mode.name}"
            else "Idle",
            color = if (sleep.running) p.accent else p.dim
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SleepTimerController.PRESETS_MS.forEach { ms ->
                ActionChip("${ms / 60_000}m") { app.sleepTimerController.startDuration(ms) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HexField(customMinutes, onChange = { customMinutes = it.filter { ch -> ch.isDigit() } }, modifier = Modifier.width(80.dp))
            ActionChip("CUSTOM MIN") {
                val mins = customMinutes.toLongOrNull() ?: return@ActionChip
                app.sleepTimerController.startDuration(mins.coerceIn(1, 240) * 60_000L)
            }
            ActionChip("END TRACK") {
                val rem = app.playbackBridge?.trackRemainingMs() ?: 60_000L
                app.sleepTimerController.startEndOfTrack(rem)
            }
            ActionChip("END QUEUE") {
                val rem = app.playbackBridge?.queueRemainingMs() ?: 180_000L
                app.sleepTimerController.startEndOfQueue(rem)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("+5 MIN") { app.sleepTimerController.extend(5 * 60_000L) }
            ActionChip("CANCEL") { app.sleepTimerController.cancel() }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fade out before stop", color = p.soft)
            Switch(
                checked = sleep.fadeOut,
                onCheckedChange = { app.sleepTimerController.setFadeOut(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = p.soft)
            )
        }
        Text(
            "Timer runs in the media service (screen-off / background / Android Auto).",
            color = p.dim,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActionChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val font = LocalBodyFont.current
    Text(
        label,
        color = p.voidBlack,
        fontFamily = font,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(p.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun HexField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalHudPalette.current
    val font = LocalBodyFont.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        cursorBrush = SolidColor(p.primary),
        textStyle = androidx.compose.ui.text.TextStyle(color = p.soft, fontSize = 14.sp, fontFamily = font),
        modifier = modifier
            .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun ColorSwatchRow(primary: Color, secondary: Color, background: Color, surface: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        listOf(primary, secondary, background, surface).forEach { c ->
            Box(
                Modifier
                    .width(36.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(c)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            255,
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        ),
        hsv
    )
    return hsv[0]
}

private fun colorFromHue(base: Color, hue: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            255,
            (base.red * 255).toInt(),
            (base.green * 255).toInt(),
            (base.blue * 255).toInt()
        ),
        hsv
    )
    hsv[0] = hue
    if (hsv[1] < 0.2f) hsv[1] = 0.85f
    if (hsv[2] < 0.35f) hsv[2] = 0.9f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
