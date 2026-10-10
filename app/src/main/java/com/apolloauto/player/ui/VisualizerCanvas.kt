package com.apolloauto.player.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.R
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualizerBackgroundPolicy
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.media.VizSizePreset
import com.apolloauto.player.media.boostSpectrum
import com.apolloauto.player.media.resolveVisualizerMode
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.theme.LocalMotionProfile
import com.apolloauto.player.theme.MotionProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ApolloVisualizerSurface(
    playing: Boolean,
    modifier: Modifier = Modifier,
    /** Skip opaque void fills so album art / black can show through. */
    clearBackdrop: Boolean = false
) {
    val themePalette = LocalHudPalette.current
    val app = ApolloApp.instance
    val artTint by app.albumArtColors.tint.collectAsState()
    val artLock by app.themeStore.artColorLock.collectAsState()
    val beatFlashRaw by app.themeStore.beatFlash.collectAsState()
    val photosensitive by app.themeStore.photosensitiveMode.collectAsState()
    val beatFlash = if (photosensitive) beatFlashRaw.coerceAtMost(0.35f) else beatFlashRaw
    val cinematic by app.themeStore.cinematicViz.collectAsState()
    val tranceProfile by app.themeStore.tranceProfile.collectAsState()
    val paletteMode by app.themeStore.vizPaletteMode.collectAsState()
    val quality by app.themeStore.vizQuality.collectAsState()
    val vizSat by app.themeStore.vizSaturation.collectAsState()
    val vizGlow by app.themeStore.vizGlow.collectAsState()
    val spiralVariant by app.themeStore.spiralVariant.collectAsState()
    val spiralColor by app.themeStore.spiralColorPreset.collectAsState()
    val obsidianPreset by app.themeStore.obsidianPreset.collectAsState()
    val vissonSub by app.themeStore.vissonanceSubmode.collectAsState()
    val smokePreset by app.themeStore.smokeColorPreset.collectAsState()
    val smokeDensity by app.themeStore.smokeDensity.collectAsState()
    val smokeTurb by app.themeStore.smokeTurbulence.collectAsState()
    val smokeFlow by app.themeStore.smokeFlow.collectAsState()
    val smokeBass by app.themeStore.smokeBassPressure.collectAsState()
    val smokeSoft by app.themeStore.smokeSoftness.collectAsState()
    val smokeGlowCtrl by app.themeStore.smokeGlowCtrl.collectAsState()
    val smokeSatCtrl by app.themeStore.smokeSatCtrl.collectAsState()
    val smokePlume by app.themeStore.smokePlume.collectAsState()
    val smokeAudio by app.themeStore.smokeAudio.collectAsState()
    val smokeContrast by app.themeStore.smokeContrast.collectAsState()
    val smokeChromatic by app.themeStore.smokeChromatic.collectAsState()
    val polyScale by app.themeStore.polySpiralScale.collectAsState()
    val polyFill by app.themeStore.polyFillMode.collectAsState()
    val polyCx by app.themeStore.polyCenterX.collectAsState()
    val polyCy by app.themeStore.polyCenterY.collectAsState()
    val sceneBgOverrides by app.themeStore.sceneBgOverrides.collectAsState()
    val visualIntensity by app.themeStore.visualIntensity.collectAsState()
    val vizSizePresets by app.themeStore.vizSizePresets.collectAsState()
    app.albumArtColors.colorLock = artLock
    val p = artTint?.applyTo(themePalette) ?: themePalette
    val motion = LocalMotionProfile.current
    val viz = app.visualizerController
    val fft by viz.fft.collectAsState()
    val wave by viz.waveform.collectAsState()
    val level by viz.level.collectAsState()
    val bass by viz.bass.collectAsState()
    val featuresRaw by viz.features.collectAsState()
    val features = featuresRaw.withProfile(tranceProfile).withIntensity(visualIntensity)
    val capturing by viz.capturing.collectAsState()
    val modeRaw by viz.mode.collectAsState()
    val mode = resolveVisualizerMode(modeRaw)
    val sizePreset = vizSizePresets[mode] ?: VizSizePreset.M
    val bgMode by app.themeStore.vizBackgroundMode.collectAsState()
    val animate = motion != MotionProfile.STATIC
    val cinematicAmt = if (cinematic) 1f else 0f
    val sceneOverride = sceneBgOverrides[mode]
    val effectiveBg = remember(bgMode, mode, sceneOverride) {
        VisualizerBackgroundPolicy.resolve(bgMode, mode, sceneOverride)
    }
    // Force clear backdrop when BLACK/ALBUM or AUTO→BLACK so scenes don't paint opaque void.
    val resolvedClear = clearBackdrop || effectiveBg.clearSceneBackdrop

    // Seamless decorative pulse only — never used as absolute scene phase.
    val spin = rememberInfiniteTransition(label = "vizSpin")
    val pulse by spin.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    /**
     * Monotonic animation clocks (V4.2 root-cause fix).
     * Previous warpClock tween Restart every 3200ms snapped all TR/cosmic phases to 0.
     */
    var monoMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        viz.sceneRuntime.ensureRenderClock()
        var last = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (last != 0L) {
                    val dtMs = ((nanos - last) / 1_000_000f).coerceIn(0f, 48f)
                    monoMs += dtMs
                }
                last = nanos
            }
        }
    }
    val spinAngle = (monoMs * 0.04f) % 360f
    val warpClock = monoMs * 0.12f // continuous; never restarts

    // Full-bleed canvas — size presets scale geometry from center (never the backdrop).
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // Pure black underlay unless album art is meant to show through.
            if (!resolvedClear) {
                drawRect(androidx.compose.ui.graphics.Color.Black)
            }
            // Scale all viz geometry from center. Scenes skip their own full-rect fills
            // so we never get a shrinking black "box". Animation clocks are outside this.
            val geomScale = sizePreset.scale.coerceIn(0.45f, 1.25f)
            // Parent owns backdrop (black underlay or album layer).
            val sceneClear = true
            scale(
                scaleX = geomScale,
                scaleY = geomScale,
                pivot = Offset(size.width * 0.5f, size.height * 0.5f)
            ) {
            val cosmicScene = when (mode) {
                VisualizerMode.HYPERDRIVE,
                VisualizerMode.NEBULA,
                VisualizerMode.RAVE_CORE,
                VisualizerMode.SIGNAL_OCEAN,
                VisualizerMode.PLASMA_HEART,
                VisualizerMode.FRACTURE,
                VisualizerMode.WAVEBARS,
                VisualizerMode.MIRROR_BARS,
                VisualizerMode.PULSE,
                VisualizerMode.PARTICLES,
                VisualizerMode.DNA_HELIX,
                VisualizerMode.DNA_WAVE,
                VisualizerMode.BEAT_TUNNEL,
                VisualizerMode.BEAT_CASCADE,
                VisualizerMode.BASS_NATION,
                VisualizerMode.BUBBLE_BASS,
                VisualizerMode.CIRCULAR_PULSE,
                VisualizerMode.COLOR_SIGNAL,
                VisualizerMode.DUAL_SIGNAL,
                VisualizerMode.MIXED_WAVE,
                VisualizerMode.SHAPE_WAVE,
                VisualizerMode.STAR_FIELD,
                VisualizerMode.STARS2,
                VisualizerMode.SPACE_WARP,
                VisualizerMode.XR3_OSCILLO,
                VisualizerMode.XR3_CIRCLE,
                VisualizerMode.XR3_POLY,
                VisualizerMode.XR3_BARS,
                VisualizerMode.XR3_VU,
                VisualizerMode.XR3_SIERPINSKI,
                VisualizerMode.XR3_JULIA,
                VisualizerMode.XR3_SPRITE,
                VisualizerMode.CHROMATIC_PHI,
                VisualizerMode.ECTOPLASM,
                VisualizerMode.CIRC_INFINITY,
                VisualizerMode.SYNTH_SMOKE,
                VisualizerMode.NOTE_SWARM,
                VisualizerMode.HARMONIC_MANDALA,
                VisualizerMode.LIQUID_SPECTRUM,
                VisualizerMode.AUDIBLE_SPIRALS,
                VisualizerMode.FLUCTUS_FLOW,
                VisualizerMode.WAVEFORM_VOID,
                VisualizerMode.OBSIDIAN_REACTOR,
                VisualizerMode.VISSONANCE_MATRIX,
                VisualizerMode.PROJECTM_PLASMA,
                VisualizerMode.PROJECTM_SCOPE,
                VisualizerMode.MILKDROP_WAVE,
                VisualizerMode.MILKDROP_ROTOR,
                VisualizerMode.AV_BAR,
                VisualizerMode.AV_BLAST,
                VisualizerMode.AV_BLOB,
                VisualizerMode.AV_WAVE,
                VisualizerMode.AV_CIRCLE_BARS,
                VisualizerMode.AV_HIFI,
                VisualizerMode.BAUER_FOUNTAIN,
                VisualizerMode.BAUER_FFT,
                VisualizerMode.BAUER_SPEC,
                VisualizerMode.DNA_CIRCLE,
                VisualizerMode.DNA_SPIRAL,
                VisualizerMode.TOPO_CONTOUR,
                VisualizerMode.WIRE_TERRAIN,
                VisualizerMode.NEON_TOPO,
                VisualizerMode.PSY_KALEIDO,
                VisualizerMode.ACID_TUNNEL,
                VisualizerMode.LYRICS_RAINBOW,
                VisualizerMode.PARTY_MARQUEE,
                VisualizerMode.ST01_GRAF_EQ,
                VisualizerMode.ST02_LUNCHBREAK,
                VisualizerMode.ST03_CYBER_FUJI,
                VisualizerMode.ML_BLACK_HOLE,
                VisualizerMode.ML_EVENT_HORIZON,
                VisualizerMode.ML_XOR_PLASMA,
                VisualizerMode.ML_INFERNAL_MESH,
                VisualizerMode.ML_ASTRAL_COLLAPSE,
                VisualizerMode.ML_NEON_RITUAL,
                VisualizerMode.ML_LIQUID_WARP,
                VisualizerMode.ML_GLITCH_CORE,
                VisualizerMode.ML_CHAOS_TUNNEL,
                VisualizerMode.ML_FRACTAL_BITE -> true
                else -> false
            }
            if (!cosmicScene && !resolvedClear) {
                val gridStep = size.width / 12f
                for (i in 0..12) {
                    val x = i * gridStep
                    drawLine(
                        color = p.primary.copy(alpha = 0.05f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }
                for (i in 0..6) {
                    val y = i * (size.height / 6f)
                    drawLine(
                        color = p.primary.copy(alpha = 0.04f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
            }
            // Use sceneClear (not resolvedClear) for scene backdrop flags below.

            val idle = FloatArray(fft.size) { i ->
                val t = (spinAngle / 360f) * Math.PI.toFloat() * 2f
                (0.1f + 0.07f * sin(t + i * 0.4f) + if (playing) 0.04f * sin(t * 2f + i) else 0f)
                    .coerceIn(0f, 1f)
            }
            // Prefer live buffers whenever capture is on (even briefly paused).
            val live = capturing && animate
            // Intensity gain applies to FFT geometry for every mode (immersive + car).
            val spectrum = boostSpectrum(if (live) fft else idle, visualIntensity)
            val waveData = if (live) wave else FloatArray(wave.size) { i ->
                0.12f * sin((spinAngle / 40f) + i * 0.28f)
            }
            val lvl = if (live) level else 0.18f + 0.08f * (pulse - 1f)
            val bassLvl = if (live) bass else lvl * 0.7f
            val energy = (lvl * 0.55f + bassLvl * 0.45f).coerceIn(0f, 1f)
            val liveFeatures = if (live) {
                features
            } else {
                AudioFeatures(
                    subBass = bassLvl * 0.8f,
                    bass = bassLvl,
                    lowMid = energy * 0.55f,
                    upperMid = energy * 0.5f,
                    presence = energy * 0.4f,
                    treble = energy * 0.45f,
                    mid = energy * 0.65f,
                    high = energy * 0.45f,
                    rms = lvl,
                    peak = lvl,
                    spectralFlux = 0f,
                    onset = 0f,
                    silence = 1f - energy
                )
            }
            val fastPhase = warpClock

            when (mode) {
                VisualizerMode.SPECTRUM, VisualizerMode.DECORATIVE -> {
                    val barCount = spectrum.size
                    val gap = size.width * 0.008f
                    val barW = (size.width - gap * (barCount + 1)) / barCount
                    val midY = size.height * 0.52f
                    spectrum.forEachIndexed { i, v ->
                        val amp = (v * (0.75f + energy * 0.55f)).coerceIn(0f, 1.2f)
                        val h = size.height * (0.08f + amp * 0.78f)
                        val x = gap + i * (barW + gap)
                        val color = when {
                            i < barCount * 0.2f -> p.secondary
                            i > barCount * 0.75f -> p.accent
                            else -> p.primary
                        }
                        drawRoundRect(
                            color = color.copy(alpha = 0.2f + energy * 0.15f),
                            topLeft = Offset(x - 2f, midY - h * 0.55f),
                            size = Size(barW + 4f, h * 1.1f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.98f), color.copy(alpha = 0.3f))
                            ),
                            topLeft = Offset(x, midY - h * 0.5f),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = color.copy(alpha = 0.2f),
                            topLeft = Offset(x, midY + h * 0.06f),
                            size = Size(barW, h * 0.32f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                    drawLine(
                        color = p.accent.copy(alpha = 0.4f + energy * 0.35f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f + energy * 2f
                    )
                }

                VisualizerMode.BARS -> {
                    val count = (spectrum.size / 2).coerceAtLeast(16)
                    val gap = size.width * 0.01f
                    val barW = (size.width - gap * (count + 1)) / count
                    val floor = size.height * 0.92f
                    for (i in 0 until count) {
                        val v = ((spectrum[i * 2] + spectrum[(i * 2 + 1).coerceAtMost(spectrum.lastIndex)]) * 0.5f)
                            .coerceIn(0f, 1f)
                        val h = size.height * (0.06f + v * 0.82f)
                        val x = gap + i * (barW + gap)
                        val color = Brush.verticalGradient(
                            listOf(p.accent, p.primary, p.secondary.copy(alpha = 0.7f))
                        )
                        drawRoundRect(
                            color = p.primary.copy(alpha = 0.12f + v * 0.2f),
                            topLeft = Offset(x - 1f, floor - h - 4f),
                            size = Size(barW + 2f, h + 8f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            brush = color,
                            topLeft = Offset(x, floor - h),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Peak cap
                        drawRect(
                            color = p.soft.copy(alpha = 0.75f),
                            topLeft = Offset(x, floor - h - 3f),
                            size = Size(barW, 3f)
                        )
                    }
                }

                VisualizerMode.WAVEFORM -> {
                    if (waveData.size > 1) {
                        val mid = size.height * 0.5f
                        val amp = size.height * (0.28f + energy * 0.22f)
                        val fill = Path().apply {
                            moveTo(0f, mid)
                            for (i in waveData.indices) {
                                val x = size.width * i / (waveData.size - 1f)
                                val y = mid + waveData[i] * amp
                                lineTo(x, y)
                            }
                            lineTo(size.width, mid)
                            close()
                        }
                        drawPath(
                            fill,
                            brush = Brush.verticalGradient(
                                listOf(
                                    p.primary.copy(alpha = 0.35f + energy * 0.25f),
                                    p.accent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        for (i in 0 until waveData.lastIndex) {
                            val x0 = size.width * i / (waveData.size - 1f)
                            val x1 = size.width * (i + 1) / (waveData.size - 1f)
                            val y0 = mid + waveData[i] * amp
                            val y1 = mid + waveData[i + 1] * amp
                            drawLine(
                                color = p.accent.copy(alpha = 0.95f),
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 2.5f + energy * 2f,
                                cap = StrokeCap.Round
                            )
                        }
                        // Ghost secondary wave
                        for (i in 0 until waveData.lastIndex step 2) {
                            val x0 = size.width * i / (waveData.size - 1f)
                            val x1 = size.width * (i + 1).coerceAtMost(waveData.lastIndex) / (waveData.size - 1f)
                            val y0 = mid - waveData[i] * amp * 0.55f
                            val y1 = mid - waveData[(i + 1).coerceAtMost(waveData.lastIndex)] * amp * 0.55f
                            drawLine(
                                color = p.secondary.copy(alpha = 0.45f),
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 1.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                VisualizerMode.CIRCULAR -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val base = size.minDimension * (0.16f + bassLvl * 0.08f) * (if (playing) pulse else 1f)
                    rotate(spinAngle * 0.12f + energy * 8f, Offset(cx, cy)) {
                        spectrum.forEachIndexed { i, v ->
                            val ang = (i / spectrum.size.toFloat()) * Math.PI.toFloat() * 2f
                            val outer = base + size.minDimension * 0.34f * (v * (0.7f + energy * 0.5f))
                            val c = when {
                                i % 3 == 0 -> p.primary
                                i % 3 == 1 -> p.secondary
                                else -> p.accent
                            }
                            drawLine(
                                color = c.copy(alpha = 0.85f + v * 0.15f),
                                start = Offset(cx + cos(ang) * base, cy + sin(ang) * base),
                                end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                                strokeWidth = 2.5f + v * 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    drawCircle(
                        color = p.primary.copy(alpha = 0.35f),
                        radius = base * 1.05f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = p.accent.copy(alpha = 0.18f + energy * 0.45f),
                        radius = base * (0.5f + energy * 0.45f),
                        center = Offset(cx, cy)
                    )
                    rotate(spinAngle * (1f + energy), Offset(cx, cy)) {
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(p.accent.copy(alpha = 0.9f), Color.Transparent)
                            ),
                            start = Offset(cx, cy),
                            end = Offset(cx + base * (1.6f + energy), cy),
                            strokeWidth = 3f
                        )
                    }
                }

                VisualizerMode.RINGS -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.46f
                    val ringCount = 6
                    for (r in 0 until ringCount) {
                        val band = spectrum[(r * spectrum.size / ringCount).coerceIn(0, spectrum.lastIndex)]
                        val radius = maxR * ((r + 1) / ringCount.toFloat()) * (0.55f + band * 0.55f + bassLvl * 0.12f)
                        val color = if (r % 2 == 0) p.primary else p.secondary
                        drawCircle(
                            color = color.copy(alpha = 0.25f + band * 0.55f),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f + band * 6f + energy * 2f)
                        )
                    }
                    // Spokes driven by FFT
                    val spokes = 24
                    for (i in 0 until spokes) {
                        val ang = (i / spokes.toFloat()) * Math.PI.toFloat() * 2f + spinAngle * 0.01f
                        val v = spectrum[(i * spectrum.size / spokes).coerceIn(0, spectrum.lastIndex)]
                        val inner = maxR * 0.12f
                        val outer = maxR * (0.2f + v * 0.55f)
                        drawLine(
                            color = p.accent.copy(alpha = 0.35f + v * 0.5f),
                            start = Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
                            end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = p.accent.copy(alpha = 0.15f + energy * 0.4f),
                        radius = maxR * (0.1f + energy * 0.18f),
                        center = Offset(cx, cy)
                    )
                }

                VisualizerMode.LEVEL, VisualizerMode.MINIMAL -> {
                    val left = (bassLvl * 0.7f + lvl * 0.3f).coerceIn(0f, 1f)
                    val right = (lvl * 0.75f + energy * 0.25f).coerceIn(0f, 1f)
                    val trackH = size.height * 0.18f
                    val gap = size.height * 0.08f
                    val topL = size.height * 0.28f
                    val topR = topL + trackH + gap
                    fun meter(top: Float, value: Float, labelBias: Float) {
                        drawRoundRect(
                            color = p.dim.copy(alpha = 0.35f),
                            topLeft = Offset(size.width * 0.06f, top),
                            size = Size(size.width * 0.88f, trackH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(p.primary, p.accent, p.secondary)
                            ),
                            topLeft = Offset(size.width * 0.06f, top),
                            size = Size(size.width * 0.88f * value, trackH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        // Peak needle
                        val nx = size.width * 0.06f + size.width * 0.88f * value
                        drawLine(
                            color = p.soft.copy(alpha = 0.9f),
                            start = Offset(nx, top - 4f),
                            end = Offset(nx, top + trackH + 4f),
                            strokeWidth = 2.5f
                        )
                        for (i in 1..10) {
                            val x = size.width * 0.06f + size.width * 0.88f * (i / 10f)
                            drawLine(
                                color = p.soft.copy(alpha = 0.2f + labelBias),
                                start = Offset(x, top - 2f),
                                end = Offset(x, top + trackH + 2f),
                                strokeWidth = 1f
                            )
                        }
                    }
                    meter(topL, left, 0.05f)
                    meter(topR, right, 0.1f)
                }

                VisualizerMode.HYPERDRIVE -> drawWireframeSphereScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    pulse = pulse,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.NEBULA -> drawNebulaScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    pulse = pulse,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.RAVE_CORE -> drawRaveCoreScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    fft = spectrum,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.SIGNAL_OCEAN -> drawSignalOceanScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    wave = waveData,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.PLASMA_HEART -> drawPlasmaHeartScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    pulse = pulse,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.FRACTURE -> drawFractureScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    fft = spectrum,
                    spin = spinAngle + fastPhase * 0.05f,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.WAVEBARS -> drawWavebarsScene(
                    features = liveFeatures,
                    palette = p,
                    wave = waveData,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.MIRROR_BARS -> drawMirrorBarsScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.PULSE -> drawPulseScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.PARTICLES -> drawParticlesScene(
                    features = liveFeatures,
                    palette = p,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.BASS_NATION -> drawBassNationScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.BUBBLE_BASS -> drawBubbleBassScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.CIRCULAR_PULSE -> drawCircularPulseMvScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.COLOR_SIGNAL -> drawColorSignalScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.DUAL_SIGNAL -> drawDualSignalScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.MIXED_WAVE -> drawMixedWaveScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.SHAPE_WAVE -> drawShapeWaveScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.STAR_FIELD,
                VisualizerMode.STARS2,
                VisualizerMode.SPACE_WARP -> drawCosmicWarpScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.DNA_HELIX -> drawDnaHelixScene(
                    features = liveFeatures,
                    palette = p,
                    wave = waveData,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear,
                    cinematic = cinematicAmt
                )

                VisualizerMode.DNA_WAVE -> drawDnaWaveScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.BEAT_TUNNEL -> drawBeatTunnelScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear,
                    beatFlash = beatFlash,
                    cinematic = cinematicAmt
                )

                VisualizerMode.BEAT_CASCADE -> drawBeatCascadeScene(
                    features = liveFeatures,
                    palette = p,
                    fft = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear,
                    beatFlash = beatFlash
                )

                VisualizerMode.XR3_OSCILLO -> drawXr3OscilloScene(
                    features = liveFeatures,
                    palette = p,
                    wave = waveData,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.XR3_CIRCLE -> drawXr3CircleScene(
                    features = liveFeatures,
                    palette = p,
                    spectrum = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.XR3_POLY -> drawXr3PolyScene(
                    features = liveFeatures,
                    palette = p,
                    spectrum = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear,
                    spiralScale = polyScale,
                    fillMode = polyFill,
                    centerXOffset = polyCx,
                    centerYOffset = polyCy
                )

                VisualizerMode.XR3_BARS -> drawXr3BarsScene(
                    features = liveFeatures,
                    palette = p,
                    spectrum = spectrum,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.XR3_VU -> drawXr3VuScene(
                    features = liveFeatures,
                    palette = p,
                    level = lvl,
                    bass = bassLvl,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                // Retired: fall back to POLY rather than broken SIER geometry.
                VisualizerMode.XR3_SIERPINSKI -> drawXr3PolyScene(
                    features = liveFeatures,
                    palette = p,
                    spectrum = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear,
                    spiralScale = polyScale,
                    fillMode = polyFill,
                    centerXOffset = polyCx,
                    centerYOffset = polyCy
                )

                VisualizerMode.XR3_JULIA -> drawXr3JuliaScene(
                    features = liveFeatures,
                    palette = p,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.XR3_SPRITE -> drawXr3SpriteScene(
                    features = liveFeatures,
                    palette = p,
                    spectrum = spectrum,
                    phase = fastPhase,
                    playing = playing,
                    clearBackdrop = sceneClear
                )

                VisualizerMode.CHROMATIC_PHI -> drawChromaticPhiScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.ECTOPLASM -> drawEctoplasmScene(
                    liveFeatures, p, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.CIRC_INFINITY -> drawCircInfinityScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.PRISM_RAIN -> drawTopoContourScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.SYNTH_SMOKE -> drawSynthSmokeScene(
                    liveFeatures, p, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow,
                    smokePreset = smokePreset,
                    controls = SmokeControls(
                        density = smokeDensity,
                        turbulence = smokeTurb,
                        flowSpeed = smokeFlow,
                        bassPressure = smokeBass,
                        softness = smokeSoft,
                        glow = smokeGlowCtrl,
                        saturation = smokeSatCtrl,
                        plumeSize = smokePlume,
                        audioSensitivity = smokeAudio,
                        densityContrast = smokeContrast,
                        chromaticVibration = smokeChromatic
                    )
                )
                VisualizerMode.NOTE_SWARM -> drawNoteSwarmScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.HARMONIC_MANDALA -> drawHarmonicMandalaScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.LIQUID_SPECTRUM -> drawLiquidSpectrumScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.AUDIBLE_SPIRALS -> drawAudibleSpiralsScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow,
                    variant = spiralVariant,
                    spiralPreset = spiralColor
                )
                VisualizerMode.FLUCTUS_FLOW -> drawFluctusFlowScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.WAVEFORM_VOID -> drawWaveformVoidScene(
                    liveFeatures, p, spectrum, waveData, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow
                )
                VisualizerMode.OBSIDIAN_REACTOR -> drawObsidianReactorScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow,
                    crystalPreset = obsidianPreset
                )
                VisualizerMode.VISSONANCE_MATRIX -> drawVissonanceMatrixScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear,
                    paletteMode, quality, vizSat, vizGlow,
                    submode = vissonSub
                )
                VisualizerMode.LYRICS_RAINBOW,
                VisualizerMode.PARTY_MARQUEE -> {
                    if (!resolvedClear) {
                        drawRect(Color.Black)
                    }
                }
                VisualizerMode.PROJECTM_PLASMA -> drawProjectmPlasmaScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.PROJECTM_SCOPE -> drawProjectmScopeScene(
                    p, waveData, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.MILKDROP_WAVE -> drawMilkdropWaveScene(
                    liveFeatures, p, waveData, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.MILKDROP_ROTOR -> drawMilkdropRotorScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.AV_BAR -> drawAvBarScene(p, spectrum, playing, sceneClear)
                VisualizerMode.AV_BLAST -> drawAvBlastScene(p, spectrum, playing, sceneClear)
                VisualizerMode.AV_BLOB -> drawAvBlobScene(p, spectrum, fastPhase, playing, sceneClear)
                VisualizerMode.AV_WAVE -> drawAvWaveFilledScene(p, spectrum, playing, sceneClear)
                VisualizerMode.AV_CIRCLE_BARS -> drawAvCircleLineScene(
                    p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.AV_HIFI -> drawAvHifiScene(p, spectrum, playing, sceneClear)
                VisualizerMode.BAUER_FOUNTAIN -> drawBauerFountainScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.BAUER_FFT -> drawBauerFftScene(
                    liveFeatures, p, spectrum, playing, sceneClear
                )
                VisualizerMode.BAUER_SPEC -> drawBauerSpecScene(p, spectrum, playing, sceneClear)
                VisualizerMode.DNA_CIRCLE -> drawDnaCircleScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.DNA_SPIRAL -> {
                    val pos = app.playbackBridge?.currentPositionMs()?.toFloat() ?: 0f
                    // Prefer live song progress; always blend mono clock so the spiral keeps painting.
                    val songT = if (pos > 0f) (pos / 210_000f) else 0f
                    val progress = ((songT + monoMs * 0.000035f) % 1f).coerceIn(0f, 1f)
                    drawMusicDnaSpiralScene(
                        features = liveFeatures,
                        palette = p,
                        fft = spectrum,
                        progress01 = progress,
                        phase = fastPhase,
                        playing = playing,
                        clearBackdrop = sceneClear
                    )
                }
                VisualizerMode.TOPO_CONTOUR -> drawTopoContourScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.WIRE_TERRAIN -> drawWireTerrainScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.NEON_TOPO -> drawNeonTopoScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.PSY_KALEIDO -> drawPsyKaleidoScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ACID_TUNNEL -> drawAcidTunnelScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ST01_GRAF_EQ -> drawSt01GrafEqScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ST02_LUNCHBREAK -> drawSt02LunchbreakScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ST03_CYBER_FUJI -> drawSt03CyberFujiScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_BLACK_HOLE -> drawMlBlackHoleScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_EVENT_HORIZON -> drawMlEventHorizonScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_XOR_PLASMA -> drawMlXorPlasmaScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_INFERNAL_MESH -> drawMlInfernalMeshScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_ASTRAL_COLLAPSE -> drawMlAstralCollapseScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_NEON_RITUAL -> drawMlNeonRitualScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_LIQUID_WARP -> drawMlLiquidWarpScene(
                    liveFeatures, p, waveData, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_GLITCH_CORE -> drawMlGlitchCoreScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_CHAOS_TUNNEL -> drawMlChaosTunnelScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
                VisualizerMode.ML_FRACTAL_BITE -> drawMlFractalBiteScene(
                    liveFeatures, p, spectrum, fastPhase, playing, sceneClear
                )
            }
            } // end geometry scale
        }
        val overlayScale = sizePreset.scale.coerceIn(0.45f, 1.25f)
        when (mode) {
            VisualizerMode.LYRICS_RAINBOW -> LyricsRainbowOverlay(
                features = features,
                photosensitive = photosensitive,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = overlayScale
                        scaleY = overlayScale
                    }
            )
            VisualizerMode.PARTY_MARQUEE -> PartyMarqueeOverlay(
                features = features,
                photosensitive = photosensitive,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = overlayScale
                        scaleY = overlayScale
                    }
            )
            else -> Unit
        }
        if (!resolvedClear && mode != VisualizerMode.LYRICS_RAINBOW && mode != VisualizerMode.PARTY_MARQUEE) {
            Image(
                painter = painterResource(R.drawable.apollo_crosshair_c),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp),
                colorFilter = ColorFilter.tint(p.primary.copy(alpha = 0.7f), BlendMode.SrcAtop)
            )
        }
    }
}
