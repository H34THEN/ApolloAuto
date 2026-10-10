package com.apolloauto.player.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.companion.CompanionVoice
import com.apolloauto.player.data.LrcDocument
import com.apolloauto.player.data.LyricsLoader
import com.apolloauto.player.data.TrackMetadataFormatter
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class ImmersiveVisualizerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloCyberTheme {
                ImmersiveVisualizerScreen(onExit = { finish() })
            }
        }
    }
}

@Composable
private fun ImmersiveVisualizerScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val controller = remember { PlayerController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    val mode by app.visualizerController.mode.collectAsState()
    val randomizer by app.themeStore.vizRandomizer.collectAsState()
    val enabledModes by app.themeStore.enabledVisualizerModes.collectAsState()
    val driveMode by app.themeStore.driveMode.collectAsState()
    val libraryTracks by app.libraryStore.tracks.collectAsState()
    val view = LocalView.current

    DisposableEffect(driveMode) {
        val window = (view.context as? Activity)?.window
        if (driveMode) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val lines = TrackMetadataFormatter.displayLines(playback.title, playback.artist, playback.album)
    val title = lines.getOrElse(0) { playback.title }
    val artist = lines.getOrElse(1) { playback.artist }

    var lyricsDoc by remember { mutableStateOf<LrcDocument?>(null) }
    var showLyrics by remember { mutableStateOf(false) }
    val lyricsAvailable = lyricsDoc != null && lyricsDoc!!.lines.isNotEmpty()

    fun applyMode(next: com.apolloauto.player.media.VisualizerMode) {
        app.themeStore.setVisualizerMode(next)
        app.visualizerController.setMode(next)
    }

    fun cycleNext() {
        applyMode(VisualizerModeCatalog.nextMode(mode, enabledModes))
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

    DisposableEffect(Unit) {
        app.visualizerController.setMode(app.themeStore.visualizerMode.value)
        controller.connect()
        onDispose {
            app.visualizerController.setEnabled(false)
            controller.release()
        }
    }

    LaunchedEffect(showLyrics) {
        val interval = if (showLyrics) 120L else 400L
        while (isActive) {
            controller.pollPosition()
            delay(interval)
        }
    }

    LaunchedEffect(playback.mediaId, libraryTracks) {
        val track = libraryTracks.firstOrNull { it.id == playback.mediaId }
        lyricsDoc = LyricsLoader.load(context, track)
        if (lyricsDoc == null || lyricsDoc!!.lines.isEmpty()) {
            showLyrics = false
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

    LaunchedEffect(randomizer, playback.playing) {
        if (!randomizer) return@LaunchedEffect
        while (isActive && app.themeStore.vizRandomizer.value) {
            val waitMs = Random.nextLong(8_000L, 28_001L)
            delay(waitMs)
            if (!app.themeStore.vizRandomizer.value) break
            val current = app.visualizerController.mode.value
            val enabled = app.themeStore.enabledVisualizerModes.value
            applyMode(VisualizerModeCatalog.randomMode(excluding = current, enabled = enabled))
        }
    }

    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        Modifier
            .fillMaxSize()
            .background(p.voidBlack)
    ) {
        // Full-bleed blurred album art — Crop so there are no letterbox gaps.
        if (playback.artworkUri != null) {
            AsyncImage(
                model = playback.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canBlur) Modifier.blur(28.dp) else Modifier)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        p.voidBlack.copy(alpha = if (canBlur) 0.32f else 0.48f)
                    )
            )
        }

        ApolloVisualizerSurface(
            playing = playback.playing,
            clearBackdrop = true,
            modifier = Modifier.fillMaxSize()
        )

        // Very light edge fade — keeps chrome readable without eating the art.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            p.voidBlack.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.Transparent,
                            p.voidBlack.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        // Top-right tools: viz cycle, randomizer, lyrics, exit.
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImmersiveGhostButton(onClick = { cycleNext() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Next visualizer",
                    tint = p.soft.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }
            ImmersiveGhostButton(
                selected = randomizer || driveMode,
                onClick = { app.themeStore.toggleVizRandomizer() }
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = if (randomizer) "Randomizer on" else "Randomizer off",
                    tint = if (randomizer) p.accent.copy(alpha = 0.9f) else p.soft.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
            ImmersiveGhostButton(
                selected = driveMode,
                onClick = {
                    val on = app.themeStore.toggleDriveMode()
                    app.themeStore.speak(
                        if (on) CompanionVoice.driveOnLine() else CompanionVoice.driveOffLine()
                    )
                }
            ) {
                Text(
                    "DRV",
                    color = if (driveMode) p.accent else p.soft.copy(alpha = 0.45f),
                    fontFamily = body,
                    fontSize = 10.sp
                )
            }
            ImmersiveGhostButton(
                selected = showLyrics && lyricsAvailable,
                enabled = lyricsAvailable,
                onClick = {
                    if (lyricsAvailable) showLyrics = !showLyrics
                }
            ) {
                Icon(
                    Icons.Default.Lyrics,
                    contentDescription = if (lyricsAvailable) {
                        if (showLyrics) "Hide lyrics" else "Show lyrics"
                    } else {
                        "Lyrics unavailable"
                    },
                    tint = when {
                        !lyricsAvailable -> p.dim.copy(alpha = 0.28f)
                        showLyrics -> p.accent
                        else -> p.soft.copy(alpha = 0.95f)
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(if (lyricsAvailable) 1f else 0.55f)
                )
            }
            IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Exit",
                    tint = p.dim.copy(alpha = 0.5f)
                )
            }
        }

        if (showLyrics && lyricsAvailable) {
            KaraokeLyricsOverlay(
                document = lyricsDoc!!,
                positionMs = playback.positionMs,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.88f)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 72.dp)
            )
        }

        // Driving-safe cluster: transport + title, bottom-left, low opacity.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 14.dp, bottom = 14.dp, end = 14.dp)
                .widthIn(max = 320.dp)
                .alpha(0.72f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImmersiveTransportButton(
                    onClick = controller::skipPrevious,
                    contentDescription = "Previous"
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = p.soft.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                ImmersiveTransportButton(
                    onClick = controller::togglePlayPause,
                    contentDescription = if (playback.playing) "Pause" else "Play",
                    emphasized = true
                ) {
                    Icon(
                        if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = p.soft.copy(alpha = 0.9f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                ImmersiveTransportButton(
                    onClick = controller::skipNext,
                    contentDescription = "Next"
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = p.soft.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    title,
                    color = p.soft.copy(alpha = 0.75f),
                    fontFamily = body,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    color = p.dim.copy(alpha = 0.65f),
                    fontFamily = body,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    VisualizerModeCatalog.compactLabel(mode) +
                        if (randomizer) " · RND" else "",
                    color = p.dim.copy(alpha = 0.45f),
                    fontFamily = body,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun KaraokeLyricsOverlay(
    document: LrcDocument,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val active = document.activeIndex(positionMs)
    val prev = document.lines.getOrNull(active - 1)?.text
    val current = document.lines.getOrNull(active)?.text
        ?: document.lines.firstOrNull()?.text.orEmpty()
    val next = document.lines.getOrNull(active + 1)?.text

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.voidBlack.copy(alpha = 0.28f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            prev.orEmpty(),
            color = p.dim.copy(alpha = if (prev == null) 0f else 0.45f),
            fontFamily = body,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
        Text(
            current,
            color = p.soft,
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            next.orEmpty(),
            color = p.dim.copy(alpha = if (next == null) 0f else 0.5f),
            fontFamily = body,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
    }
}

@Composable
private fun ImmersiveGhostButton(
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val p = LocalHudPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(
                1.dp,
                when {
                    !enabled -> p.dim.copy(alpha = 0.15f)
                    selected -> p.accent.copy(alpha = 0.55f)
                    else -> p.primary.copy(alpha = 0.2f)
                },
                CircleShape
            )
            .background(
                when {
                    !enabled -> p.voidBlack.copy(alpha = 0.15f)
                    selected -> p.accent.copy(alpha = 0.12f)
                    else -> p.voidBlack.copy(alpha = 0.22f)
                }
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ImmersiveTransportButton(
    onClick: () -> Unit,
    contentDescription: String,
    emphasized: Boolean = false,
    content: @Composable () -> Unit
) {
    val p = LocalHudPalette.current
    Box(
        modifier = Modifier
            .size(if (emphasized) 48.dp else 44.dp)
            .clip(CircleShape)
            .border(
                1.dp,
                if (emphasized) p.accent.copy(alpha = 0.35f) else p.primary.copy(alpha = 0.18f),
                CircleShape
            )
            .background(p.voidBlack.copy(alpha = 0.22f))
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
