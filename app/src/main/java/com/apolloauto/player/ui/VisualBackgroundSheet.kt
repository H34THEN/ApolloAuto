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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.media.VisualizerBackgroundMode
import com.apolloauto.player.media.VisualizerBackgroundPolicy
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette

/**
 * Compact VISUAL BACKGROUND sheet for Immersive / Viz-Only HUDs.
 * Changes only compositing prefs — never recreates the renderer.
 */
@Composable
fun VisualBackgroundSheet(
    scene: VisualizerMode,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showPerSceneOverride: Boolean = true
) {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val bgMode by app.themeStore.vizBackgroundMode.collectAsState()
    val albumBlur by app.themeStore.albumBlur.collectAsState()
    val albumDark by app.themeStore.albumDarkness.collectAsState()
    val albumSat by app.themeStore.albumSaturation.collectAsState()
    val overrides by app.themeStore.sceneBgOverrides.collectAsState()
    val sceneOverride = overrides[scene]
    val effective = remember(bgMode, scene, sceneOverride) {
        VisualizerBackgroundPolicy.resolve(bgMode, scene, sceneOverride)
    }

    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.panel.copy(alpha = 0.94f))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "VISUAL BACKGROUND",
                color = p.soft,
                fontFamily = body,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            Text(
                "CLOSE",
                color = p.dim,
                fontFamily = body,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VisualizerBackgroundMode.entries.forEach { m ->
                val on = bgMode == m && sceneOverride == null
                Text(
                    m.shortLabel,
                    color = if (on) p.voidBlack else p.soft,
                    fontFamily = body,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) p.primary else p.voidBlack.copy(alpha = 0.4f))
                        .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable {
                            app.themeStore.clearSceneBackgroundOverride(scene)
                            app.themeStore.setVizBackgroundMode(m)
                        }
                        .padding(vertical = 8.dp)
                        .padding(horizontal = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            effective.note,
            color = p.dim,
            fontFamily = body,
            fontSize = 10.sp
        )
        if (effective.showAlbumArt || bgMode == VisualizerBackgroundMode.ALBUM_ART) {
            Spacer(Modifier.height(8.dp))
            Text("Blur", color = p.soft, fontFamily = body, fontSize = 11.sp)
            Slider(
                value = albumBlur,
                onValueChange = { app.themeStore.setAlbumBlur(it) },
                colors = SliderDefaults.colors(
                    thumbColor = p.accent,
                    activeTrackColor = p.primary,
                    inactiveTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
            Text("Darkness", color = p.soft, fontFamily = body, fontSize = 11.sp)
            Slider(
                value = albumDark,
                onValueChange = { app.themeStore.setAlbumDarkness(it) },
                valueRange = 0.2f..0.92f,
                colors = SliderDefaults.colors(
                    thumbColor = p.accent,
                    activeTrackColor = p.primary,
                    inactiveTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
            Text("Saturation", color = p.soft, fontFamily = body, fontSize = 11.sp)
            Slider(
                value = albumSat,
                onValueChange = { app.themeStore.setAlbumSaturation(it) },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = p.accent,
                    activeTrackColor = p.primary,
                    inactiveTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
        if (showPerSceneOverride) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (sceneOverride != null) {
                    "Scene override: ${sceneOverride.shortLabel} (tap mode chips to clear)"
                } else {
                    "Hold a mode as scene-only: long-press chips in settings"
                },
                color = p.dim.copy(alpha = 0.85f),
                fontFamily = body,
                fontSize = 9.sp
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VisualizerBackgroundMode.entries.forEach { m ->
                    val on = sceneOverride == m
                    Text(
                        "S:${m.shortLabel.take(3)}",
                        color = if (on) p.accent else p.dim,
                        fontFamily = body,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .clickable {
                                if (on) app.themeStore.clearSceneBackgroundOverride(scene)
                                else app.themeStore.setSceneBackgroundOverride(scene, m)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
        if (scene == VisualizerMode.PARTY_MARQUEE) {
            Spacer(Modifier.height(12.dp))
            PartyMarqueeEditor()
        }
    }
}
