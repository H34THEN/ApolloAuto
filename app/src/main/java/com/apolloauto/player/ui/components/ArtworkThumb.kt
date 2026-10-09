package com.apolloauto.player.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun ArtworkThumb(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = ApolloSpace.thumbArt,
    contentDescription: String? = "Album artwork"
) {
    val p = LocalHudPalette.current
    val shape = RoundedCornerShape(ApolloSpace.cornerTight)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, p.primary.copy(alpha = 0.45f), shape)
            .background(p.voidBlack),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ApolloArtPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ApolloArtPlaceholder(modifier: Modifier = Modifier) {
    val p = LocalHudPalette.current
    val hud = LocalHudFont.current
    val body = LocalBodyFont.current
    Box(modifier = modifier.background(p.panel), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val step = size.minDimension / 6f
            for (i in 0..6) {
                drawLine(
                    color = p.primary.copy(alpha = 0.08f),
                    start = Offset(i * step, 0f),
                    end = Offset(i * step, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = p.primary.copy(alpha = 0.06f),
                    start = Offset(0f, i * step),
                    end = Offset(size.width, i * step),
                    strokeWidth = 1f
                )
            }
            drawRect(
                brush = Brush.radialGradient(
                    listOf(p.primary.copy(alpha = 0.2f), p.voidBlack.copy(alpha = 0.1f))
                )
            )
        }
        Text(
            "AII",
            color = p.primary.copy(alpha = 0.85f),
            fontFamily = hud,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "NO ART",
            color = p.dim,
            fontFamily = body,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.BottomCenter),
            letterSpacing = 1.sp
        )
    }
}
