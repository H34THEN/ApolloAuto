package com.apolloauto.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.Track
import com.apolloauto.player.data.TrackMetadataFormatter
import com.apolloauto.player.data.displayArtist
import com.apolloauto.player.data.displayTitle
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.QueueEntry
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.components.ApolloArtPlaceholder
import com.apolloauto.player.ui.components.ArtworkThumb
import com.apolloauto.player.ui.hud.ApolloArtworkFrame
import com.apolloauto.player.ui.hud.ApolloHudProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class CarListMode { CAR_FAVORITES, ALL, QUEUE }
private enum class CarDeckMode { STANDARD, VISUALIZER }

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
    var panelExpanded by remember { mutableStateOf(true) }
    var deckMode by remember { mutableStateOf(CarDeckMode.STANDARD) }

    val carTracks = remember(tracks, playlists) {
        app.playlistStore.resolveById(PlaylistStore.CAR_FAVORITES_ID, tracks)
    }
    val queueEntries = playback.queue
    val listTracks = when (listMode) {
        CarListMode.CAR_FAVORITES -> carTracks
        CarListMode.ALL -> tracks
        CarListMode.QUEUE -> emptyList()
    }
    val inCar = remember(playback.mediaId, playlists) {
        playback.mediaId.isNotBlank() && app.playlistStore.isInCarFavorites(playback.mediaId)
    }
    val progress = if (playback.durationMs > 0) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f
    val metaLines = TrackMetadataFormatter.displayLines(playback.title, playback.artist, playback.album)
    val vizMode by app.visualizerController.mode.collectAsState()

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val session = app.playbackBridge?.audioSessionId() ?: 0
            if (session != 0) app.visualizerController.attach(session)
            app.visualizerController.setEnabled(true)
        }
    }

    DisposableEffect(Unit) {
        app.playlistStore.ensureCarFavorites()
        controller.connect()
        onDispose {
            app.visualizerController.setEnabled(false)
            controller.release()
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            controller.pollPosition()
            delay(350)
        }
    }

    LaunchedEffect(playback.playing) {
        if (playback.playing) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val session = app.playbackBridge?.audioSessionId() ?: 0
                if (session != 0) app.visualizerController.attach(session)
                app.visualizerController.setEnabled(true)
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            app.visualizerController.setEnabled(false)
        }
    }

    CyberBackground {
        Box(Modifier.fillMaxSize()) {
            GlitchHudOverlay(Modifier.fillMaxSize())
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(ApolloSpace.space12)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("APOLLO II", color = p.primary, fontFamily = hud, fontSize = 18.sp, letterSpacing = 1.sp)
                        Text(" // CAR", color = p.secondary, fontFamily = body, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            source.name,
                            color = p.soft,
                            fontFamily = body,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .border(1.dp, p.accent.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModeToggleChip(
                            label = if (deckMode == CarDeckMode.VISUALIZER) "VIZ ONLY" else "FULL HUD",
                            selected = deckMode == CarDeckMode.VISUALIZER
                        ) {
                            deckMode = if (deckMode == CarDeckMode.VISUALIZER) {
                                CarDeckMode.STANDARD
                            } else {
                                CarDeckMode.VISUALIZER
                            }
                        }
                        if (deckMode == CarDeckMode.STANDARD) {
                            ModeToggleChip(
                                label = if (panelExpanded) "HIDE LIST" else "SHOW LIST",
                                selected = panelExpanded
                            ) { panelExpanded = !panelExpanded }
                        }
                        Text(
                            "EXIT",
                            color = p.dim,
                            fontFamily = body,
                            modifier = Modifier
                                .border(1.dp, p.dim.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable(onClick = onExit)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(ApolloSpace.space8))

                when (deckMode) {
                    CarDeckMode.VISUALIZER -> {
                        VisualizerOnlyDeck(
                            title = metaLines.getOrElse(0) { playback.title },
                            artist = metaLines.getOrElse(1) { playback.artist },
                            playing = playback.playing,
                            onPrev = controller::skipPrevious,
                            onNext = controller::skipNext,
                            onToggle = controller::togglePlayPause,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CarDeckMode.STANDARD -> {
                        Row(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ApolloSpace.space8)
                        ) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .background(p.voidBlack.copy(alpha = 0.55f))
                                ) {
                                    ApolloVisualizerSurface(
                                        playing = playback.playing,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Corner tech marks
                                    Canvas(Modifier.fillMaxSize()) {
                                        val c = p.accent.copy(alpha = 0.7f)
                                        val len = 18f
                                        drawLine(c, Offset(8f, 8f), Offset(8f + len, 8f), 2f)
                                        drawLine(c, Offset(8f, 8f), Offset(8f, 8f + len), 2f)
                                        drawLine(c, Offset(size.width - 8f, 8f), Offset(size.width - 8f - len, 8f), 2f)
                                        drawLine(c, Offset(size.width - 8f, 8f), Offset(size.width - 8f, 8f + len), 2f)
                                    }
                                    VizModeChips(
                                        mode = vizMode,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) { m ->
                                        app.themeStore.setVisualizerMode(m)
                                        app.visualizerController.setMode(m)
                                    }
                                }
                                Spacer(Modifier.height(ApolloSpace.space8))
                                CarTransportBar(
                                    title = metaLines.getOrElse(0) { playback.title },
                                    artist = metaLines.getOrElse(1) { playback.artist },
                                    artworkUri = playback.artworkUri,
                                    playing = playback.playing,
                                    shuffle = playback.shuffle,
                                    repeatMode = playback.repeatMode,
                                    progress = progress,
                                    position = playback.positionMs,
                                    duration = playback.durationMs,
                                    inCar = inCar,
                                    onPrev = controller::skipPrevious,
                                    onToggle = controller::togglePlayPause,
                                    onNext = controller::skipNext,
                                    onShuffle = controller::toggleShuffle,
                                    onRemix = controller::remixShuffle,
                                    onRepeat = controller::cycleRepeat,
                                    onHeart = {
                                        if (playback.mediaId.isNotBlank()) {
                                            app.playlistStore.toggleCarFavorite(playback.mediaId)
                                        }
                                    },
                                    onAdd = { if (playback.mediaId.isNotBlank()) addSheet = true }
                                )
                                if (status.isNotBlank()) {
                                    Text(
                                        status,
                                        color = p.dim,
                                        fontFamily = body,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = panelExpanded,
                                enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                            ) {
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
                                        .width(300.dp)
                                        .fillMaxHeight(),
                                    onMode = { listMode = it },
                                    onPlay = { list, index -> controller.playTracks(list, index) },
                                    onPlayQueueIndex = controller::seekToQueueIndex,
                                    onToggleCar = { app.playlistStore.toggleCarFavorite(it) }
                                )
                            }

                            if (!panelExpanded) {
                                Box(
                                    Modifier
                                        .width(36.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .background(p.panel.copy(alpha = 0.8f))
                                        .clickable { panelExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, "Expand list", tint = p.primary)
                                }
                            }
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
private fun VisualizerOnlyDeck(
    title: String,
    artist: String,
    playing: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Box(modifier = modifier.fillMaxSize()) {
        ApolloVisualizerSurface(
            playing = playing,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .background(p.voidBlack)
        )
        val app = ApolloApp.instance
        val vizMode by app.visualizerController.mode.collectAsState()
        VizModeChips(
            mode = vizMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(10.dp)
        ) { m ->
            app.themeStore.setVisualizerMode(m)
            app.visualizerController.setMode(m)
        }
        // Tap center to play/pause
        Box(
            Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .clip(CircleShape)
                .border(1.dp, p.accent.copy(alpha = 0.5f), CircleShape)
                .background(p.voidBlack.copy(alpha = 0.35f))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) "Pause" else "Play",
                tint = p.soft,
                modifier = Modifier.size(36.dp)
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, p.voidBlack.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        "Previous",
                        tint = p.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        color = p.soft,
                        fontFamily = body,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        artist,
                        color = p.dim,
                        fontFamily = body,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        "Next",
                        tint = p.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CarTransportBar(
    title: String,
    artist: String,
    artworkUri: android.net.Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    progress: Float,
    position: Long,
    duration: Long,
    inCar: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRemix: () -> Unit,
    onRepeat: () -> Unit,
    onHeart: () -> Unit,
    onAdd: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val heartScale by animateFloatAsState(if (inCar) 1.12f else 1f, label = "heart")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .background(p.panel.copy(alpha = 0.88f))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ApolloArtworkFrame(Modifier.size(56.dp)) {
                if (artworkUri != null) {
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ApolloArtPlaceholder(Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onPrev, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipPrevious, "Previous", tint = p.primary, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = p.soft, fontFamily = body, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = p.dim, fontFamily = body, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                ApolloHudProgress(progress, height = 8.dp)
                Text(
                    "${fmt(position)} / ${fmt(duration)}",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.SkipNext, "Next", tint = p.primary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(
                    Icons.Default.Shuffle,
                    if (shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (shuffle) p.accent else p.dim
                )
            }
            IconButton(
                onClick = onRemix,
                enabled = shuffle,
                modifier = Modifier.size(ApolloSpace.touchMin)
            ) {
                Icon(
                    Icons.Default.Tune,
                    "Remix shuffle",
                    tint = if (shuffle) p.secondary else p.dim.copy(alpha = 0.35f)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = onRepeat, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary
                )
            }
            IconButton(onClick = onHeart, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(
                    if (inCar) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (inCar) "Remove from Car Favorites" else "Add to Car Favorites",
                    tint = if (inCar) p.accent else p.dim,
                    modifier = Modifier.size((22 * heartScale).dp)
                )
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.PlaylistAdd, "Add to playlist", tint = p.primary)
            }
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
            .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(ApolloSpace.corner))
            .background(p.panel.copy(alpha = 0.82f))
            .padding(ApolloSpace.space8)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BROWSER", color = p.primary, fontFamily = body, fontSize = 11.sp, letterSpacing = 1.sp)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = p.dim, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeChip("CAR $carCount", listMode == CarListMode.CAR_FAVORITES, Modifier.weight(1f)) {
                onMode(CarListMode.CAR_FAVORITES)
            }
            ModeChip("ALL $allCount", listMode == CarListMode.ALL, Modifier.weight(1f)) {
                onMode(CarListMode.ALL)
            }
            ModeChip("Q $queueCount", listMode == CarListMode.QUEUE, Modifier.weight(1f)) {
                onMode(CarListMode.QUEUE)
            }
        }
        Spacer(Modifier.height(6.dp))
        when (listMode) {
            CarListMode.QUEUE -> {
                if (queueEntries.isEmpty()) {
                    Text("Queue empty — play a track.", color = p.dim, fontFamily = body, fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(queueEntries, key = { i, e -> "${e.mediaId}#$i" }) { index, entry ->
                            val selected = entry.isCurrent || entry.mediaId == playingId
                            val lines = TrackMetadataFormatter.displayLines(entry.title, entry.artist, entry.album)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (selected) p.accent.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { onPlayQueueIndex(index) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        lines[0],
                                        color = if (selected) p.accent else p.soft,
                                        fontFamily = body,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (lines.size > 1) {
                                        Text(lines[1], color = p.dim, fontFamily = body, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                if (listTracks.isEmpty()) {
                    Text(
                        if (listMode == CarListMode.CAR_FAVORITES) "No Car Favorites yet." else "No tracks.",
                        color = p.dim,
                        fontFamily = body,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(listTracks, key = { _, t -> t.id }) { index, track ->
                            val selected = track.id == playingId
                            val saved = playlistStore.isInCarFavorites(track.id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (selected) p.accent.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { onPlay(listTracks, index) }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkThumb(artworkUri = track.artworkUri, size = 36.dp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        track.displayTitle(),
                                        color = if (selected) p.accent else p.soft,
                                        fontFamily = body,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(track.displayArtist(), color = p.dim, fontFamily = body, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(
                                    if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (saved) p.accent else p.dim,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { onToggleCar(track.id) }
                                        .padding(10.dp)
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
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        fontFamily = body,
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
            .background(if (selected) p.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp)
    )
}

@Composable
private fun VizModeChips(
    mode: VisualizerMode,
    modifier: Modifier = Modifier,
    onMode: (VisualizerMode) -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            VisualizerMode.SPECTRUM to "FFT",
            VisualizerMode.BARS to "BARS",
            VisualizerMode.WAVEFORM to "WAVE",
            VisualizerMode.CIRCULAR to "ORB",
            VisualizerMode.RINGS to "RINGS",
            VisualizerMode.LEVEL to "VU"
        ).forEach { (m, label) ->
            Text(
                label,
                color = if (mode == m) p.soft else p.dim,
                fontFamily = body,
                fontSize = 9.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        1.dp,
                        if (mode == m) p.accent else p.dim.copy(alpha = 0.35f),
                        RoundedCornerShape(3.dp)
                    )
                    .background(
                        if (mode == m) p.accent.copy(alpha = 0.18f)
                        else p.voidBlack.copy(alpha = 0.45f)
                    )
                    .clickable { onMode(m) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ModeToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        fontFamily = body,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .background(if (selected) p.accent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun GlitchHudOverlay(modifier: Modifier = Modifier) {
    val p = LocalHudPalette.current
    Canvas(modifier) {
        // Sparse scanlines + noise bars for cyber HUD atmosphere (static, not flashing).
        val step = 6f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = p.primary.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
        drawLine(
            color = p.accent.copy(alpha = 0.12f),
            start = Offset(0f, size.height * 0.33f),
            end = Offset(size.width, size.height * 0.33f),
            strokeWidth = 1f
        )
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
