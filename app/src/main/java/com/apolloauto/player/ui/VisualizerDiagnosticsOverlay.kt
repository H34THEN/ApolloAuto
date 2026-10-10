package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.media.VisualizerBackgroundPolicy
import com.apolloauto.player.theme.LocalBodyFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VisualizerDiagnosticsOverlay(modifier: Modifier = Modifier) {
    val app = ApolloApp.instance
    val body = LocalBodyFont.current
    val features by app.visualizerController.features.collectAsState()
    val capture by app.visualizerController.captureDiagnostics.collectAsState()
    val scene by app.visualizerController.sceneRuntime.diagnostics.collectAsState()
    val mode by app.visualizerController.mode.collectAsState()
    val bg by app.themeStore.vizBackgroundMode.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(250)
            tick++
        }
    }
    val effective = VisualizerBackgroundPolicy.resolve(bg, mode)
    val age = app.visualizerController.frameAgeMs().let { if (it == Long.MAX_VALUE) -1 else it }
    // Reference tick so Compose re-reads age each pulse.
    val lines = listOf(
        "tick=$tick src=${capture.captureSource} session=${capture.audioSessionId}",
        "capturing=${capture.capturing} paused=${capture.playbackPaused} stale=${capture.stale}",
        "analysisFps=${"%.0f".format(capture.analysisFps)} frameAgeMs=$age",
        "rms=${"%.2f".format(features.rms)} sub=${"%.2f".format(features.subBass)} bass=${"%.2f".format(features.bass)}",
        "lowMid=${"%.2f".format(features.lowMid)} upMid=${"%.2f".format(features.upperMid)} tre=${"%.2f".format(features.treble)}",
        "flux=${"%.2f".format(features.spectralFlux)} onset=${"%.2f".format(features.onset)}",
        "scene=${mode.name} uptimeMs=${scene.sceneUptimeMs}",
        "creates=${scene.createCount} resetsUnexpected=${scene.unexpectedResetCount} autodj=${scene.autoDjTransitionCount}",
        "bg=$bg → ${effective.preference} (${effective.note})",
        "attach=${capture.attachCount} interrupts=${capture.interruptCount}"
    )
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(8.dp)
    ) {
        lines.forEach { s ->
            Text(s, color = Color(0xFFB8FFC8), fontFamily = body, fontSize = 9.sp)
        }
    }
}
