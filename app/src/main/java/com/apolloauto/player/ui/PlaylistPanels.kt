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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.data.Playlist
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun AddToPlaylistSheet(
    trackId: String,
    trackTitle: String,
    playlists: List<Playlist>,
    playlistStore: PlaylistStore,
    onDismiss: () -> Unit
) {
    val p = LocalHudPalette.current
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(p.panel.copy(alpha = 0.96f))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ADD TO PLAYLIST", color = p.primary, letterSpacing = 2.sp, fontSize = 12.sp)
                Text(trackTitle, color = p.soft, fontSize = 16.sp, maxLines = 1)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = p.dim)
            }
        }

        Spacer(Modifier.height(10.dp))

        // One-tap Car Favorites
        val inCar = playlistStore.isInCarFavorites(trackId)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (inCar) p.secondary.copy(alpha = 0.22f) else p.voidBlack.copy(alpha = 0.55f))
                .border(1.dp, if (inCar) p.secondary else p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .clickable {
                    playlistStore.toggleCarFavorite(trackId)
                }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.DirectionsCar, null, tint = if (inCar) p.secondary else p.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Car Favorites", color = p.soft, fontSize = 16.sp)
                Text(
                    if (inCar) "Saved for quick car play" else "Tap to save for the car",
                    color = p.dim,
                    fontSize = 12.sp
                )
            }
            Icon(
                if (inCar) Icons.Default.Check else Icons.Default.Add,
                null,
                tint = if (inCar) p.accent else p.dim
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("YOUR PLAYLISTS", color = p.dim, letterSpacing = 2.sp, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(playlists, key = { it.id }) { pl ->
                val inList = playlistStore.contains(pl.id, trackId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, p.primary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                        .background(p.voidBlack.copy(alpha = 0.4f))
                        .clickable { playlistStore.toggleTrack(pl.id, trackId) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        null,
                        tint = if (inList) p.accent else p.dim
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.name, color = p.soft)
                        Text("${pl.trackIds.size} tracks", color = p.dim, fontSize = 12.sp)
                    }
                    Text(
                        if (inList) "IN LIST" else "ADD",
                        color = if (inList) p.accent else p.primary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("NEW PLAYLIST", color = p.dim, letterSpacing = 2.sp, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                cursorBrush = SolidColor(p.primary),
                textStyle = androidx.compose.ui.text.TextStyle(color = p.soft, fontSize = 14.sp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (newName.isEmpty()) Text("Night Drive…", color = p.dim)
                    inner()
                }
            )
            Text(
                "CREATE",
                color = p.voidBlack,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.primary)
                    .clickable {
                        val created = playlistStore.create(newName.trim())
                        playlistStore.addTrack(created.id, trackId)
                        newName = ""
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
