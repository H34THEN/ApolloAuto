package com.apolloauto.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.KenneyFuture
import com.apolloauto.player.theme.KenneyFutureNarrow
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.theme.PlayerLayoutMode
import com.apolloauto.player.ui.hud.ApolloArtworkFrame
import com.apolloauto.player.ui.hud.ApolloHudButton
import com.apolloauto.player.ui.hud.ApolloHudIconButton
import com.apolloauto.player.ui.hud.ApolloHudLabel
import com.apolloauto.player.ui.hud.ApolloHudProgress
import com.apolloauto.player.ui.hud.ApolloHudTab
import com.apolloauto.player.ui.hud.ApolloSciFiPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class NowPlayingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloCyberTheme {
                NowPlayingScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val layout by app.themeStore.playerLayout.collectAsState()
    val controller = remember { PlayerController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    val capturing by app.visualizerController.capturing.collectAsState()
    val mode by app.visualizerController.mode.collectAsState()
    var addSheet by remember { mutableStateOf(false) }
    val playlists by app.playlistStore.playlists.collectAsState()
    val inCar = playback.mediaId.isNotBlank() &&
        app.playlistStore.isInCarFavorites(playback.mediaId)

    DisposableEffect(Unit) {
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

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val session = app.playbackBridge?.audioSessionId() ?: 0
            if (session != 0) app.visualizerController.attach(session)
            app.visualizerController.setEnabled(true)
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
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NOW PLAYING",
                    color = p.primary,
                    fontFamily = KenneyFuture,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    "CLOSE",
                    color = p.dim,
                    modifier = Modifier
                        .border(1.dp, p.dim.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontFamily = KenneyFutureNarrow,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlayerLayoutMode.entries.forEach { m ->
                    ApolloHudTab(
                        label = when (m) {
                            PlayerLayoutMode.TACTICAL -> "TACTICAL"
                            PlayerLayoutMode.IMMERSIVE -> "IMMERSE"
                            PlayerLayoutMode.TERMINAL -> "TERMINAL"
                        },
                        selected = layout == m,
                        onClick = { app.themeStore.setPlayerLayout(m) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            when (layout) {
                PlayerLayoutMode.TACTICAL -> TacticalLayout(
                    playbackTitle = playback.title,
                    artist = playback.artist,
                    artwork = playback.artworkUri,
                    playing = playback.playing,
                    shuffle = playback.shuffle,
                    repeatMode = playback.repeatMode,
                    progress = progressOf(playback.positionMs, playback.durationMs),
                    position = playback.positionMs,
                    duration = playback.durationMs,
                    inCar = inCar,
                    capturing = capturing,
                    mode = mode,
                    onPrev = controller::skipPrevious,
                    onToggle = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onShuffle = controller::toggleShuffle,
                    onRepeat = controller::cycleRepeat,
                    onCar = {
                        if (playback.mediaId.isNotBlank()) {
                            app.playlistStore.toggleCarFavorite(playback.mediaId)
                        }
                    },
                    onAdd = { addSheet = true },
                    onEq = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    onTimer = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    onMode = { app.visualizerController.setMode(it) }
                )
                PlayerLayoutMode.IMMERSIVE -> ImmersiveLayout(
                    playbackTitle = playback.title,
                    artist = playback.artist,
                    artwork = playback.artworkUri,
                    playing = playback.playing,
                    progress = progressOf(playback.positionMs, playback.durationMs),
                    inCar = inCar,
                    onPrev = controller::skipPrevious,
                    onToggle = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onCar = {
                        if (playback.mediaId.isNotBlank()) {
                            app.playlistStore.toggleCarFavorite(playback.mediaId)
                        }
                    }
                )
                PlayerLayoutMode.TERMINAL -> TerminalLayout(
                    playbackTitle = playback.title,
                    artist = playback.artist,
                    artwork = playback.artworkUri,
                    playing = playback.playing,
                    shuffle = playback.shuffle,
                    repeatMode = playback.repeatMode,
                    progress = progressOf(playback.positionMs, playback.durationMs),
                    position = playback.positionMs,
                    duration = playback.durationMs,
                    capturing = capturing,
                    onPrev = controller::skipPrevious,
                    onToggle = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onShuffle = controller::toggleShuffle,
                    onRepeat = controller::cycleRepeat
                )
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

@Composable
private fun TacticalLayout(
    playbackTitle: String,
    artist: String,
    artwork: android.net.Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    progress: Float,
    position: Long,
    duration: Long,
    inCar: Boolean,
    capturing: Boolean,
    mode: VisualizerMode,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onCar: () -> Unit,
    onAdd: () -> Unit,
    onEq: () -> Unit,
    onTimer: () -> Unit,
    onMode: (VisualizerMode) -> Unit
) {
    val p = LocalHudPalette.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ApolloArtworkFrame(
                Modifier
                    .weight(0.9f)
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ApolloSciFiPanel(Modifier.weight(1.1f), title = "SIGNAL") {
                Text(
                    if (capturing) "LIVE FFT" else "NO CAPTURE",
                    color = if (capturing) p.accent else p.dim,
                    fontFamily = KenneyFutureNarrow,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                ApolloVisualizerSurface(
                    playing = playing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(1.dp, p.primary.copy(alpha = 0.3f))
                        .background(p.voidBlack)
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        VisualizerMode.SPECTRUM to "FFT",
                        VisualizerMode.WAVEFORM to "WAVE",
                        VisualizerMode.CIRCULAR to "ORB",
                        VisualizerMode.LEVEL to "LVL"
                    ).forEach { (m, label) ->
                        Text(
                            label,
                            color = if (mode == m) p.soft else p.dim,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .border(1.dp, if (mode == m) p.accent else p.dim.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                .clickable { onMode(m) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        ApolloSciFiPanel(title = "TRACK DATA") {
            Text(playbackTitle, color = p.soft, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = p.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            ApolloHudProgress(progress)
            Spacer(Modifier.height(4.dp))
            Text(
                "${fmt(position)} / ${fmt(duration)}",
                color = p.dim,
                fontFamily = KenneyFutureNarrow,
                fontSize = 12.sp
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApolloHudIconButton(onClick = onShuffle) {
                Icon(Icons.Default.Shuffle, null, tint = if (shuffle) p.accent else p.dim, modifier = Modifier.size(22.dp))
            }
            ApolloHudIconButton(onClick = onPrev) {
                Icon(Icons.Default.SkipPrevious, null, tint = p.soft, modifier = Modifier.size(28.dp))
            }
            ApolloHudIconButton(onClick = onToggle, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = p.soft,
                    modifier = Modifier.size(34.dp)
                )
            }
            ApolloHudIconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, null, tint = p.soft, modifier = Modifier.size(28.dp))
            }
            ApolloHudIconButton(onClick = onRepeat) {
                Icon(
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    null,
                    tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApolloHudButton(if (inCar) "IN CAR" else "SAVE CAR", onClick = onCar, modifier = Modifier.weight(1f))
            ApolloHudButton("ADD LIST", onClick = onAdd, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApolloHudButton("EQ", onClick = onEq, modifier = Modifier.weight(1f))
            ApolloHudButton("TIMER", onClick = onTimer, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImmersiveLayout(
    playbackTitle: String,
    artist: String,
    artwork: android.net.Uri?,
    playing: Boolean,
    progress: Float,
    inCar: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onCar: () -> Unit
) {
    val p = LocalHudPalette.current
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(p.voidBlack.copy(alpha = 0.25f), p.voidBlack.copy(alpha = 0.85f))
                    )
                )
        )
        ApolloVisualizerSurface(
            playing = playing,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 24.dp)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(playbackTitle, color = p.soft, fontSize = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = p.dim, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            ApolloHudProgress(progress)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ApolloHudIconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, null, tint = p.soft)
                }
                ApolloHudIconButton(onClick = onToggle, modifier = Modifier.size(68.dp)) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = p.soft, modifier = Modifier.size(36.dp))
                }
                ApolloHudIconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, null, tint = p.soft)
                }
            }
            Spacer(Modifier.height(8.dp))
            ApolloHudButton(if (inCar) "IN CAR ★" else "SAVE TO CAR", onClick = onCar, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}

@Composable
private fun TerminalLayout(
    playbackTitle: String,
    artist: String,
    artwork: android.net.Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    progress: Float,
    position: Long,
    duration: Long,
    capturing: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val p = LocalHudPalette.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .border(1.dp, p.primary.copy(alpha = 0.4f))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                ApolloHudLabel("TERMINAL MODE")
                Text(playbackTitle, color = p.soft, fontFamily = KenneyFutureNarrow, fontSize = 16.sp, maxLines = 2)
                Text(artist, color = p.dim, fontFamily = KenneyFutureNarrow, fontSize = 12.sp)
                Text(
                    "POS ${fmt(position)}  DUR ${fmt(duration)}  VIZ ${if (capturing) "ON" else "OFF"}",
                    color = p.dim,
                    fontFamily = KenneyFutureNarrow,
                    fontSize = 11.sp
                )
            }
        }
        ApolloHudProgress(progress, height = 14.dp)
        ApolloVisualizerSurface(
            playing = playing,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, p.dim.copy(alpha = 0.35f))
                .background(p.voidBlack)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                Triple(onShuffle, Icons.Default.Shuffle, if (shuffle) p.accent else p.dim),
                Triple(onPrev, Icons.Default.SkipPrevious, p.soft),
                Triple(onToggle, if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, p.accent),
                Triple(onNext, Icons.Default.SkipNext, p.soft),
                Triple(
                    onRepeat,
                    if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary
                )
            ).forEach { (click, icon, tint) ->
                Box(
                    Modifier
                        .size(48.dp)
                        .border(1.dp, p.primary.copy(alpha = 0.35f))
                        .clickable(onClick = click),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tint)
                }
            }
        }
    }
}

private fun progressOf(position: Long, duration: Long): Float {
    if (duration <= 0L) return 0f
    return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
