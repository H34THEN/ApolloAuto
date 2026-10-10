package com.apolloauto.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.ApolloTypeScale
import com.apolloauto.player.theme.InterfaceStyle
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.useKenneyChrome

enum class HomeDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryMusic),
    PLAYLISTS("Lists", Icons.Default.PlaylistPlay),
    CAR("Car", Icons.Default.DirectionsCar),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun CompactApolloHeader(
    themeLabel: String,
    sourceLabel: String,
    playing: Boolean,
    sleepLabel: String?,
    onSettings: () -> Unit,
    companionLine: String? = null,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val hud = LocalHudFont.current
    val body = LocalBodyFont.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ApolloSpace.headerHeight)
            .padding(vertical = ApolloSpace.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "APOLLO II",
                color = p.primary,
                fontFamily = hud,
                fontSize = ApolloTypeScale.logoSp.sp,
                letterSpacing = 1.5.sp,
                maxLines = 1
            )
            Text(
                companionLine?.takeIf { it.isNotBlank() }
                    ?: "AUDIO COMMAND // ${themeLabel.uppercase()}",
                color = p.secondary,
                fontFamily = body,
                fontSize = ApolloTypeScale.techSp.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusPill(sourceLabel, active = true)
            if (sleepLabel != null) StatusPill(sleepLabel, active = true)
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (playing) p.accent else p.dim.copy(alpha = 0.5f))
            )
            IconButton(onClick = onSettings, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.Settings, "Settings", tint = p.primary)
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label.uppercase(),
        color = if (active) p.soft else p.dim,
        fontFamily = body,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (active) p.accent.copy(alpha = 0.7f) else p.dim.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .background(if (active) p.accent.copy(alpha = 0.12f) else p.voidBlack.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1
    )
}

@Composable
fun ApolloBottomNav(
    current: HomeDestination,
    onSelect: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ApolloSpace.bottomNavHeight)
            .clip(RoundedCornerShape(topStart = ApolloSpace.corner, topEnd = ApolloSpace.corner))
            .border(1.dp, p.primary.copy(alpha = 0.35f), RoundedCornerShape(topStart = ApolloSpace.corner, topEnd = ApolloSpace.corner))
            .background(p.panel.copy(alpha = 0.95f))
            .padding(horizontal = ApolloSpace.space4),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeDestination.entries.forEach { dest ->
            val selected = dest == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(ApolloSpace.touchMin)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) p.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(dest) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    dest.icon,
                    contentDescription = dest.label,
                    tint = if (selected) p.accent else p.dim,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    dest.label,
                    color = if (selected) p.soft else p.dim,
                    fontFamily = body,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LibraryFilterChips(
    selected: LibraryCategory,
    onSelect: (LibraryCategory) -> Unit,
    interfaceStyle: InterfaceStyle = InterfaceStyle.CYBERPUNK_HUD,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val kenney = useKenneyChrome(interfaceStyle)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ApolloSpace.space8)
    ) {
        LibraryCategory.entries.forEach { cat ->
            val on = selected == cat
            Text(
                cat.label,
                color = if (on) p.soft else p.dim,
                fontFamily = body,
                fontSize = ApolloTypeScale.metaSp.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(if (kenney) 2.dp else ApolloSpace.cornerTight))
                    .border(
                        1.dp,
                        if (on) p.accent else p.primary.copy(alpha = if (kenney) 0.4f else 0.25f),
                        RoundedCornerShape(if (kenney) 2.dp else ApolloSpace.cornerTight)
                    )
                    .background(
                        if (on) p.accent.copy(alpha = if (kenney) 0.22f else 0.14f)
                        else p.voidBlack.copy(alpha = 0.25f)
                    )
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

enum class LibraryCategory(val label: String) {
    TRACKS("Tracks"),
    CAR("Car ★"),
    PLAYLISTS("Lists"),
    NEW("New"),
    UNPLAYED("Unplayed"),
    RECENT("Recent"),
    TOP("Top"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}
