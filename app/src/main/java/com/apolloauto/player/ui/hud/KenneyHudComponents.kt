package com.apolloauto.player.ui.hud

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.R
import com.apolloauto.player.theme.InterfaceStyle
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun ApolloSciFiPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = LocalHudPalette.current
    val font = LocalHudFont.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .background(p.panel.copy(alpha = 0.9f))
            .padding(12.dp)
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                color = p.primary,
                fontFamily = font,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
fun ApolloHudButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val p = LocalHudPalette.current
    val font = LocalHudFont.current
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.apollo_btn_blade),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(
                if (enabled) p.primary else p.dim,
                BlendMode.SrcAtop
            ),
            alpha = if (enabled) 0.95f else 0.45f
        )
        Text(
            label.uppercase(),
            color = if (enabled) p.soft else p.dim,
            fontFamily = font,
            fontSize = fontSize,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun ApolloHudIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val p = LocalHudPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.apollo_btn_notch),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(p.primary, BlendMode.SrcAtop),
            alpha = if (enabled) 0.95f else 0.4f
        )
        content()
    }
}

@Composable
fun ApolloHudProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp
) {
    val p = LocalHudPalette.current
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Segmented track
        Row(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.apollo_bar_round_l),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight(),
                colorFilter = ColorFilter.tint(p.dim, BlendMode.SrcAtop)
            )
            Image(
                painter = painterResource(R.drawable.apollo_bar_round_m),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colorFilter = ColorFilter.tint(p.dim, BlendMode.SrcAtop)
            )
            Image(
                painter = painterResource(R.drawable.apollo_bar_round_r),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight(),
                colorFilter = ColorFilter.tint(p.dim, BlendMode.SrcAtop)
            )
        }
        // Fill overlay
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(p.secondary.copy(alpha = 0.55f))
        )
    }
}

@Composable
fun ApolloArtworkFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val p = LocalHudPalette.current
    Box(
        modifier = modifier
            .border(1.dp, p.primary.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            .background(p.voidBlack)
            .padding(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
        ) {
            content()
        }
        Image(
            painter = painterResource(R.drawable.apollo_crosshair_a),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(18.dp)
                .padding(2.dp),
            colorFilter = ColorFilter.tint(p.accent.copy(alpha = 0.85f), BlendMode.SrcAtop)
        )
        Image(
            painter = painterResource(R.drawable.apollo_crosshair_a),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .padding(2.dp),
            colorFilter = ColorFilter.tint(p.secondary.copy(alpha = 0.85f), BlendMode.SrcAtop)
        )
    }
}

@Composable
fun ApolloHudLabel(text: String, modifier: Modifier = Modifier, accent: Boolean = true) {
    val p = LocalHudPalette.current
    val font = LocalHudFont.current
    Text(
        text.uppercase(),
        color = if (accent) p.primary else p.dim,
        fontFamily = font,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}

@Composable
fun ApolloHudTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val font = LocalHudFont.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.apollo_btn_small),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(
                if (selected) p.accent else p.primary.copy(alpha = 0.55f),
                BlendMode.SrcAtop
            ),
            alpha = if (selected) 1f else 0.7f
        )
        Text(
            label.uppercase(),
            color = p.soft,
            fontFamily = font,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun useKenneyChrome(style: InterfaceStyle): Boolean = style == InterfaceStyle.CYBERPUNK_HUD
