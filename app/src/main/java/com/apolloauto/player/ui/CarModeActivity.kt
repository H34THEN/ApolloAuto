package com.apolloauto.player.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.Track
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.ApolloTypeScale
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.components.ArtworkThumb
import com.apolloauto.player.ui.hud.ApolloArtworkFrame
import com.apolloauto.player.ui.hud.ApolloHudProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class CarListMode { CAR_FAVORITES, ALL, QUEUE }

class CarModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloCyberTheme {
                CarDeckScreen(onExit = { finish() })
            }
        }
    }
}

@Composable
private fun CarDeckScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val hud = LocalHudFont.current
    val tracks by app.libraryStore.tracks.collectAsState()
    val playlists by app.playlistStore.playlists.collectAsState()
    val source by app.libraryStore.source.collectAsState()
    val status by app.libraryStore.status.collectAsState()
    val controller = remember { PlayerController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    var listMode by remember { mutableStateOf(CarListMode.CAR_FAVORITES) }
    var addSheet by remember { mutableStateOf(false) }

    val carTracks = remember(tracks, playlists) {
        app.playlistStore.resolveById(PlaylistStore.CAR_FAVORITES_ID, tracks)
    }
    val queueTracks = remember(tracks, playback.mediaId) {
        if (playback.mediaId.isBlank()) emptyList()
        else {
            val idx = tracks.indexOfFirst { it.id == playback.mediaId }
            if (idx < 0) emptyList() else tracks.drop(idx)
        }
    }
    val listTracks = when (listMode) {
        CarListMode.CAR_FAVORITES -> carTracks
        CarListMode.ALL -> tracks
        CarListMode.QUEUE -> queueTracks
    }
    val inCar = playback.mediaId.isNotBlank() && app.playlistStore.isInCarFavorites(playback.mediaId)
    val progress = if (playback.durationMs > 0) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f

    DisposableEffect(Unit) {
        app.playlistStore.ensureCarFavorites()
        controller.connect()
        onDispose { controller.release() }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            controller.pollPosition()
            delay(350)
        }
    }

    CyberBackground {
        Box(Modifier.fillMaxSize()) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(ApolloSpace.screenPadding)
            ) {
                val narrow = maxWidth < 700.dp
                val short = maxHeight < 420.dp
                val artSize = when {
                    short -> ApolloSpace.carArtMin
                    narrow -> 160.dp
                    else -> ApolloSpace.carArtMax
                }

                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "APOLLO II",
                                color = p.primary,
                                fontFamily = hud,
                                fontSize = 20.sp,
                                letterSpacing = 1.sp,
                                maxLines = 1
                            )
                            Text(
                                "  // CAR MODE",
                                color = p.secondary,
                                fontFamily = body,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                maxLines = 1
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                source.name,
                                color = p.soft,
                                fontFamily = body,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, p.accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                "EXIT",
                                color = p.dim,
                                fontFamily = body,
                                modifier = Modifier
                                    .border(1.dp, p.dim.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable(onClick = onExit)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(ApolloSpace.space12))

                    if (narrow) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ApolloSpace.space12)) {
                            CarPlaybackZone(
                                artSize = artSize,
                                title = playback.title,
                                artist = playback.artist,
                                album = playback.album,
                                artworkUri = playback.artworkUri,
                                playing = playback.playing,
                                shuffle = playback.shuffle,
                                repeatMode = playback.repeatMode,
                                progress = progress,
                                position = playback.positionMs,
                                duration = playback.durationMs,
                                inCar = inCar,
                                status = status,
                                compact = true,
                                onPrev = controller::skipPrevious,
                                onToggle = controller::togglePlayPause,
                                onNext = controller::skipNext,
                                onShuffle = controller::toggleShuffle,
                                onRepeat = controller::cycleRepeat,
                                onSaveCar = {
                                    if (playback.mediaId.isNotBlank()) {
                                        app.playlistStore.toggleCarFavorite(playback.mediaId)
                                    }
                                },
                                onAddList = { if (playback.mediaId.isNotBlank()) addSheet = true }
                            )
                            CarBrowsePanel(
                                listMode = listMode,
                                carCount = carTracks.size,
                                allCount = tracks.size,
                                queueCount = queueTracks.size,
                                listTracks = listTracks,
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                modifier = Modifier.weight(1f),
                                onMode = { listMode = it },
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onToggleCar = { app.playlistStore.toggleCarFavorite(it) }
                            )
                        }
                    } else {
                        Row(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ApolloSpace.space12)
                        ) {
                            CarPlaybackZone(
                                artSize = artSize,
                                title = playback.title,
                                artist = playback.artist,
                                album = playback.album,
                                artworkUri = playback.artworkUri,
                                playing = playback.playing,
                                shuffle = playback.shuffle,
                                repeatMode = playback.repeatMode,
                                progress = progress,
                                position = playback.positionMs,
                                duration = playback.durationMs,
                                inCar = inCar,
                                status = status,
                                compact = short,
                                onPrev = controller::skipPrevious,
                                onToggle = controller::togglePlayPause,
                                onNext = controller::skipNext,
                                onShuffle = controller::toggleShuffle,
                                onRepeat = controller::cycleRepeat,
                                onSaveCar = {
                                    if (playback.mediaId.isNotBlank()) {
                                        app.playlistStore.toggleCarFavorite(playback.mediaId)
                                    }
                                },
                                onAddList = { if (playback.mediaId.isNotBlank()) addSheet = true },
                                modifier = Modifier
                                    .weight(1.35f)
                                    .fillMaxHeight()
                            )
                            CarBrowsePanel(
                                listMode = listMode,
                                carCount = carTracks.size,
                                allCount = tracks.size,
                                queueCount = queueTracks.size,
                                listTracks = listTracks,
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onMode = { listMode = it },
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onToggleCar = { app.playlistStore.toggleCarFavorite(it) }
                            )
                        }
                    }
                }
            }

            if (addSheet && playback.mediaId.isNotBlank()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(p.voidBlack.copy(alpha = 0.55f))
                        .clickable { addSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(Modifier.clickable(enabled = false) {}) {
                        AddToPlaylistSheet(
                            trackId = playback.mediaId,
                            trackTitle = playback.title,
                            playlists = playlists,
                            playlistStore = app.playlistStore,
                            onDismiss = { addSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarPlaybackZone(
    artSize: androidx.compose.ui.unit.Dp,
    title: String,
    artist: String,
    album: String,
    artworkUri: android.net.Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    progress: Float,
    position: Long,
    duration: Long,
    inCar: Boolean,
    status: String,
    compact: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSaveCar: () -> Unit,
    onAddList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val hud = LocalHudFont.current
    Column(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ApolloSpace.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApolloArtworkFrame(Modifier.size(artSize)) {
                if (artworkUri != null) {
                    coil.compose.AsyncImage(
                        model = artworkUri,
                        contentDescription = "Album artwork",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    com.apolloauto.player.ui.components.ApolloArtPlaceholder(Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", color = p.primary, fontFamily = body, letterSpacing = 1.5.sp, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    color = p.soft,
                    fontFamily = body,
                    fontSize = if (compact) 20.sp else 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.isNotBlank() && !compact) {
                    Text(
                        album,
                        color = p.dim.copy(alpha = 0.85f),
                        fontFamily = body,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(ApolloSpace.space8))
                ApolloHudProgress(progress, height = 10.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${fmt(position)} / ${fmt(duration)}",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(ApolloSpace.space12))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = p.primary, modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.4f), p.panel)))
                    .border(2.dp, p.accent, CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (playing) "Pause" else "Play",
                    tint = p.soft,
                    modifier = Modifier.size(34.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipNext, "Next", tint = p.primary, modifier = Modifier.size(32.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) p.accent else p.dim)
            }
            IconButton(onClick = onRepeat, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary
                )
            }
            Text(
                if (inCar) "IN CAR ★" else "SAVE TO CAR",
                color = if (inCar) p.secondary else p.soft,
                fontFamily = body,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, if (inCar) p.secondary else p.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .background(if (inCar) p.secondary.copy(alpha = 0.15f) else p.panel)
                    .clickable(onClick = onSaveCar)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            IconButton(onClick = onAddList, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.PlaylistAdd, "Add to playlist", tint = p.primary)
            }
        }
        if (status.isNotBlank()) {
            Text(status, color = p.dim, fontFamily = body, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CarBrowsePanel(
    listMode: CarListMode,
    carCount: Int,
    allCount: Int,
    queueCount: Int,
    listTracks: List<Track>,
    playingId: String,
    playlistStore: PlaylistStore,
    onMode: (CarListMode) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onToggleCar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(ApolloSpace.corner))
            .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(ApolloSpace.corner))
            .background(p.panel.copy(alpha = 0.78f))
            .padding(ApolloSpace.panelPadding)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip("CAR ★ $carCount", listMode == CarListMode.CAR_FAVORITES, Modifier.weight(1f)) {
                onMode(CarListMode.CAR_FAVORITES)
            }
            ModeChip("ALL $allCount", listMode == CarListMode.ALL, Modifier.weight(1f)) {
                onMode(CarListMode.ALL)
            }
            ModeChip("QUEUE $queueCount", listMode == CarListMode.QUEUE, Modifier.weight(1f)) {
                onMode(CarListMode.QUEUE)
            }
        }
        Spacer(Modifier.height(ApolloSpace.space8))
        if (listTracks.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(ApolloSpace.space12),
                verticalArrangement = Arrangement.Top
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = p.dim, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(ApolloSpace.space8))
                Text(
                    when (listMode) {
                        CarListMode.CAR_FAVORITES -> "No Car Favorites yet."
                        CarListMode.QUEUE -> "Queue is empty. Play a track to populate."
                        CarListMode.ALL -> "No tracks. Scan phone music first."
                    },
                    color = p.soft,
                    fontFamily = body,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(ApolloSpace.space4))
                Text(
                    when (listMode) {
                        CarListMode.CAR_FAVORITES -> "Save a track with SAVE TO CAR to add it here."
                        CarListMode.QUEUE -> "Switch to ALL to browse the library."
                        CarListMode.ALL -> "Return to Home and scan the phone library."
                    },
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(listTracks, key = { _, t -> t.id }) { index, track ->
                    val selected = track.id == playingId
                    val saved = playlistStore.isInCarFavorites(track.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selected) p.accent.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selected) p.accent.copy(alpha = 0.7f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onPlay(listTracks, index) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkThumb(artworkUri = track.artworkUri, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                color = if (selected) p.accent else p.soft,
                                fontFamily = body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                            Text(
                                track.artist,
                                color = p.dim,
                                fontFamily = body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            if (saved) "★" else "+",
                            color = if (saved) p.secondary else p.dim,
                            fontFamily = body,
                            modifier = Modifier
                                .clickable { onToggleCar(track.id) }
                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        fontFamily = body,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .background(if (selected) p.accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        fontSize = 11.sp,
        maxLines = 1
    )
}

private fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
