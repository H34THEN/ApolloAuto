package com.apolloauto.player.ui

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.apolloauto.player.media.EffectiveBackground

/**
 * Unified background layer under all visualizer scenes.
 * Does not own scene state — artwork updates must not recreate the scene.
 */
@Composable
fun VisualizerBackgroundLayer(
    effective: EffectiveBackground,
    artworkUri: Uri?,
    albumBlur: Float,
    albumDarkness: Float,
    albumSaturation: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (effective.showAlbumArt && artworkUri != null) {
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val blurDp = (albumBlur.coerceIn(0f, 1f) * 40f).dp
            val sat = albumSaturation.coerceIn(0f, 1.5f)
            val matrix = ColorMatrix().apply {
                setToSaturation(sat)
            }
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(matrix),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canBlur && albumBlur > 0.05f) Modifier.blur(blurDp) else Modifier)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = albumDarkness.coerceIn(0.2f, 0.92f)))
            )
        }
        // Pure black path: the outer Box is already #000000.
    }
}
