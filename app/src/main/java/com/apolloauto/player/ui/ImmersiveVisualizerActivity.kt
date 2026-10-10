package com.apolloauto.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.TrackMetadataFormatter
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.shortLabel
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.ApolloHudProgress
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
    val hud = LocalHudFont.current
    val body = LocalBodyFont.current
    val controller = remember { PlayerController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    val mode by app.visualizerController.mode.collectAsState()
    val captureSource by app.visualizerController.captureSource.collectAsState()
    var overlayVisible by remember { mutableStateOf(true) }

    val progress = if (playback.durationMs > 0) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f
    val titleLine = TrackMetadataFormatter.displayLines(playback.title, playback.artist, playback.album)
        .firstOrNull() ?: playback.title

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

    Box(
        Modifier
            .fillMaxSize()
            .background(p.voidBlack)
    ) {
        if (!overlayVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { overlayVisible = true }
            ) {
                ApolloVisualizerSurface(
                    playing = playback.playing,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            ApolloVisualizerSurface(
                playing = playback.playing,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { overlayVisible = false }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                p.voidBlack.copy(alpha = 0.72f),
                                Color.Transparent,
                                Color.Transparent,
                                p.voidBlack.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "IMMERSIVE VISUALIZER",
                            color = p.primary,
                            fontFamily = hud,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            captureSource.shortLabel(),
                            color = p.dim,
                            fontFamily = body,
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { overlayVisible = false }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide overlay", tint = p.dim)
                        }
                        IconButton(onClick = onExit) {
                            Icon(Icons.Default.Close, contentDescription = "Exit", tint = p.soft)
                        }
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        titleLine,
                        color = p.soft,
                        fontFamily = body,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ApolloHudProgress(progress, height = 10.dp, modifier = Modifier.fillMaxWidth())
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = controller::skipPrevious) {
                            Icon(Icons.Default.SkipPrevious, null, tint = p.soft)
                        }
                        IconButton(onClick = controller::togglePlayPause) {
                            Icon(
                                if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                tint = p.accent
                            )
                        }
                        IconButton(onClick = controller::skipNext) {
                            Icon(Icons.Default.SkipNext, null, tint = p.soft)
                        }
                    }
                    VisualizerModeCompactChips(
                        selected = mode,
                        onSelect = { m ->
                            app.themeStore.setVisualizerMode(m)
                            app.visualizerController.setMode(m)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}
