package com.apolloauto.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.LibrarySource
import com.apolloauto.player.data.Playlist
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.Track
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.media.RepeatModeUi
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.ApolloHudButton
import com.apolloauto.player.ui.hud.ApolloHudIconButton
import com.apolloauto.player.ui.hud.ApolloHudProgress
import com.apolloauto.player.ui.hud.ApolloHudTab
import com.apolloauto.player.ui.hud.ApolloSciFiPanel
import com.apolloauto.player.ui.hud.ApolloStatusBar
import com.apolloauto.player.ui.hud.useKenneyChrome
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class LibraryTab { ALL, CAR_FAVORITES, PLAYLISTS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloCyberTheme {
                ApolloHomeScreen()
            }
        }
    }
}

@Composable
private fun ApolloHomeScreen() {
    val context = LocalContext.current
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val scope = rememberCoroutineScope()
    val tracks by app.libraryStore.tracks.collectAsState()
    val query by app.libraryStore.query.collectAsState()
    val source by app.libraryStore.source.collectAsState()
    val scanning by app.libraryStore.scanning.collectAsState()
    val status by app.libraryStore.status.collectAsState()
    val treeUri by app.libraryStore.treeUri.collectAsState()
    val playlists by app.playlistStore.playlists.collectAsState()
    val themeId by app.themeStore.themeId.collectAsState()
    val interfaceStyle by app.themeStore.interfaceStyle.collectAsState()
    val sleep by app.sleepTimerController.state.collectAsState()
    val controller = remember { PlayerController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    val kenney = useKenneyChrome(interfaceStyle)
    val hudFont = LocalHudFont.current
    val bodyFont = LocalBodyFont.current

    var tab by remember { mutableStateOf(LibraryTab.ALL) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var addTrackId by remember { mutableStateOf<String?>(null) }
    var addTrackTitle by remember { mutableStateOf("") }

    val filtered = remember(tracks, query) { app.libraryStore.filteredTracks() }
    val carTracks = remember(tracks, playlists, query) {
        filterList(app.playlistStore.resolveById(PlaylistStore.CAR_FAVORITES_ID, tracks), query)
    }
    val selectedPlaylistTracks = remember(tracks, playlists, selectedPlaylistId, query) {
        val id = selectedPlaylistId ?: return@remember emptyList()
        filterList(app.playlistStore.resolveById(id, tracks), query)
    }

    DisposableEffect(Unit) {
        app.playlistStore.ensureCarFavorites()
        controller.connect()
        onDispose { controller.release() }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            controller.pollPosition()
            delay(400)
        }
    }

    fun scanPhone() {
        scope.launch { app.deviceScanner.scanPhoneLibrary() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanPhone()
        else app.libraryStore.setStatus("Audio permission denied — enable in Settings")
    }

    fun requestPhoneScan() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val granted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) scanPhone() else permissionLauncher.launch(permission)
    }

    LaunchedEffect(source, treeUri) {
        when (source) {
            LibrarySource.PHONE -> requestPhoneScan()
            LibrarySource.USB -> {
                val uri = treeUri
                if (uri != null) app.usbScanner.scanTree(uri)
                else {
                    app.libraryStore.setTracks(emptyList())
                    app.libraryStore.setStatus("USB not linked yet — use phone library for now")
                }
            }
        }
    }

    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            app.libraryStore.setTreeUri(uri)
            app.libraryStore.setSource(LibrarySource.USB)
        }
    }

    CyberBackground {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "APOLLO II",
                            color = p.primary,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp,
                            fontFamily = hudFont
                        )
                        Text(
                            "AUDIO COMMAND SYSTEM",
                            color = p.secondary,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontFamily = hudFont
                        )
                    }
                    if (kenney) {
                        ApolloHudIconButton(onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = p.soft)
                        }
                    } else {
                        IconButton(onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = p.primary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ApolloStatusBar(
                    themeLabel = themeId.label.take(12).uppercase(),
                    sourceLabel = source.name,
                    playing = playback.playing,
                    sleepLabel = if (sleep.running) "SLEEP ${sleep.remainingMs / 60000}m" else null
                )
                Spacer(Modifier.height(6.dp))
                Text(status, color = p.dim, fontSize = 13.sp, fontFamily = bodyFont)

                Spacer(Modifier.height(12.dp))
                if (kenney) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApolloHudTab(
                            "PHONE",
                            source == LibrarySource.PHONE,
                            onClick = { app.libraryStore.setSource(LibrarySource.PHONE) },
                            modifier = Modifier.weight(1f)
                        )
                        ApolloHudTab(
                            "USB SSD",
                            source == LibrarySource.USB,
                            onClick = { app.libraryStore.setSource(LibrarySource.USB) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ApolloHudButton(
                            label = if (source == LibrarySource.PHONE) "SCAN PHONE" else "LINK USB",
                            onClick = {
                                if (source == LibrarySource.PHONE) requestPhoneScan()
                                else openTree.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ApolloHudButton(
                            label = "CAR MODE",
                            onClick = {
                                context.startActivity(Intent(context, CarModeActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SourceChip("PHONE", source == LibrarySource.PHONE, Modifier.weight(1f)) {
                            app.libraryStore.setSource(LibrarySource.PHONE)
                        }
                        SourceChip("USB SSD", source == LibrarySource.USB, Modifier.weight(1f)) {
                            app.libraryStore.setSource(LibrarySource.USB)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton(
                            label = if (source == LibrarySource.PHONE) "SCAN PHONE" else "LINK USB SSD",
                            accent = p.primary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (source == LibrarySource.PHONE) requestPhoneScan()
                                else openTree.launch(null)
                            }
                        ) {
                            Icon(
                                if (source == LibrarySource.PHONE) Icons.Default.PhoneAndroid else Icons.Default.Usb,
                                null,
                                tint = p.soft
                            )
                        }
                        NeonButton(
                            label = "CAR MODE",
                            accent = p.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                context.startActivity(Intent(context, CarModeActivity::class.java))
                            }
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, tint = p.soft)
                        }
                    }
                }

                if (scanning) {
                    Spacer(Modifier.height(10.dp))
                    if (kenney) {
                        ApolloHudProgress(0.45f, height = 14.dp)
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = p.accent,
                            trackColor = p.panel
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                SearchField(query) { app.libraryStore.setQuery(it) }

                Spacer(Modifier.height(12.dp))
                NowPlayingBar(
                    kenney = kenney,
                    playbackTitle = playback.title,
                    playbackArtist = playback.artist,
                    artworkUri = playback.artworkUri,
                    playing = playback.playing,
                    shuffle = playback.shuffle,
                    repeatMode = playback.repeatMode,
                    inCarFavorites = playback.mediaId.isNotBlank() &&
                        app.playlistStore.isInCarFavorites(playback.mediaId),
                    progress = progressOf(playback.positionMs, playback.durationMs),
                    onOpenPlayer = {
                        context.startActivity(Intent(context, NowPlayingActivity::class.java))
                    },
                    onPrev = controller::skipPrevious,
                    onToggle = controller::togglePlayPause,
                    onNext = controller::skipNext,
                    onShuffle = controller::toggleShuffle,
                    onRepeat = controller::cycleRepeat,
                    onToggleCarFavorite = {
                        if (playback.mediaId.isNotBlank()) {
                            app.playlistStore.toggleCarFavorite(playback.mediaId)
                        }
                    },
                    onAddToPlaylist = {
                        if (playback.mediaId.isNotBlank()) {
                            addTrackId = playback.mediaId
                            addTrackTitle = playback.title
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (kenney) {
                        ApolloHudTab("ALL", tab == LibraryTab.ALL, onClick = {
                            tab = LibraryTab.ALL
                            selectedPlaylistId = null
                        }, modifier = Modifier.weight(1f))
                        ApolloHudTab("CAR", tab == LibraryTab.CAR_FAVORITES, onClick = {
                            tab = LibraryTab.CAR_FAVORITES
                            selectedPlaylistId = null
                        }, modifier = Modifier.weight(1f))
                        ApolloHudTab("LISTS", tab == LibraryTab.PLAYLISTS, onClick = {
                            tab = LibraryTab.PLAYLISTS
                        }, modifier = Modifier.weight(1f))
                    } else {
                        TabChip("ALL", tab == LibraryTab.ALL, Modifier.weight(1f)) {
                            tab = LibraryTab.ALL
                            selectedPlaylistId = null
                        }
                        TabChip("CAR ★", tab == LibraryTab.CAR_FAVORITES, Modifier.weight(1f)) {
                            tab = LibraryTab.CAR_FAVORITES
                            selectedPlaylistId = null
                        }
                        TabChip("LISTS", tab == LibraryTab.PLAYLISTS, Modifier.weight(1f)) {
                            tab = LibraryTab.PLAYLISTS
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                when (tab) {
                    LibraryTab.ALL -> TrackList(
                        label = "LIBRARY · ${filtered.size}",
                        tracks = filtered,
                        playlistStore = app.playlistStore,
                        onPlay = { list, index -> controller.playTracks(list, index) },
                        onAdd = { track ->
                            addTrackId = track.id
                            addTrackTitle = track.title
                        },
                        onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                    )

                    LibraryTab.CAR_FAVORITES -> TrackList(
                        label = "CAR FAVORITES · ${carTracks.size}",
                        tracks = carTracks,
                        playlistStore = app.playlistStore,
                        emptyText = "Tap the car icon on any song to save it here for quick Autoplays.",
                        onPlay = { list, index -> controller.playTracks(list, index) },
                        onAdd = { track ->
                            addTrackId = track.id
                            addTrackTitle = track.title
                        },
                        onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                    )

                    LibraryTab.PLAYLISTS -> {
                        if (selectedPlaylistId == null) {
                            PlaylistBrowser(
                                playlists = playlists,
                                onOpen = { selectedPlaylistId = it },
                                onCreate = {
                                    val created = app.playlistStore.create("Deck ${playlists.size}")
                                    selectedPlaylistId = created.id
                                },
                                onDelete = { id -> app.playlistStore.delete(id) }
                            )
                        } else {
                            val pl = playlists.find { it.id == selectedPlaylistId }
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "← ${pl?.name ?: "Playlist"}",
                                        color = p.primary,
                                        modifier = Modifier
                                            .clickable { selectedPlaylistId = null }
                                            .padding(vertical = 6.dp)
                                    )
                                    Text(
                                        "PLAY ALL",
                                        color = p.voidBlack,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(p.accent)
                                            .clickable {
                                                controller.playTracks(selectedPlaylistTracks, 0)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 12.sp
                                    )
                                }
                                TrackList(
                                    label = "${selectedPlaylistTracks.size} TRACKS",
                                    tracks = selectedPlaylistTracks,
                                    playlistStore = app.playlistStore,
                                    emptyText = "Open ALL, tap + on a song, and add it to this playlist.",
                                    onPlay = { list, index -> controller.playTracks(list, index) },
                                    onAdd = { track ->
                                        addTrackId = track.id
                                        addTrackTitle = track.title
                                    },
                                    onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                                )
                            }
                        }
                    }
                }
            }

            val sheetTrackId = addTrackId
            if (sheetTrackId != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(p.voidBlack.copy(alpha = 0.55f))
                        .clickable { addTrackId = null },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(Modifier.clickable(enabled = false) {}) {
                        AddToPlaylistSheet(
                            trackId = sheetTrackId,
                            trackTitle = addTrackTitle,
                            playlists = playlists,
                            playlistStore = app.playlistStore,
                            onDismiss = { addTrackId = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    label: String,
    tracks: List<Track>,
    playlistStore: PlaylistStore,
    emptyText: String = "No tracks yet.",
    onPlay: (List<Track>, Int) -> Unit,
    onAdd: (Track) -> Unit,
    onToggleCar: (Track) -> Unit
) {
    val p = LocalHudPalette.current
    Column(Modifier.fillMaxSize()) {
        Text(label, color = p.primary, letterSpacing = 2.sp, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (tracks.isEmpty()) {
            Text(emptyText, color = p.dim, fontSize = 13.sp)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    val inCar = playlistStore.isInCarFavorites(track.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, p.primary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .background(p.panel.copy(alpha = 0.65f))
                            .clickable { onPlay(tracks, index) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(p.voidBlack)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = p.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${track.artist} · ${track.album}",
                                color = p.dim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { onToggleCar(track) }) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = "Car Favorites",
                                tint = if (inCar) p.secondary else p.dim
                            )
                        }
                        IconButton(onClick = { onAdd(track) }) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = p.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistBrowser(
    playlists: List<Playlist>,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onDelete: (String) -> Unit
) {
    val p = LocalHudPalette.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PLAYLISTS · ${playlists.size}", color = p.primary, letterSpacing = 2.sp, fontSize = 12.sp)
            Text(
                "+ NEW",
                color = p.voidBlack,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.primary)
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(playlists, key = { it.id }) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            1.dp,
                            if (pl.id == PlaylistStore.CAR_FAVORITES_ID) p.secondary.copy(alpha = 0.7f)
                            else p.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .background(p.panel.copy(alpha = 0.7f))
                        .clickable { onOpen(pl.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, color = p.soft)
                        Text("${pl.trackIds.size} tracks", color = p.dim, fontSize = 12.sp)
                    }
                    if (!pl.locked) {
                        IconButton(onClick = { onDelete(pl.id) }) {
                            Icon(Icons.Default.Delete, null, tint = p.danger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val accent = if (selected) p.accent else p.dim
    Text(
        text = label,
        color = if (selected) p.soft else p.dim,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else p.panel)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
}

@Composable
private fun TabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SourceChip(label, selected, modifier, onClick)
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val p = LocalHudPalette.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(p.primary),
        textStyle = androidx.compose.ui.text.TextStyle(color = p.soft, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .background(p.panel.copy(alpha = 0.8f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("SEARCH TITLE / ARTIST / ALBUM", color = p.dim, fontSize = 13.sp)
            inner()
        }
    )
}

@Composable
private fun NeonButton(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val p = LocalHudPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f), p.panel)))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(label, color = p.soft, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun NowPlayingBar(
    kenney: Boolean,
    playbackTitle: String,
    playbackArtist: String,
    artworkUri: Uri?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatModeUi,
    inCarFavorites: Boolean,
    progress: Float,
    onOpenPlayer: () -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onToggleCarFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val p = LocalHudPalette.current
    val hudFont = LocalHudFont.current
    val bodyFont = LocalBodyFont.current
    val animated by animateFloatAsState(progress, label = "progress")

    @Composable
    fun MetaAndControls() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.voidBlack)
                    .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "NOW PLAYING",
                    color = p.primary,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontFamily = hudFont
                )
                Text(
                    playbackTitle,
                    color = p.soft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = bodyFont
                )
                Text(
                    playbackArtist,
                    color = p.dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontFamily = bodyFont
                )
            }
            if (kenney) {
                ApolloHudIconButton(onClick = onToggleCarFavorite, size = 36.dp) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Save to Car Favorites",
                        tint = if (inCarFavorites) p.secondary else p.dim,
                        modifier = Modifier.size(18.dp)
                    )
                }
                ApolloHudIconButton(onClick = onAddToPlaylist, size = 36.dp) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = p.soft,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(onClick = onToggleCarFavorite) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Save to Car Favorites",
                        tint = if (inCarFavorites) p.secondary else p.dim
                    )
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = p.primary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (kenney) {
            ApolloHudProgress(animated, height = 14.dp)
        } else {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = p.secondary,
                trackColor = p.soft.copy(alpha = 0.12f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (kenney) {
                ApolloHudIconButton(onClick = onShuffle, size = 34.dp) {
                    Icon(Icons.Default.Shuffle, null, tint = if (shuffle) p.accent else p.dim, modifier = Modifier.size(16.dp))
                }
                ApolloHudIconButton(onClick = onPrev, size = 38.dp) {
                    Icon(Icons.Default.SkipPrevious, null, tint = p.primary, modifier = Modifier.size(20.dp))
                }
                ApolloHudIconButton(onClick = onToggle, size = 44.dp) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = p.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                ApolloHudIconButton(onClick = onNext, size = 38.dp) {
                    Icon(Icons.Default.SkipNext, null, tint = p.primary, modifier = Modifier.size(20.dp))
                }
                ApolloHudIconButton(onClick = onRepeat, size = 34.dp) {
                    Icon(
                        if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        null,
                        tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Default.Shuffle, null, tint = if (shuffle) p.accent else p.dim)
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, null, tint = p.primary)
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = p.accent
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, null, tint = p.primary)
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        if (repeatMode == RepeatModeUi.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        null,
                        tint = if (repeatMode == RepeatModeUi.OFF) p.dim else p.secondary
                    )
                }
            }
        }
    }

    if (kenney) {
        Box(Modifier.clickable(onClick = onOpenPlayer)) {
            ApolloSciFiPanel(title = null) {
                MetaAndControls()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, p.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .background(p.panel.copy(alpha = 0.85f))
                .clickable(onClick = onOpenPlayer)
                .padding(12.dp)
        ) {
            MetaAndControls()
        }
    }
}

private fun filterList(list: List<Track>, query: String): List<Track> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return list
    return list.filter {
        it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
    }
}

private fun progressOf(position: Long, duration: Long): Float {
    if (duration <= 0L) return 0f
    return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}
