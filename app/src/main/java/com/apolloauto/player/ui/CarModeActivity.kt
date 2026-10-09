package com.apolloauto.player.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.LocalHudPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

private enum class CarListMode { CAR_FAVORITES, ALL }

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
    val listTracks = if (listMode == CarListMode.CAR_FAVORITES) carTracks else tracks
    val inCar = playback.mediaId.isNotBlank() && app.playlistStore.isInCarFavorites(playback.mediaId)

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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("APOLLO", color = p.primary, fontSize = 34.sp)
                            Text(
                                "CAR LINK · ${source.name}",
                                color = p.secondary,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp
                            )
                        }
                        Text(
                            text = "EXIT",
                            color = p.dim,
                            modifier = Modifier
                                .border(1.dp, p.dim.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable(onClick = onExit)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.9f)
                                .aspectRatio(1f)
                        ) {
                            if (playback.artworkUri != null) {
                                AsyncImage(
                                    model = playback.artworkUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, p.primary.copy(alpha = 0.7f), CircleShape)
                                )
                            } else {
                                SpectrumOrb(playing = playback.playing, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NOW PLAYING", color = p.primary, letterSpacing = 2.sp, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                playback.title,
                                color = p.soft,
                                fontSize = 26.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                playback.artist,
                                color = p.dim,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(12.dp))
                            SeekRail(
                                progress = if (playback.durationMs > 0) {
                                    playback.positionMs.toFloat() / playback.durationMs
                                } else 0f
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${fmt(playback.positionMs)} / ${fmt(playback.durationMs)}",
                                color = p.dim,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            TransportRow(
                                playing = playback.playing,
                                shuffle = playback.shuffle,
                                repeatMode = playback.repeatMode,
                                onPrev = controller::skipPrevious,
                                onToggle = controller::togglePlayPause,
                                onNext = controller::skipNext,
                                onShuffle = controller::toggleShuffle,
                                onRepeat = controller::cycleRepeat
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                QuickAction(
                                    label = if (inCar) "IN CAR ★" else "SAVE TO CAR",
                                    active = inCar,
                                    onClick = {
                                        if (playback.mediaId.isNotBlank()) {
                                            app.playlistStore.toggleCarFavorite(playback.mediaId)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.DirectionsCar, null, tint = if (inCar) p.secondary else p.soft)
                                }
                                QuickAction(
                                    label = "ADD LIST",
                                    active = false,
                                    onClick = { if (playback.mediaId.isNotBlank()) addSheet = true }
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, null, tint = p.soft)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(status, color = p.accent.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .background(p.panel.copy(alpha = 0.72f))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("CAR ★ ${carTracks.size}", listMode == CarListMode.CAR_FAVORITES) {
                            listMode = CarListMode.CAR_FAVORITES
                        }
                        ModeChip("ALL ${tracks.size}", listMode == CarListMode.ALL) {
                            listMode = CarListMode.ALL
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (listTracks.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (listMode == CarListMode.CAR_FAVORITES) {
                                    "No Car Favorites yet. Tap SAVE TO CAR while a song plays."
                                } else {
                                    "No tracks. Scan phone music first."
                                },
                                color = p.dim,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(listTracks, key = { _, t -> t.id }) { index, track ->
                                val selected = track.id == playback.mediaId
                                val saved = app.playlistStore.isInCarFavorites(track.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (selected) p.secondary.copy(alpha = 0.18f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) p.secondary else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { controller.playTracks(listTracks, index) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        track.title,
                                        color = p.soft,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (saved) "★" else "+",
                                        color = if (saved) p.secondary else p.dim,
                                        modifier = Modifier
                                            .clickable {
                                                app.playlistStore.toggleCarFavorite(track.id)
                                            }
                                            .padding(start = 8.dp),
                                        fontSize = 18.sp
                                    )
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
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) p.accent else p.dim.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .background(if (selected) p.accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        fontSize = 12.sp
    )
}

@Composable
private fun QuickAction(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val p = LocalHudPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (active) p.secondary else p.primary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(if (active) p.secondary.copy(alpha = 0.2f) else p.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(label, color = p.soft, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SpectrumOrb(playing: Boolean, modifier: Modifier = Modifier) {
    val p = LocalHudPalette.current
    val infinite = rememberInfiniteTransition(label = "orb")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = Math.PI.toFloat() * 2f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(p.voidBlack)
            .border(2.dp, p.primary.copy(alpha = 0.7f), CircleShape)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(p.glowA.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.3f
            ),
            radius = radius * 1.3f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = p.primary.copy(alpha = 0.8f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
        val bars = 28
        for (i in 0 until bars) {
            val t = i / bars.toFloat()
            val angle = t * Math.PI.toFloat() * 2f
            val amp = if (playing) {
                (0.35f + 0.65f * ((sin((phase + t * 12f).toDouble()) + 1.0) / 2.0).toFloat())
            } else 0.2f
            val inner = radius * 0.55f
            val outer = radius * (0.62f + 0.32f * amp)
            val start = Offset(cx + kotlin.math.cos(angle) * inner, cy + kotlin.math.sin(angle) * inner)
            val end = Offset(cx + kotlin.math.cos(angle) * outer, cy + kotlin.math.sin(angle) * outer)
            drawLine(
                color = if (i % 2 == 0) p.primary else p.secondary,
                start = start,
                end = end,
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SeekRail(progress: Float) {
    val p = LocalHudPalette.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val h = size.height
        drawRoundRect(
            color = p.soft.copy(alpha = 0.12f),
            size = Size(size.width, h),
            cornerRadius = CornerRadius(h / 2f, h / 2f)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(p.primary, p.secondary)),
            size = Size(size.width * progress.coerceIn(0f, 1f), h),
            cornerRadius = CornerRadius(h / 2f, h / 2f)
        )
    }
}

@Composable
private fun TransportRow(
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val p = LocalHudPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BigControl(onClick = onShuffle) {
            Icon(Icons.Default.Shuffle, null, tint = if (shuffle) p.accent else p.dim, modifier = Modifier.size(26.dp))
        }
        BigControl(onClick = onPrev) {
            Icon(Icons.Default.SkipPrevious, null, tint = p.primary, modifier = Modifier.size(36.dp))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(p.secondary.copy(alpha = 0.45f), p.panel)))
                .border(2.dp, p.accent, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = p.soft,
                modifier = Modifier.size(40.dp)
            )
        }
        BigControl(onClick = onNext) {
            Icon(Icons.Default.SkipNext, null, tint = p.primary, modifier = Modifier.size(36.dp))
        }
        BigControl(onClick = onRepeat) {
            Icon(
                if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                null,
                tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun BigControl(onClick: () -> Unit, content: @Composable () -> Unit) {
    val p = LocalHudPalette.current
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.dp, p.primary.copy(alpha = 0.55f), CircleShape)
            .background(p.panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
