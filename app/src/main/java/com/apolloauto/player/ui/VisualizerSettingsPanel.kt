package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.companion.CompanionVoice
import com.apolloauto.player.media.TranceResponseProfile
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.media.VizQualityTier
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.ApolloHudPanel
import com.apolloauto.player.ui.hud.ApolloSciFiPanel
import com.apolloauto.player.ui.hud.useKenneyChrome
// SpiralVariant is in this package

@Composable
fun VisualizerSettingsPanel() {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val mode by app.visualizerController.mode.collectAsState()
    val randomizer by app.themeStore.vizRandomizer.collectAsState()
    val enabled by app.themeStore.enabledVisualizerModes.collectAsState()
    val driveMode by app.themeStore.driveMode.collectAsState()
    val beatFlash by app.themeStore.beatFlash.collectAsState()
    val artLock by app.themeStore.artColorLock.collectAsState()
    val cinematic by app.themeStore.cinematicViz.collectAsState()
    val capturing by app.visualizerController.capturing.collectAsState()
    val interfaceStyle by app.themeStore.interfaceStyle.collectAsState()
    val tranceProfile by app.themeStore.tranceProfile.collectAsState()
    val paletteMode by app.themeStore.vizPaletteMode.collectAsState()
    val quality by app.themeStore.vizQuality.collectAsState()
    val vizSat by app.themeStore.vizSaturation.collectAsState()
    val vizGlow by app.themeStore.vizGlow.collectAsState()
    val photosensitive by app.themeStore.photosensitiveMode.collectAsState()
    val autoDjSec by app.themeStore.autoDjIntervalSec.collectAsState()
    val autoDjTrack by app.themeStore.autoDjOnTrackChange.collectAsState()
    val kenney = useKenneyChrome(interfaceStyle)

    val Panel: @Composable (String, @Composable () -> Unit) -> Unit = { title, content ->
        if (kenney) ApolloSciFiPanel(title = title) { content() }
        else ApolloHudPanel(title = title) { content() }
    }

    Panel("LIVE PREVIEW") {
        Text(
            if (capturing) "Session FFT live" else "Start playback for live FFT",
            color = if (capturing) p.accent else p.dim,
            fontFamily = body,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        ApolloVisualizerSurface(
            playing = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .background(p.voidBlack)
        )
        Spacer(Modifier.height(8.dp))
        VisualizerModeSelector(
            selected = mode,
            enabledModes = enabled,
            showDescriptions = false,
            onSelect = { m ->
                app.themeStore.setVisualizerMode(m)
                app.visualizerController.setMode(m)
            }
        )
    }

    Panel("DRIVE MODE") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Cockpit profile", color = p.soft, fontFamily = body, fontSize = 14.sp)
                Text(
                    "Randomizer on · reduced motion · keep-screen-on in Car/Immersive",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = driveMode,
                onCheckedChange = {
                    app.themeStore.setDriveMode(it)
                    app.themeStore.speak(
                        if (it) CompanionVoice.driveOnLine() else CompanionVoice.driveOffLine()
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
    }

    Panel("AUTO DJ") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Scene rotation", color = p.soft, fontFamily = body, fontSize = 14.sp)
                Text(
                    "Favorites pool first · interval ${autoDjSec}s",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = randomizer,
                onCheckedChange = { app.themeStore.setVizRandomizer(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
        Text("Rotate interval", color = p.soft, fontFamily = body, fontSize = 12.sp)
        Slider(
            value = autoDjSec.toFloat(),
            onValueChange = { app.themeStore.setAutoDjIntervalSec(it.toInt()) },
            valueRange = 10f..180f,
            colors = SliderDefaults.colors(
                thumbColor = p.accent,
                activeTrackColor = p.primary,
                inactiveTrackColor = p.dim.copy(alpha = 0.35f)
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Change on track boundary", color = p.soft, fontFamily = body, fontSize = 13.sp)
            Switch(
                checked = autoDjTrack,
                onCheckedChange = { app.themeStore.setAutoDjOnTrackChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
    }

    Panel("TRANCE REACTOR") {
        Text("Response profile", color = p.soft, fontFamily = body, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TranceResponseProfile.entries.take(4).forEach { profile ->
                QuickFilterChip(profile.name.take(6)) {
                    app.themeStore.setTranceProfile(profile)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TranceResponseProfile.entries.drop(4).forEach { profile ->
                QuickFilterChip(profile.name.take(8)) {
                    app.themeStore.setTranceProfile(profile)
                }
            }
        }
        Text(
            "Active: ${tranceProfile.title}",
            color = p.accent,
            fontFamily = body,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Palette", color = p.soft, fontFamily = body, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VizPaletteMode.entries.forEach { m ->
                QuickFilterChip(m.name.take(6)) { app.themeStore.setVizPaletteMode(m) }
            }
        }
        Text("Active: ${paletteMode.name}", color = p.dim, fontFamily = body, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text("Quality", color = p.soft, fontFamily = body, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VizQualityTier.entries.forEach { q ->
                QuickFilterChip(q.name) { app.themeStore.setVizQuality(q) }
            }
        }
        Text("Active: ${quality.name}", color = p.dim, fontFamily = body, fontSize = 11.sp)
        Text("Saturation", color = p.soft, fontFamily = body, fontSize = 12.sp)
        Slider(
            value = vizSat,
            onValueChange = { app.themeStore.setVizSaturation(it) },
            valueRange = 0.2f..1.4f,
            colors = SliderDefaults.colors(
                thumbColor = p.accent,
                activeTrackColor = p.primary,
                inactiveTrackColor = p.dim.copy(alpha = 0.35f)
            )
        )
        Text("Scene glow", color = p.soft, fontFamily = body, fontSize = 12.sp)
        Slider(
            value = vizGlow,
            onValueChange = { app.themeStore.setVizGlow(it) },
            valueRange = 0.2f..1.6f,
            colors = SliderDefaults.colors(
                thumbColor = p.accent,
                activeTrackColor = p.primary,
                inactiveTrackColor = p.dim.copy(alpha = 0.35f)
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Photosensitive mode", color = p.soft, fontFamily = body, fontSize = 13.sp)
                Text("Caps beat flash · default on", color = p.dim, fontFamily = body, fontSize = 11.sp)
            }
            Switch(
                checked = photosensitive,
                onCheckedChange = { app.themeStore.setPhotosensitiveMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickFilterChip("SPIRAL+") {
                val vals = SpiralVariant.entries
                val idx = vals.indexOf(app.themeStore.spiralVariant.value)
                app.themeStore.setSpiralVariant(vals[(idx + 1) % vals.size])
            }
            QuickFilterChip("VISSON+") { app.themeStore.cycleVissonanceSubmode() }
            QuickFilterChip("TRANCE") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.TRANCE
                }
            }
            QuickFilterChip("XPER") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.EXPERIMENTAL
                }
            }
        }
    }

    Panel("VISUAL ESCALATION") {
        Text("Beat flash / strobe", color = p.soft, fontFamily = body, fontSize = 13.sp)
        Slider(
            value = beatFlash,
            onValueChange = { app.themeStore.setBeatFlash(it) },
            colors = SliderDefaults.colors(
                thumbColor = p.accent,
                activeTrackColor = p.primary,
                inactiveTrackColor = p.dim.copy(alpha = 0.35f)
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Art color lock", color = p.soft, fontFamily = body, fontSize = 13.sp)
            Switch(
                checked = artLock,
                onCheckedChange = { app.themeStore.setArtColorLock(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Cinematic", color = p.soft, fontFamily = body, fontSize = 13.sp)
                Text("Slows motion between drops", color = p.dim, fontFamily = body, fontSize = 11.sp)
            }
            Switch(
                checked = cinematic,
                onCheckedChange = { app.themeStore.setCinematicViz(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
    }

    Panel("CUSTOMIZER") {
        Text(
            "Toggle which visuals appear in selectors, immersive next, and randomizer. At least one must stay on.",
            color = p.dim,
            fontFamily = body,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickFilterChip("ALL") { app.themeStore.enableAllVisualizerModes() }
            QuickFilterChip("SIGNAL") {
                app.themeStore.enableVisualizerCategory { m ->
                    VisualizerModeCatalog.entry(m).category == VisualizerCategory.SIGNAL &&
                        m != VisualizerMode.MINIMAL
                }
            }
            QuickFilterChip("SPECTR") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.SPECTR
                }
            }
            QuickFilterChip("COSMIC") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.COSMIC
                }
            }
            QuickFilterChip("MV2") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.MV2
                }
            }
            QuickFilterChip("DNA") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.DNA
                }
            }
            QuickFilterChip("DROP") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.DROP
                }
            }
            QuickFilterChip("XR3") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.XR3
                }
            }
            QuickFilterChip("TRANCE") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.TRANCE
                }
            }
            QuickFilterChip("XPER") {
                app.themeStore.enableVisualizerCategory {
                    VisualizerModeCatalog.entry(it).category == VisualizerCategory.EXPERIMENTAL
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        VisualizerCategory.entries.forEach { cat ->
            val entries = VisualizerModeCatalog.all.filter { it.category == cat }
            if (entries.isEmpty()) return@forEach
            Text(
                cat.title,
                color = p.primary,
                fontFamily = body,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )
            entries.forEach { entry ->
                val on = entry.mode in enabled
                val lastOn = on && enabled.size == 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            1.dp,
                            if (on) p.primary.copy(alpha = 0.35f) else p.dim.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${entry.shortName} · ${entry.mode.name}",
                            color = if (on) p.soft else p.dim,
                            fontFamily = body,
                            fontSize = 12.sp
                        )
                        Text(
                            entry.description,
                            color = p.dim.copy(alpha = 0.8f),
                            fontFamily = body,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = on,
                        onCheckedChange = { checked ->
                            app.themeStore.setVisualizerModeEnabled(entry.mode, checked)
                            if (checked && mode != entry.mode) {
                                // keep preview on current unless enabling from empty edge
                            }
                            val active = app.themeStore.visualizerMode.value
                            app.visualizerController.setMode(active)
                        },
                        enabled = !lastOn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = p.accent,
                            checkedTrackColor = p.accent.copy(alpha = 0.45f),
                            uncheckedThumbColor = p.dim,
                            uncheckedTrackColor = p.dim.copy(alpha = 0.35f),
                            disabledCheckedThumbColor = p.accent.copy(alpha = 0.5f),
                            disabledCheckedTrackColor = p.accent.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${enabled.size} / ${VisualizerMode.entries.size} enabled",
            color = p.dim,
            fontFamily = body,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun QuickFilterChip(label: String, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = p.soft,
        fontFamily = body,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, p.accent.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
            .background(p.accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}
