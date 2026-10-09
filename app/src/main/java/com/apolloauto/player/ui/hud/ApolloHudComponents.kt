package com.apolloauto.player.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun ApolloHudPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = LocalHudPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .background(p.panel.copy(alpha = 0.88f))
            .padding(12.dp)
    ) {
        if (title != null) {
            Text(title, color = p.primary, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
fun ApolloSectionHeader(title: String, trailing: String? = null) {
    val p = LocalHudPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = p.primary, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = p.dim, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

@Composable
fun ApolloSignalBadge(label: String, active: Boolean) {
    val p = LocalHudPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (active) p.accent else p.dim.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .background(if (active) p.accent.copy(alpha = 0.15f) else p.voidBlack.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (active) p.soft else p.dim, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun ApolloStatusBar(
    themeLabel: String,
    sourceLabel: String,
    playing: Boolean,
    sleepLabel: String?
) {
    val p = LocalHudPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, p.primary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .background(p.voidBlack.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("APOLLO II", color = p.primary, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.width(10.dp))
        ApolloSignalBadge(themeLabel, true)
        Spacer(Modifier.width(6.dp))
        ApolloSignalBadge(sourceLabel, true)
        Spacer(Modifier.width(6.dp))
        ApolloSignalBadge(if (playing) "AUDIO LOCK" else "STANDBY", playing)
        if (sleepLabel != null) {
            Spacer(Modifier.width(6.dp))
            ApolloSignalBadge(sleepLabel, true)
        }
    }
}
