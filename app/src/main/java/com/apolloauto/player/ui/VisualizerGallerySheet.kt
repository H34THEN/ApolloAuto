package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.media.VisualizerBackgroundMode
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun VisualizerGallerySheet(
    onSelect: (VisualizerMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val hud = LocalHudFont.current
    val enabled by app.themeStore.enabledVisualizerModes.collectAsState()
    val current by app.themeStore.visualizerMode.collectAsState()
    val favorites by app.themeStore.favoriteVizModes.collectAsState()
    val quality by app.themeStore.vizQuality.collectAsState()
    val bgMode by app.themeStore.vizBackgroundMode.collectAsState()
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(VisualizerCategory.TRANCE) }

    val galleryGroups = listOf(
        VisualizerCategory.SIGNAL,
        VisualizerCategory.COSMIC,
        VisualizerCategory.TRANCE,
        VisualizerCategory.EXPERIMENTAL,
        VisualizerCategory.SPECTR,
        VisualizerCategory.MV2,
        VisualizerCategory.DNA,
        VisualizerCategory.DROP,
        VisualizerCategory.XR3,
        VisualizerCategory.RAVE
    )

    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel.copy(alpha = 0.94f))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VISUALIZER GALLERY", color = p.primary, fontFamily = hud, fontSize = 13.sp, letterSpacing = 1.sp)
            Text(
                "CLOSE",
                color = p.dim,
                fontFamily = body,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }
        Text(
            "Quality ${quality.name} · ${favorites.size} favorites · BG ${bgMode.name}",
            color = p.dim,
            fontFamily = body,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VisualizerBackgroundMode.entries.forEach { m ->
                val on = bgMode == m
                Text(
                    m.shortLabel,
                    color = if (on) p.voidBlack else p.soft,
                    fontFamily = body,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) p.primary else p.voidBlack.copy(alpha = 0.35f))
                        .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { app.themeStore.setVizBackgroundMode(m) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("Search scenes", fontFamily = body, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.primary,
                unfocusedBorderColor = p.dim.copy(alpha = 0.4f),
                focusedTextColor = p.soft,
                unfocusedTextColor = p.soft,
                cursorColor = p.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            galleryGroups.forEach { cat ->
                val on = group == cat
                Text(
                    cat.title,
                    color = if (on) p.voidBlack else p.soft,
                    fontFamily = body,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) p.primary else p.voidBlack.copy(alpha = 0.35f))
                        .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { group = cat }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val needle = query.trim().lowercase()
            VisualizerModeCatalog.all
                .filter { it.category == group }
                .filter { it.mode in enabled }
                .filter {
                    needle.isEmpty() ||
                        it.shortName.lowercase().contains(needle) ||
                        it.description.lowercase().contains(needle) ||
                        it.mode.name.lowercase().contains(needle)
                }
                .forEach { entry ->
                    val selected = entry.mode == current
                    val fav = entry.mode in favorites
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) p.primary.copy(alpha = 0.2f)
                                else p.voidBlack.copy(alpha = 0.35f)
                            )
                            .border(
                                1.dp,
                                if (selected) p.primary else p.dim.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(entry.mode) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.shortName, color = p.soft, fontFamily = hud, fontSize = 12.sp)
                            Text(entry.description, color = p.dim, fontFamily = body, fontSize = 10.sp)
                        }
                        Text(
                            if (fav) "★" else "☆",
                            color = if (fav) p.accent else p.dim,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { app.themeStore.toggleFavoriteViz(entry.mode) }
                                .padding(start = 8.dp)
                        )
                    }
                }
        }
    }
}
