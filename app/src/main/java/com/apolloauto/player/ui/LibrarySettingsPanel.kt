package com.apolloauto.player.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.MusicSource
import com.apolloauto.player.data.MusicSourceKind
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.ApolloHudPanel
import com.apolloauto.player.ui.hud.ApolloSciFiPanel
import com.apolloauto.player.ui.hud.useKenneyChrome
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun LibrarySettingsPanel() {
    val app = ApolloApp.instance
    val context = LocalContext.current
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val scope = rememberCoroutineScope()
    val sources by app.musicSourceStore.sources.collectAsState()
    val errors by app.musicSourceStore.scanErrors.collectAsState()
    val kenney = useKenneyChrome(app.themeStore.interfaceStyle.value)
    val Panel: @Composable (String, @Composable () -> Unit) -> Unit = { title, content ->
        if (kenney) ApolloSciFiPanel(title = title) { content() }
        else ApolloHudPanel(title = title) { content() }
    }

    val openPhoneFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        scope.launch {
            app.libraryScanCoordinator.addPhoneFolder(uri, uri.lastPathSegment)
        }
    }

    val openAddSource = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        scope.launch {
            app.libraryScanCoordinator.addUsbOrExternal(uri, uri.lastPathSegment)
        }
    }

    Panel("APOLLO // STORAGE MATRIX") {
        Text(
            "Configure phone folders, USB SSD, and SD card roots. Removing a source never deletes music files.",
            color = p.dim,
            fontFamily = body,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceActionChip("CHANGE PHONE FOLDER", Modifier.weight(1f)) { openPhoneFolder.launch(null) }
            SourceActionChip("+ ADD SOURCE", Modifier.weight(1f)) { openAddSource.launch(null) }
        }
        Spacer(Modifier.height(8.dp))
        SourceActionChip("RESCAN ALL", Modifier.fillMaxWidth()) {
            scope.launch { app.libraryScanCoordinator.scanAllEnabled() }
        }
    }

    sources.forEach { source ->
        Panel(source.kindLabel) {
            SourceCard(
                source = source,
                error = errors[source.id] ?: source.lastError,
                onRescan = {
                    scope.launch { app.libraryScanCoordinator.scanOne(source) }
                },
                onToggle = {
                    val enable = !source.enabled
                    app.musicSourceStore.setEnabled(source.id, enable)
                    if (enable) {
                        scope.launch { app.libraryScanCoordinator.scanOne(source.copy(enabled = true)) }
                    }
                },
                onPreferred = { app.musicSourceStore.setPreferred(source.id) },
                onRemove = {
                    if (source.id != com.apolloauto.player.data.MusicSourceStore.PHONE_MEDIASTORE_ID) {
                        app.musicSourceStore.removeSource(source.id)
                    } else {
                        app.musicSourceStore.setEnabled(source.id, false)
                    }
                }
            )
        }
    }
}

@Composable
private fun SourceCard(
    source: MusicSource,
    error: String?,
    onRescan: () -> Unit,
    onToggle: () -> Unit,
    onPreferred: () -> Unit,
    onRemove: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val last = if (source.lastScanMs > 0) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(source.lastScanMs))
    } else "Never"
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(source.label, color = p.soft, fontFamily = body, fontSize = 16.sp)
                Text(
                    "${source.trackCount} tracks · ${source.playlistCount} playlists",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 12.sp
                )
            }
            Text(
                when {
                    !source.enabled -> "OFF"
                    source.connected -> "ONLINE"
                    else -> "OFFLINE"
                },
                color = when {
                    !source.enabled -> p.dim
                    source.connected -> p.accent
                    else -> p.danger
                },
                fontFamily = body,
                fontSize = 11.sp
            )
        }
        Text("Last scan: $last", color = p.dim, fontFamily = body, fontSize = 11.sp)
        if (source.preferred) {
            Text("PREFERRED SOURCE", color = p.secondary, fontFamily = body, fontSize = 10.sp)
        }
        if (!error.isNullOrBlank()) {
            Text("Error: $error", color = p.danger, fontFamily = body, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SourceActionChip(if (source.enabled) "DISABLE" else "ENABLE") { onToggle() }
            SourceActionChip("RESCAN") { onRescan() }
            if (!source.preferred) SourceActionChip("DEFAULT") { onPreferred() }
            if (source.kind != MusicSourceKind.PHONE_MEDIASTORE) {
                SourceActionChip("REMOVE") { onRemove() }
            }
        }
        Text(
            "Remove drops Apollo’s index only — files stay on storage.",
            color = p.dim,
            fontFamily = body,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SourceActionChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = p.voidBlack,
        fontFamily = body,
        fontSize = 11.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(p.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}
