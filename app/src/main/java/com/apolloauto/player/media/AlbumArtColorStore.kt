package com.apolloauto.player.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.apolloauto.player.theme.HudPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Samples album artwork into a small set of vivid colors for visualizer tinting.
 * Emits null when art is missing / unreadable so the active theme palette is used.
 */
class AlbumArtColorStore(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _tint = MutableStateFlow<ArtVizTint?>(null)
    val tint: StateFlow<ArtVizTint?> = _tint.asStateFlow()

    private var job: Job? = null
    private var lastUri: Uri? = null
    /** When true, colors are stable per artwork URI; when false, reshuffle each load. */
    var colorLock: Boolean = false

    fun update(artworkUri: Uri?) {
        if (artworkUri == lastUri && _tint.value != null) return
        lastUri = artworkUri
        job?.cancel()
        if (artworkUri == null) {
            _tint.value = null
            return
        }
        job = scope.launch {
            val next = withContext(Dispatchers.IO) { extract(artworkUri, colorLock) }
            if (lastUri == artworkUri) {
                _tint.value = next
            }
        }
    }

    private fun extract(uri: Uri, lock: Boolean): ArtVizTint? {
        val bitmap = decodeSampled(uri, maxSide = 96) ?: return null
        try {
            val buckets = LinkedHashMap<Int, Int>()
            val stepX = max(1, bitmap.width / 24)
            val stepY = max(1, bitmap.height / 24)
            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val c = bitmap.getPixel(x, y)
                    val a = AndroidColor.alpha(c)
                    if (a < 200) {
                        x += stepX
                        continue
                    }
                    val r = AndroidColor.red(c)
                    val g = AndroidColor.green(c)
                    val b = AndroidColor.blue(c)
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
                    val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                    // Skip near-black / near-white / grey pixels.
                    if (sat < 0.18f || lum < 0.08f || lum > 0.92f) {
                        x += stepX
                        continue
                    }
                    val key = quantize(r, g, b)
                    buckets[key] = (buckets[key] ?: 0) + 1
                    x += stepX
                }
                y += stepY
            }
            if (buckets.isEmpty()) return null
            val ranked = buckets.entries
                .sortedByDescending { it.value }
                .map { unpack(it.key) }
                .distinctBy { (it.first shr 3) xor (it.second shr 3) xor (it.third shr 3) }
                .take(8)
            if (ranked.isEmpty()) return null
            val rng = if (lock) Random(uri.toString().hashCode()) else Random.Default
            val picked = ranked.shuffled(rng)
            fun toColor(rgb: Triple<Int, Int, Int>, boost: Float = 1.1f): Color {
                val r = (rgb.first * boost).toInt().coerceIn(40, 255)
                val g = (rgb.second * boost).toInt().coerceIn(40, 255)
                val b = (rgb.third * boost).toInt().coerceIn(40, 255)
                return Color(r, g, b)
            }
            val primary = toColor(picked[0], 1.15f)
            val secondary = toColor(picked.getOrElse(1) { picked[0] }, 1.05f)
            val accent = toColor(picked.getOrElse(2) { picked[0] }, 1.2f)
            val soft = blendTowardWhite(primary, 0.55f)
            return ArtVizTint(primary, secondary, accent, soft)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun decodeSampled(uri: Uri, maxSide: Int): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                while (longest / sample > maxSide * 2) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
        }.getOrNull()
    }

    private fun quantize(r: Int, g: Int, b: Int): Int {
        val rq = (r / 24) * 24
        val gq = (g / 24) * 24
        val bq = (b / 24) * 24
        return (rq shl 16) or (gq shl 8) or bq
    }

    private fun unpack(key: Int): Triple<Int, Int, Int> =
        Triple((key shr 16) and 0xFF, (key shr 8) and 0xFF, key and 0xFF)

    private fun blendTowardWhite(c: Color, amount: Float): Color {
        val a = amount.coerceIn(0f, 1f)
        return Color(
            red = c.red + (1f - c.red) * a,
            green = c.green + (1f - c.green) * a,
            blue = c.blue + (1f - c.blue) * a,
            alpha = 1f
        )
    }
}

data class ArtVizTint(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val soft: Color
) {
    fun applyTo(base: HudPalette): HudPalette = base.copy(
        primary = primary,
        secondary = secondary,
        accent = accent,
        soft = soft,
        dim = soft.copy(alpha = 0.65f),
        glowA = primary,
        glowB = accent,
        grid = primary.copy(alpha = 0.2f)
    )
}
