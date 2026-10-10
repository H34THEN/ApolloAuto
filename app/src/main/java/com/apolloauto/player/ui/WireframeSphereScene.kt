package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Wireframe audio-reactive sphere inspired by
 * [nixonyh/UnityAudioVisualizer](https://github.com/nixonyh/UnityAudioVisualizer)
 * (Apache-2.0). Reimplemented for Compose Canvas — not a Unity/VFX Graph port.
 */
fun DrawScope.drawWireframeSphereScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    pulse: Float,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    val mesh = IcoMesh.INSTANCE
    val cx = size.width / 2f
    val cy = size.height * 0.48f
    val radius = size.minDimension * (0.28f + features.bass * 0.04f) * pulse.coerceIn(0.92f, 1.12f)
    val yaw = phase * 0.0045f
    val pitch = 0.35f + sin(phase * 0.002f) * 0.08f
    val energy = (features.rms * 0.5f + features.bass * 0.35f + features.onset * 0.4f).coerceIn(0f, 1.3f)
    val motion = if (playing) 1f else 0.45f

    if (!clearBackdrop) drawRect(palette.voidBlack)
    // Soft bloom core
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.primary.copy(alpha = 0.14f + energy * 0.12f),
                palette.secondary.copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 2.2f
        ),
        radius = radius * 2.2f,
        center = Offset(cx, cy)
    )

    val faceCount = mesh.faces.size
    val projected = Array(mesh.vertices.size) { Offset.Zero }
    val depths = FloatArray(mesh.vertices.size)
    val displaced = Array(mesh.vertices.size) { mesh.vertices[it] }

    // Displace along normals per face (Unity-style band → triangle push), then average.
    val accum = Array(mesh.vertices.size) { Vec3.ZERO }
    val counts = IntArray(mesh.vertices.size)
    for (f in 0 until faceCount) {
        val face = mesh.faces[f]
        val band = fftBand(fft, f, faceCount) * motion
        val push = (band * band * 0.55f + features.onset * 0.12f) * (0.85f + features.bass * 0.4f)
        val i0 = face[0]
        val i1 = face[1]
        val i2 = face[2]
        val n = mesh.faceNormals[f]
        val d = n * push
        accum[i0] = accum[i0] + d
        accum[i1] = accum[i1] + d
        accum[i2] = accum[i2] + d
        counts[i0]++
        counts[i1]++
        counts[i2]++
    }
    for (i in displaced.indices) {
        val avg = if (counts[i] > 0) accum[i] * (1f / counts[i]) else Vec3.ZERO
        val base = mesh.vertices[i]
        displaced[i] = (base + avg).normalized()
    }

    for (i in displaced.indices) {
        val r = rotate(displaced[i], yaw, pitch)
        depths[i] = r.z
        val persp = 1.8f / (2.4f - r.z)
        projected[i] = Offset(cx + r.x * radius * persp, cy - r.y * radius * persp)
    }

    // Draw faces back-to-front for soft fills, then all edges.
    val order = (0 until faceCount).sortedBy { f ->
        val face = mesh.faces[f]
        depths[face[0]] + depths[face[1]] + depths[face[2]]
    }
    for (f in order) {
        val face = mesh.faces[f]
        val zAvg = (depths[face[0]] + depths[face[1]] + depths[face[2]]) / 3f
        if (zAvg < -0.15f) continue // mostly backface cull soft fill
        val band = fftBand(fft, f, faceCount)
        val a = projected[face[0]]
        val b = projected[face[1]]
        val c = projected[face[2]]
        val fill = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            close()
        }
        val tint = if (f % 2 == 0) palette.primary else palette.secondary
        drawPath(
            fill,
            color = tint.copy(alpha = (0.03f + band * 0.12f + energy * 0.04f).coerceIn(0.02f, 0.22f))
        )
    }

    for (f in 0 until faceCount) {
        val face = mesh.faces[f]
        val zAvg = (depths[face[0]] + depths[face[1]] + depths[face[2]]) / 3f
        val band = fftBand(fft, f, faceCount)
        val depthFade = ((zAvg + 1f) * 0.5f).coerceIn(0.2f, 1f)
        val lineColor = when {
            band > 0.55f -> palette.accent
            f % 3 == 0 -> palette.secondary
            else -> palette.primary
        }
        val alpha = (0.25f + band * 0.55f) * depthFade
        val width = 1.1f + band * 2.4f + energy * 0.8f
        val pts = arrayOf(
            projected[face[0]],
            projected[face[1]],
            projected[face[2]],
            projected[face[0]]
        )
        // Glow pass
        for (e in 0 until 3) {
            drawLine(
                color = lineColor.copy(alpha = alpha * 0.35f),
                start = pts[e],
                end = pts[e + 1],
                strokeWidth = width + 3.5f,
                cap = StrokeCap.Round
            )
        }
        for (e in 0 until 3) {
            drawLine(
                color = lineColor.copy(alpha = alpha.coerceIn(0.15f, 0.95f)),
                start = pts[e],
                end = pts[e + 1],
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }

    // Radial spectrum spikes around the equator (demo screenshot look).
    val bars = minOf(48, max(16, fft.size))
    for (i in 0 until bars) {
        val ang = i / bars.toFloat() * PI.toFloat() * 2f + yaw * 0.5f
        val band = fftBand(fft, i, bars)
        val inner = radius * 1.05f
        val outer = inner + radius * (0.08f + band * 0.55f + features.bass * 0.12f) * motion
        val ix = cx + cos(ang) * inner
        val iy = cy + sin(ang) * inner * 0.55f
        val ox = cx + cos(ang) * outer
        val oy = cy + sin(ang) * outer * 0.55f
        val col = if (i % 2 == 0) palette.primary else palette.secondary
        drawLine(
            color = col.copy(alpha = 0.2f + band * 0.65f),
            start = Offset(ix, iy),
            end = Offset(ox, oy),
            strokeWidth = 2f + band * 3f,
            cap = StrokeCap.Round
        )
    }

    // Fragment particles
    val particles = 36
    for (i in 0 until particles) {
        val ang = i * 0.7f + phase * 0.01f + features.mid
        val dist = radius * (1.15f + (i % 5) * 0.12f + features.onset * 0.35f + features.high * 0.25f)
        val px = cx + cos(ang) * dist
        val py = cy + sin(ang) * dist * 0.62f
        val s = 2f + features.high * 4f + (i % 3)
        drawCircle(
            color = (if (i % 2 == 0) palette.accent else palette.primary)
                .copy(alpha = 0.15f + features.rms * 0.35f),
            radius = s,
            center = Offset(px, py)
        )
    }

    // Inner energy ring
    drawCircle(
        color = palette.accent.copy(alpha = 0.12f + features.onset * 0.35f),
        radius = radius * (0.35f + features.bass * 0.15f),
        center = Offset(cx, cy),
        style = Stroke(width = 2f + energy * 3f)
    )
}

private fun fftBand(fft: FloatArray, index: Int, count: Int): Float {
    if (fft.isEmpty()) return 0f
    val start = (index * fft.size / count).coerceIn(0, fft.lastIndex)
    val end = (((index + 1) * fft.size / count)).coerceIn(start + 1, fft.size)
    var sum = 0f
    for (i in start until end) sum += fft[i]
    return (sum / (end - start)).coerceIn(0f, 1.2f)
}

private fun rotate(v: Vec3, yaw: Float, pitch: Float): Vec3 {
    val cy = cos(yaw)
    val sy = sin(yaw)
    val x1 = v.x * cy + v.z * sy
    val z1 = -v.x * sy + v.z * cy
    val cp = cos(pitch)
    val sp = sin(pitch)
    val y2 = v.y * cp - z1 * sp
    val z2 = v.y * sp + z1 * cp
    return Vec3(x1, y2, z2)
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length() = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-6f)
    fun normalized(): Vec3 {
        val l = length()
        return Vec3(x / l, y / l, z / l)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

private class IcoMesh(
    val vertices: List<Vec3>,
    val faces: List<IntArray>,
    val faceNormals: List<Vec3>
) {
    companion object {
        val INSTANCE: IcoMesh by lazy { build(subdivisions = 1) }

        private fun build(subdivisions: Int): IcoMesh {
            val t = (1f + sqrt(5f)) / 2f
            var verts = mutableListOf(
                Vec3(-1f, t, 0f), Vec3(1f, t, 0f), Vec3(-1f, -t, 0f), Vec3(1f, -t, 0f),
                Vec3(0f, -1f, t), Vec3(0f, 1f, t), Vec3(0f, -1f, -t), Vec3(0f, 1f, -t),
                Vec3(t, 0f, -1f), Vec3(t, 0f, 1f), Vec3(-t, 0f, -1f), Vec3(-t, 0f, 1f)
            ).map { it.normalized() }.toMutableList()
            var faces = mutableListOf(
                intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
                intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
                intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
                intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
            )
            repeat(subdivisions) {
                val midCache = HashMap<Long, Int>()
                fun midpoint(a: Int, b: Int): Int {
                    val key = if (a < b) a.toLong() shl 32 or b.toLong() else b.toLong() shl 32 or a.toLong()
                    midCache[key]?.let { return it }
                    val mid = (verts[a] + verts[b]).normalized()
                    val idx = verts.size
                    verts.add(mid)
                    midCache[key] = idx
                    return idx
                }
                val next = ArrayList<IntArray>(faces.size * 4)
                for (f in faces) {
                    val a = midpoint(f[0], f[1])
                    val b = midpoint(f[1], f[2])
                    val c = midpoint(f[2], f[0])
                    next += intArrayOf(f[0], a, c)
                    next += intArrayOf(f[1], b, a)
                    next += intArrayOf(f[2], c, b)
                    next += intArrayOf(a, b, c)
                }
                faces = next
            }
            val normals = faces.map { f ->
                val v0 = verts[f[0]]
                val v1 = verts[f[1]]
                val v2 = verts[f[2]]
                val u = Vec3(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z)
                val w = Vec3(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z)
                Vec3(
                    u.y * w.z - u.z * w.y,
                    u.z * w.x - u.x * w.z,
                    u.x * w.y - u.y * w.x
                ).normalized()
            }
            return IcoMesh(verts, faces, normals)
        }
    }
}
