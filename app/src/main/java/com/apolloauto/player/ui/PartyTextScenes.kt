package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.LrcDocument
import com.apolloauto.player.data.LyricsLoader
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.theme.E1d1cText
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.theme.ThemeStore
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Fun text visualizers for Car Mode / Immersive.
 * Always render with baked-in [E1d1cText] regardless of HUD font setting.
 */

@Composable
fun LyricsRainbowOverlay(
    features: AudioFeatures,
    photosensitive: Boolean,
    modifier: Modifier = Modifier
) {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val libraryTracks by app.libraryStore.tracks.collectAsState()
    var positionMs by remember { mutableLongStateOf(0L) }
    var mediaId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lyricsDoc by remember { mutableStateOf<LrcDocument?>(null) }
    var monoMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { nanos ->
                if (last != 0L) {
                    monoMs += ((nanos - last) / 1_000_000f).coerceIn(0f, 48f)
                }
                last = nanos
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val bridge = app.playbackBridge
            positionMs = bridge?.currentPositionMs() ?: 0L
            mediaId = bridge?.currentMediaId().orEmpty()
            title = bridge?.currentTitle().orEmpty()
            delay(80L)
        }
    }

    LaunchedEffect(mediaId, libraryTracks) {
        val track = libraryTracks.firstOrNull { it.id == mediaId }
        lyricsDoc = LyricsLoader.load(app, track)
    }

    val doc = lyricsDoc
    val active = doc?.activeIndex(positionMs) ?: -1
    val prev = doc?.lines?.getOrNull(active - 1)?.text
    val current = doc?.lines?.getOrNull(active)?.text
        ?: title.takeIf { it.isNotBlank() }
        ?: "NO LYRICS LOADED"
    val next = doc?.lines?.getOrNull(active + 1)?.text
    val fallbackHint = if (doc == null || doc.lines.isEmpty()) {
        "Drop a .lrc next to the track — or just vibe with the title"
    } else null

    val lyricsSp by app.themeStore.lyricsFontSp.collectAsState()
    val beat = (features.bass * 0.55f + features.onset * 0.85f + features.rms * 0.25f)
        .coerceIn(0f, 1f)
        .let { if (photosensitive) it * 0.45f else it }
    val baseScale = 1f + beat * 0.28f

    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!prev.isNullOrBlank()) {
                RainbowPulseLine(
                    text = prev,
                    monoMs = monoMs,
                    baseScale = 0.72f,
                    beat = beat * 0.35f,
                    alpha = 0.4f,
                    fontSizeSp = lyricsSp * 0.55f
                )
            }
            RainbowPulseLine(
                text = current,
                monoMs = monoMs,
                baseScale = baseScale,
                beat = beat,
                alpha = 1f,
                fontSizeSp = lyricsSp
            )
            if (!next.isNullOrBlank()) {
                RainbowPulseLine(
                    text = next,
                    monoMs = monoMs,
                    baseScale = 0.78f,
                    beat = beat * 0.4f,
                    alpha = 0.45f,
                    fontSizeSp = lyricsSp * 0.55f
                )
            }
            if (fallbackHint != null) {
                Text(
                    fallbackHint,
                    color = p.dim.copy(alpha = 0.7f),
                    fontFamily = E1d1cText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PartyMarqueeOverlay(
    features: AudioFeatures,
    photosensitive: Boolean,
    modifier: Modifier = Modifier
) {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val marqueeText by app.themeStore.partyMarqueeText.collectAsState()
    val partySp by app.themeStore.partyScrollFontSp.collectAsState()
    val lines = remember(marqueeText) {
        marqueeText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .ifEmpty {
                ThemeStore.DEFAULT_PARTY_MARQUEE.lines().map { it.trim() }.filter { it.isNotEmpty() }
            }
    }
    var lineIndex by remember { mutableIntStateOf(0) }
    var monoMs by remember { mutableFloatStateOf(0f) }
    var scrollX by remember { mutableFloatStateOf(0f) }
    var textWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { nanos ->
                if (last != 0L) {
                    monoMs += ((nanos - last) / 1_000_000f).coerceIn(0f, 48f)
                }
                last = nanos
            }
        }
    }

    LaunchedEffect(lines) {
        if (lineIndex >= lines.size) lineIndex = 0
    }

    val message = lines.getOrElse(lineIndex) { ThemeStore.DEFAULT_PARTY_MARQUEE.lines().first() }
    val beat = (features.bass * 0.5f + features.onset * 0.7f)
        .coerceIn(0f, 1f)
        .let { if (photosensitive) it * 0.4f else it }
    var speedPxPerSec by remember { mutableFloatStateOf(160f) }
    speedPxPerSec = (120f + features.rms * 90f + beat * 50f).coerceIn(90f, 280f)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val screenW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val fallbackTextW = with(density) {
            (message.length * 26.sp.toPx()).coerceAtLeast(screenW * 0.8f)
        }
        // One coroutine owns scroll + line advance: never cut a message mid-pass.
        LaunchedEffect(message, screenW, lines.size) {
            scrollX = screenW
            var waits = 0
            while (textWidthPx < 8f && waits < 12 && isActive) {
                delay(16L)
                waits++
            }
            // Include pulse scale headroom so the last glyphs clear the screen.
            val travel = (if (textWidthPx > 8f) textWidthPx else fallbackTextW) * 1.35f
            var last = 0L
            while (isActive) {
                withFrameNanos { nanos ->
                    if (last != 0L) {
                        val dt = ((nanos - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        scrollX -= speedPxPerSec * dt
                    }
                    last = nanos
                }
                if (scrollX <= -travel) {
                    delay(400L)
                    if (lines.isNotEmpty()) {
                        lineIndex = (lineIndex + 1) % lines.size
                    }
                    break
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "PARTY SCROLL",
                color = p.dim.copy(alpha = 0.55f),
                fontFamily = E1d1cText,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = scrollX
                        scaleY = 1f + beat * 0.18f
                        scaleX = 1f + beat * 0.06f
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                RainbowPulseLine(
                    text = message,
                    monoMs = monoMs,
                    baseScale = 1f + beat * 0.12f,
                    beat = beat,
                    alpha = 1f,
                    fontSizeSp = partySp,
                    singleLine = true,
                    onWidthPx = { textWidthPx = it }
                )
            }
            Text(
                "${lineIndex + 1} / ${lines.size}  ·  Layers sheet or Settings → Visualizer",
                color = p.dim.copy(alpha = 0.5f),
                fontFamily = E1d1cText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/**
 * Edit goofy / snarky marquee lines — used in Settings and Car Mode Layers sheet.
 */
@Composable
fun PartyMarqueeEditor(modifier: Modifier = Modifier) {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val marquee by app.themeStore.partyMarqueeText.collectAsState()
    val partySp by app.themeStore.partyScrollFontSp.collectAsState()
    val lyricsSp by app.themeStore.lyricsFontSp.collectAsState()
    val sliderColors = SliderDefaults.colors(
        thumbColor = p.accent,
        activeTrackColor = p.primary,
        inactiveTrackColor = p.dim.copy(alpha = 0.35f)
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "PARTY SCROLL MESSAGES",
            color = p.soft,
            fontFamily = body,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Text(
            "One message per line. Each line finishes scrolling off-screen before the next starts.",
            color = p.dim,
            fontFamily = body,
            fontSize = 11.sp
        )
        BasicTextField(
            value = marquee,
            onValueChange = { app.themeStore.setPartyMarqueeText(it) },
            textStyle = TextStyle(
                color = p.soft,
                fontFamily = body,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp, max = 200.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .background(p.voidBlack.copy(alpha = 0.55f))
                .padding(10.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PresetChip("PARTY", marquee == ThemeStore.DEFAULT_PARTY_MARQUEE) {
                app.themeStore.setPartyMarqueeText(ThemeStore.DEFAULT_PARTY_MARQUEE)
            }
            PresetChip("SNARKY", marquee == ThemeStore.SNARKY_MUSIC_MARQUEE) {
                app.themeStore.setPartyMarqueeText(ThemeStore.SNARKY_MUSIC_MARQUEE)
            }
        }
        Text(
            "Scroll size ${"%.0f".format(partySp)} sp",
            color = p.soft,
            fontFamily = body,
            fontSize = 11.sp
        )
        Slider(
            value = partySp,
            onValueChange = { app.themeStore.setPartyScrollFontSp(it) },
            valueRange = 22f..72f,
            colors = sliderColors
        )
        Text(
            "Lyrics size ${"%.0f".format(lyricsSp)} sp",
            color = p.soft,
            fontFamily = body,
            fontSize = 11.sp
        )
        Slider(
            value = lyricsSp,
            onValueChange = { app.themeStore.setLyricsFontSp(it) },
            valueRange = 18f..64f,
            colors = sliderColors
        )
        Text(
            "SNARKY = music quips. Font sizes also under Settings → Display.",
            color = p.dim.copy(alpha = 0.75f),
            fontFamily = body,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.voidBlack else p.soft,
        fontFamily = body,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) p.accent else p.voidBlack.copy(alpha = 0.45f))
            .border(1.dp, if (selected) p.accent else p.primary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun RainbowPulseLine(
    text: String,
    monoMs: Float,
    baseScale: Float,
    beat: Float,
    alpha: Float,
    fontSizeSp: Float,
    singleLine: Boolean = false,
    onWidthPx: ((Float) -> Unit)? = null
) {
    val chars = text.toList()
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer {
                scaleX = baseScale
                scaleY = baseScale
            }
            .then(if (singleLine) Modifier else Modifier.fillMaxWidth())
            .then(
                if (onWidthPx != null) {
                    Modifier.onSizeChanged { onWidthPx(it.width.toFloat()) }
                } else {
                    Modifier
                }
            )
    ) {
        chars.forEachIndexed { i, ch ->
            val hue = ((i * 28f) + monoMs * 0.045f) % 360f
            val letterPulse = 1f + beat * 0.22f * (0.55f + 0.45f * sin(i * 0.7f + monoMs * 0.01f))
            val sat = (0.85f + beat * 0.12f).coerceIn(0f, 1f)
            val value = (0.95f * alpha).coerceIn(0f, 1f)
            Text(
                ch.toString(),
                color = Color.hsv(hue, sat, value),
                fontFamily = E1d1cText,
                fontWeight = FontWeight.Normal,
                fontSize = (fontSizeSp * letterPulse).sp,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
        }
    }
}
