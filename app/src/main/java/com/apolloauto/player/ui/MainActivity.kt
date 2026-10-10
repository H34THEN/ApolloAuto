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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.LibrarySource
import com.apolloauto.player.data.Playlist
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.Track
import com.apolloauto.player.data.displayArtist
import com.apolloauto.player.data.displaySecondary
import com.apolloauto.player.data.displayTitle
import com.apolloauto.player.media.PlayerController
import com.apolloauto.player.theme.ApolloCyberTheme
import com.apolloauto.player.theme.ApolloSpace
import com.apolloauto.player.theme.ApolloTypeScale
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.components.ArtworkThumb
import com.apolloauto.player.ui.home.ApolloBottomNav
import com.apolloauto.player.ui.home.CompactApolloHeader
import com.apolloauto.player.ui.home.HomeDestination
import com.apolloauto.player.ui.home.LibraryCategory
import com.apolloauto.player.ui.home.LibraryFilterChips
import com.apolloauto.player.ui.home.NowPlayingCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val body = LocalBodyFont.current
    val hud = LocalHudFont.current
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

    var destination by remember { mutableStateOf(HomeDestination.HOME) }
    var libraryCategory by remember { mutableStateOf(LibraryCategory.TRACKS) }
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

    fun openSettings() {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun openCarMode() {
        context.startActivity(Intent(context, CarModeActivity::class.java))
    }

    CyberBackground {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = ApolloSpace.screenPadding)
                        .padding(top = ApolloSpace.space8)
                ) {
                    CompactApolloHeader(
                        themeLabel = themeId.label.take(14),
                        sourceLabel = source.name,
                        playing = playback.playing,
                        sleepLabel = if (sleep.running) "SLEEP ${sleep.remainingMs / 60000}m" else null,
                        onSettings = ::openSettings
                    )

                    when (destination) {
                        HomeDestination.HOME -> {
                            Text(status, color = p.dim, fontFamily = body, fontSize = ApolloTypeScale.metaSp.sp, maxLines = 1)
                            Spacer(Modifier.height(ApolloSpace.space8))
                            SourceScanRow(
                                source = source,
                                scanning = scanning,
                                onPhone = { app.libraryStore.setSource(LibrarySource.PHONE) },
                                onUsb = { app.libraryStore.setSource(LibrarySource.USB) },
                                onScan = {
                                    if (source == LibrarySource.PHONE) requestPhoneScan()
                                    else openTree.launch(null)
                                }
                            )
                            Spacer(Modifier.height(ApolloSpace.panelGap))
                            NowPlayingCard(
                                title = playback.title,
                                artist = playback.artist,
                                album = playback.album,
                                artworkUri = playback.artworkUri,
                                playing = playback.playing,
                                shuffle = playback.shuffle,
                                repeatMode = playback.repeatMode,
                                inCarFavorites = playback.mediaId.isNotBlank() &&
                                    app.playlistStore.isInCarFavorites(playback.mediaId),
                                progress = progressOf(playback.positionMs, playback.durationMs),
                                positionLabel = fmtMs(playback.positionMs),
                                durationLabel = fmtMs(playback.durationMs),
                                interfaceStyle = interfaceStyle,
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
                            Spacer(Modifier.height(ApolloSpace.panelGap))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "LIBRARY // ${filtered.size}",
                                    color = p.primary,
                                    fontFamily = body,
                                    fontSize = ApolloTypeScale.sectionSp.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "SEE ALL",
                                    color = p.accent,
                                    fontFamily = body,
                                    fontSize = ApolloTypeScale.metaSp.sp,
                                    modifier = Modifier
                                        .clickable {
                                            destination = HomeDestination.LIBRARY
                                            libraryCategory = LibraryCategory.TRACKS
                                        }
                                        .padding(ApolloSpace.space4)
                                )
                            }
                            Spacer(Modifier.height(ApolloSpace.space8))
                            TrackList(
                                tracks = filtered.take(40),
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                modifier = Modifier.weight(1f),
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onAdd = { track ->
                                    addTrackId = track.id
                                    addTrackTitle = track.title
                                },
                                onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                            )
                        }

                        HomeDestination.LIBRARY -> {
                            SearchField(query) { app.libraryStore.setQuery(it) }
                            Spacer(Modifier.height(ApolloSpace.space8))
                            LibraryFilterChips(
                                selected = libraryCategory,
                                onSelect = {
                                    libraryCategory = it
                                    selectedPlaylistId = null
                                }
                            )
                            Spacer(Modifier.height(ApolloSpace.space8))
                            Text(
                                when (libraryCategory) {
                                    LibraryCategory.TRACKS -> "LIBRARY // ${filtered.size}"
                                    LibraryCategory.CAR -> "CAR FAVORITES // ${carTracks.size}"
                                    LibraryCategory.PLAYLISTS -> "PLAYLISTS // ${playlists.size}"
                                },
                                color = p.primary,
                                fontFamily = hud,
                                fontSize = ApolloTypeScale.sectionSp.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(ApolloSpace.space8))
                            when (libraryCategory) {
                                LibraryCategory.TRACKS -> TrackList(
                                    tracks = filtered,
                                    playingId = playback.mediaId,
                                    playlistStore = app.playlistStore,
                                    emptyText = "No tracks yet. Scan phone or link USB.",
                                    modifier = Modifier.weight(1f),
                                    onPlay = { list, index -> controller.playTracks(list, index) },
                                    onAdd = { track ->
                                        addTrackId = track.id
                                        addTrackTitle = track.title
                                    },
                                    onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                                )

                                LibraryCategory.CAR -> TrackList(
                                    tracks = carTracks,
                                    playingId = playback.mediaId,
                                    playlistStore = app.playlistStore,
                                    emptyText = "No Car Favorites yet. Save a track to add it here.",
                                    modifier = Modifier.weight(1f),
                                    onPlay = { list, index -> controller.playTracks(list, index) },
                                    onAdd = { track ->
                                        addTrackId = track.id
                                        addTrackTitle = track.title
                                    },
                                    onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                                )

                                LibraryCategory.PLAYLISTS -> {
                                    if (selectedPlaylistId == null) {
                                        PlaylistBrowser(
                                            playlists = playlists,
                                            modifier = Modifier.weight(1f),
                                            onOpen = { selectedPlaylistId = it },
                                            onCreate = {
                                                val created = app.playlistStore.create("Deck ${playlists.size}")
                                                selectedPlaylistId = created.id
                                            },
                                            onDelete = { id -> app.playlistStore.delete(id) }
                                        )
                                    } else {
                                        val pl = playlists.find { it.id == selectedPlaylistId }
                                        Column(Modifier.weight(1f)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "← ${pl?.name ?: "Playlist"}",
                                                    color = p.primary,
                                                    fontFamily = body,
                                                    modifier = Modifier
                                                        .clickable { selectedPlaylistId = null }
                                                        .padding(vertical = 6.dp)
                                                )
                                                Text(
                                                    "PLAY ALL",
                                                    color = p.voidBlack,
                                                    fontFamily = body,
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
                                                tracks = selectedPlaylistTracks,
                                                playingId = playback.mediaId,
                                                playlistStore = app.playlistStore,
                                                emptyText = "Add songs from Library via the overflow menu.",
                                                modifier = Modifier.weight(1f),
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

                        HomeDestination.PLAYLISTS -> {
                            if (selectedPlaylistId == null) {
                                PlaylistBrowser(
                                    playlists = playlists,
                                    modifier = Modifier.fillMaxSize(),
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
                                            fontFamily = body,
                                            modifier = Modifier
                                                .clickable { selectedPlaylistId = null }
                                                .padding(vertical = 6.dp)
                                        )
                                        Text(
                                            "PLAY ALL",
                                            color = p.voidBlack,
                                            fontFamily = body,
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
                                        tracks = selectedPlaylistTracks,
                                        playingId = playback.mediaId,
                                        playlistStore = app.playlistStore,
                                        emptyText = "Add songs from Library via the overflow menu.",
                                        modifier = Modifier.weight(1f),
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

                        HomeDestination.CAR -> {
                            Text(
                                "CAR MODE",
                                color = p.primary,
                                fontFamily = hud,
                                fontSize = ApolloTypeScale.screenHeadingSp.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(ApolloSpace.space8))
                            Text(
                                "Landscape deck for driving. Car Favorites stay locked and sync with Android Auto.",
                                color = p.dim,
                                fontFamily = body,
                                fontSize = ApolloTypeScale.bodySp.sp
                            )
                            Spacer(Modifier.height(ApolloSpace.space16))
                            Text(
                                "Open Car Mode",
                                color = p.voidBlack,
                                fontFamily = body,
                                fontSize = ApolloTypeScale.buttonSp.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ApolloSpace.corner))
                                    .background(p.primary)
                                    .clickable(onClick = ::openCarMode)
                                    .padding(vertical = 14.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(ApolloSpace.space16))
                            Text(
                                "CAR FAVORITES // ${carTracks.size}",
                                color = p.secondary,
                                fontFamily = hud,
                                fontSize = ApolloTypeScale.sectionSp.sp
                            )
                            Spacer(Modifier.height(ApolloSpace.space8))
                            TrackList(
                                tracks = carTracks,
                                playingId = playback.mediaId,
                                playlistStore = app.playlistStore,
                                emptyText = "No Car Favorites yet. Save a track to add it here.",
                                modifier = Modifier.weight(1f),
                                onPlay = { list, index -> controller.playTracks(list, index) },
                                onAdd = { track ->
                                    addTrackId = track.id
                                    addTrackTitle = track.title
                                },
                                onToggleCar = { app.playlistStore.toggleCarFavorite(it.id) }
                            )
                        }

                        HomeDestination.SETTINGS -> {
                            LaunchedEffect(Unit) {
                                openSettings()
                                destination = HomeDestination.HOME
                            }
                        }
                    }
                }

                ApolloBottomNav(
                    current = when (destination) {
                        HomeDestination.SETTINGS -> HomeDestination.HOME
                        else -> destination
                    },
                    onSelect = { dest ->
                        when (dest) {
                            HomeDestination.SETTINGS -> openSettings()
                            HomeDestination.CAR -> {
                                // Keep in-app Car hub; long-press style: also available via open button
                                destination = HomeDestination.CAR
                            }
                            HomeDestination.PLAYLISTS -> {
                                destination = HomeDestination.PLAYLISTS
                                libraryCategory = LibraryCategory.PLAYLISTS
                                selectedPlaylistId = null
                            }
                            HomeDestination.LIBRARY -> {
                                destination = HomeDestination.LIBRARY
                                selectedPlaylistId = null
                            }
                            HomeDestination.HOME -> destination = HomeDestination.HOME
                        }
                    }
                )
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
private fun SourceScanRow(
    source: LibrarySource,
    scanning: Boolean,
    onPhone: () -> Unit,
    onUsb: () -> Unit,
    onScan: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(ApolloSpace.space8)) {
            CompactChip("PHONE", source == LibrarySource.PHONE, Modifier.weight(1f), onPhone)
            CompactChip("USB SSD", source == LibrarySource.USB, Modifier.weight(1f), onUsb)
            CompactChip(
                if (source == LibrarySource.PHONE) "SCAN" else "LINK",
                false,
                Modifier.weight(0.85f),
                onScan
            )
        }
        if (scanning) {
            Spacer(Modifier.height(ApolloSpace.space8))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = p.accent,
                trackColor = p.panel
            )
        }
        // Keep icons for TalkBack context without large neon blocks
        Row(modifier = Modifier.padding(top = 2.dp)) {
            Icon(
                if (source == LibrarySource.PHONE) Icons.Default.PhoneAndroid else Icons.Default.Usb,
                contentDescription = null,
                tint = p.dim,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (source == LibrarySource.PHONE) "Phone library" else "USB storage",
                color = p.dim,
                fontFamily = body,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CompactChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        fontFamily = body,
        fontSize = ApolloTypeScale.metaSp.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(ApolloSpace.cornerTight))
            .border(
                1.dp,
                if (selected) p.accent else p.primary.copy(alpha = 0.3f),
                RoundedCornerShape(ApolloSpace.cornerTight)
            )
            .background(if (selected) p.accent.copy(alpha = 0.14f) else p.panel.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    playingId: String,
    playlistStore: PlaylistStore,
    emptyText: String = "No tracks yet.",
    modifier: Modifier = Modifier,
    onPlay: (List<Track>, Int) -> Unit,
    onAdd: (Track) -> Unit,
    onToggleCar: (Track) -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    if (tracks.isEmpty()) {
        Box(
            modifier
                .fillMaxWidth()
                .padding(ApolloSpace.space16),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                Text(emptyText, color = p.dim, fontFamily = body, fontSize = ApolloTypeScale.bodySp.sp)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = ApolloSpace.space16),
            verticalArrangement = Arrangement.spacedBy(ApolloSpace.space8),
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    playing = track.id == playingId,
                    inCar = playlistStore.isInCarFavorites(track.id),
                    onPlay = { onPlay(tracks, index) },
                    onAdd = { onAdd(track) },
                    onToggleCar = { onToggleCar(track) }
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    playing: Boolean,
    inCar: Boolean,
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onToggleCar: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(ApolloSpace.cornerTight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ApolloSpace.rowHeight)
            .clip(shape)
            .border(
                1.dp,
                if (playing) p.accent.copy(alpha = 0.85f) else p.primary.copy(alpha = 0.22f),
                shape
            )
            .background(
                if (playing) p.accent.copy(alpha = 0.08f) else p.panel.copy(alpha = 0.65f)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = ApolloSpace.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkThumb(artworkUri = track.artworkUri, size = ApolloSpace.thumbArt)
        Spacer(Modifier.width(ApolloSpace.space12))
        Column(Modifier.weight(1f)) {
            Text(
                track.displayTitle(),
                color = if (playing) p.accent else p.soft,
                fontFamily = body,
                fontSize = ApolloTypeScale.bodySp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.displayArtist(),
                color = p.dim,
                fontFamily = body,
                fontSize = ApolloTypeScale.metaSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val secondary = track.displaySecondary()
            if (secondary != null) {
                Text(
                    secondary,
                    color = p.dim.copy(alpha = 0.8f),
                    fontFamily = body,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (inCar) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = "In Car Favorites",
                tint = p.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(ApolloSpace.touchMin)) {
                Icon(Icons.Default.MoreVert, "Track actions", tint = p.primary)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = { menu = false; onAdd() },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (inCar) "Remove Car Favorite" else "Save to Car Favorites") },
                    onClick = { menu = false; onToggleCar() },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistBrowser(
    playlists: List<Playlist>,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val hud = LocalHudFont.current
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PLAYLISTS // ${playlists.size}",
                color = p.primary,
                fontFamily = hud,
                letterSpacing = 1.sp,
                fontSize = ApolloTypeScale.sectionSp.sp
            )
            Text(
                "+ NEW",
                color = p.voidBlack,
                fontFamily = body,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.primary)
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(ApolloSpace.space8))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(ApolloSpace.space8)) {
            items(playlists, key = { it.id }) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ApolloSpace.cornerTight))
                        .border(
                            1.dp,
                            if (pl.id == PlaylistStore.CAR_FAVORITES_ID) p.secondary.copy(alpha = 0.7f)
                            else p.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(ApolloSpace.cornerTight)
                        )
                        .background(p.panel.copy(alpha = 0.7f))
                        .clickable { onOpen(pl.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, color = p.soft, fontFamily = body, fontSize = ApolloTypeScale.bodySp.sp)
                        Text(
                            "${pl.trackIds.size} tracks",
                            color = p.dim,
                            fontFamily = body,
                            fontSize = ApolloTypeScale.metaSp.sp
                        )
                    }
                    if (!pl.locked) {
                        IconButton(onClick = { onDelete(pl.id) }) {
                            Icon(Icons.Default.Delete, "Delete playlist", tint = p.danger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(p.primary),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = p.soft,
            fontSize = 15.sp,
            fontFamily = body
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(ApolloSpace.cornerTight))
            .background(p.panel.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    "Search title, artist, album…",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 14.sp
                )
            }
            inner()
        }
    )
}

private fun filterList(list: List<Track>, query: String): List<Track> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return list
    return list.filter {
        it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.folder.lowercase().contains(q)
    }
}

private fun progressOf(position: Long, duration: Long): Float {
    if (duration <= 0L) return 0f
    return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
