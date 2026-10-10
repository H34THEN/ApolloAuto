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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.animation.core.animateFloatAsState
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
import com.apolloauto.player.data.TrackMetadataFormatter
import com.apolloauto.player.data.displayArtist
import com.apolloauto.player.data.displayTitle
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.QueueEntry
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.ApolloSpace
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
    val queueEntries = playback.queue
    val listTracks = when (listMode) {
        CarListMode.CAR_FAVORITES -> carTracks
        CarListMode.ALL -> tracks
        CarListMode.QUEUE -> emptyList() // rendered from queueEntries
    }
    val inCar = remember(playback.mediaId, playlists) {
        playback.mediaId.isNotBlank() && app.playlistStore.isInCarFavorites(playback.mediaId)
    }
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
                                onToggleCarFavorite = {
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
                                queueCount = queueEntries.size,
                                listTracks = listTracks,
                                queueEntries = queueEntries,
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                modifier = Modifier.weight(1f),
                                onMode = { listMode = it },
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onPlayQueueIndex = controller::seekToQueueIndex,
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
                                onToggleCarFavorite = {
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
                                queueCount = queueEntries.size,
                                listTracks = listTracks,
                                queueEntries = queueEntries,
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onMode = { listMode = it },
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onPlayQueueIndex = controller::seekToQueueIndex,
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
    onToggleCarFavorite: () -> Unit,
    onAddList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val lines = TrackMetadataFormatter.displayLines(title, artist, album)
    val heartScale by animateFloatAsState(if (inCar) 1.12f else 1f, label = "heart")
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
                    lines.getOrElse(0) { title },
                    color = p.soft,
                    fontFamily = body,
                    fontSize = if (compact) 20.sp else 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (lines.size > 1) {
                    Text(
                        lines[1],
                        color = p.dim,
                        fontFamily = body,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lines.size > 2 && !compact) {
                    Text(
                        lines[2],
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) p.accent else p.dim)
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = p.primary, modifier = Modifier.size(30.dp))
            }
            Box(
                modifier = Modifier
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
                Icon(Icons.Default.SkipNext, "Next", tint = p.primary, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onRepeat, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary
                )
            }
            IconButton(onClick = onToggleCarFavorite, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(
                    if (inCar) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (inCar) "Remove from Car Favorites" else "Add to Car Favorites",
                    tint = if (inCar) p.accent else p.dim,
                    modifier = Modifier.size((24 * heartScale).dp)
                )
            }
            IconButton(onClick = onAddList, modifier = Modifier.size(ApolloSpace.touchMin)) {
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
    queueEntries: List<QueueEntry>,
    playingId: String,
    playlistStore: PlaylistStore,
    onMode: (CarListMode) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
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
        when (listMode) {
            CarListMode.QUEUE -> {
                if (queueEntries.isEmpty()) {
                    EmptyBrowseHint(
                        title = "Queue is empty.",
                        detail = "Play a track to build a stable session queue."
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(queueEntries, key = { index, e -> "${e.mediaId}#$index" }) { index, entry ->
                            val selected = entry.isCurrent || entry.mediaId == playingId
                            val lines = TrackMetadataFormatter.displayLines(entry.title, entry.artist, entry.album)
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
                                    .clickable { onPlayQueueIndex(index) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkThumb(artworkUri = entry.artworkUri, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (selected) "NOW · ${lines[0]}" else lines[0],
                                        color = if (selected) p.accent else p.soft,
                                        fontFamily = body,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 15.sp
                                    )
                                    if (lines.size > 1) {
                                        Text(
                                            lines[1],
                                            color = p.dim,
                                            fontFamily = body,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CarListMode.CAR_FAVORITES, CarListMode.ALL -> {
                if (listTracks.isEmpty()) {
                    EmptyBrowseHint(
                        title = if (listMode == CarListMode.CAR_FAVORITES) {
                            "No Car Favorites yet."
                        } else {
                            "No tracks. Scan phone music first."
                        },
                        detail = if (listMode == CarListMode.CAR_FAVORITES) {
                            "Tap the heart while a song plays to save it here."
                        } else {
                            "Return to Home and scan the phone library."
                        }
                    )
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
                                        track.displayTitle(),
                                        color = if (selected) p.accent else p.soft,
                                        fontFamily = body,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        track.displayArtist(),
                                        color = p.dim,
                                        fontFamily = body,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    if (saved) "In Car Favorites" else "Add to Car Favorites",
                                    tint = if (saved) p.accent else p.dim,
                                    modifier = Modifier
                                        .size(ApolloSpace.touchMin)
                                        .clickable { onToggleCar(track.id) }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBrowseHint(title: String, detail: String) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(ApolloSpace.space12),
        verticalArrangement = Arrangement.Top
    ) {
        Icon(Icons.Default.FavoriteBorder, null, tint = p.dim, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(ApolloSpace.space8))
        Text(title, color = p.soft, fontFamily = body, fontSize = 15.sp)
        Spacer(Modifier.height(ApolloSpace.space4))
        Text(detail, color = p.dim, fontFamily = body, fontSize = 13.sp)
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
