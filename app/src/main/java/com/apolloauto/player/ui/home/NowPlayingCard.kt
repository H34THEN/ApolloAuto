package com.apolloauto.player.ui.home

import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.ApolloTypeScale
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.components.ArtworkThumb
import com.apolloauto.player.ui.hud.ApolloHudProgress
import com.apolloauto.player.ui.hud.useKenneyChrome
import com.apolloauto.player.theme.InterfaceStyle

@Composable
fun NowPlayingCard(
    title: String,
    artist: String,
    album: String,
    artworkUri: Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    inCarFavorites: Boolean,
    progress: Float,
    positionLabel: String,
    durationLabel: String,
    interfaceStyle: InterfaceStyle,
    onOpenPlayer: () -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onToggleCarFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val kenney = useKenneyChrome(interfaceStyle)
    val shape = RoundedCornerShape(ApolloSpace.corner)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, p.primary.copy(alpha = 0.4f), shape)
            .background(p.panel.copy(alpha = 0.9f))
            .clickable(onClick = onOpenPlayer)
            .padding(ApolloSpace.panelPadding)
    ) {
        Text(
            "NOW PLAYING",
            color = p.primary,
            fontFamily = body,
            fontSize = ApolloTypeScale.sectionSp.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(ApolloSpace.space8))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkThumb(artworkUri = artworkUri, size = ApolloSpace.playerArt)
            Spacer(Modifier.size(ApolloSpace.space12))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = p.soft,
                    fontFamily = body,
                    fontSize = ApolloTypeScale.trackTitleSp.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    color = p.dim,
                    fontFamily = body,
                    fontSize = ApolloTypeScale.metaSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.isNotBlank()) {
                    Text(
                        album,
                        color = p.dim.copy(alpha = 0.85f),
                        fontFamily = body,
                        fontSize = ApolloTypeScale.metaSp.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(ApolloSpace.space12))
        if (kenney) {
            ApolloHudProgress(progress, height = 8.dp)
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = p.secondary,
                trackColor = p.soft.copy(alpha = 0.12f)
            )
        }
        Spacer(Modifier.height(ApolloSpace.space4))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(positionLabel, color = p.dim, fontFamily = body, fontSize = ApolloTypeScale.metaSp.sp)
            Text(durationLabel, color = p.dim, fontFamily = body, fontSize = ApolloTypeScale.metaSp.sp)
        }
        Spacer(Modifier.height(ApolloSpace.space8))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = p.primary, modifier = Modifier.size(28.dp))
            }
            BoxPlay(playing = playing, onClick = onToggle)
            IconButton(onClick = onNext, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipNext, "Next", tint = p.primary, modifier = Modifier.size(28.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) p.accent else p.dim, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onRepeat, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onToggleCarFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.DirectionsCar,
                    "Car Favorites",
                    tint = if (inCarFavorites) p.secondary else p.dim,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.PlaylistAdd, "Add to playlist", tint = p.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BoxPlay(playing: Boolean, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(horizontal = ApolloSpace.space8)
            .size(56.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.35f), p.panel)))
            .border(1.5.dp, p.accent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (playing) "Pause" else "Play",
            tint = p.soft,
            modifier = Modifier.size(30.dp)
        )
    }
}
