package com.apolloauto.player.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.PlaybackBridge
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.Track
import com.apolloauto.player.ui.CarModeActivity
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class ApolloPlaybackService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var librarySession: MediaLibrarySession? = null
    /** Last browsed playable children — used to expand single-track AA taps into queues. */
    private val browsePlayContext = BrowsePlayContext()

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        fun attachAudioFx(sessionId: Int) {
            ApolloApp.instance.equalizerController.attach(sessionId)
            ApolloApp.instance.visualizerController.attach(sessionId)
        }
        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachAudioFx(audioSessionId)
            }
        })
        if (exo.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            attachAudioFx(exo.audioSessionId)
        }

        ApolloApp.instance.playbackBridge = object : PlaybackBridge {
            override fun pause() {
                exo.pause()
            }

            override fun volume(): Float = exo.volume

            override fun setVolume(volume: Float) {
                exo.volume = volume.coerceIn(0f, 1f)
            }

            override fun trackRemainingMs(): Long {
                val dur = exo.duration
                if (dur <= 0L || dur == C.TIME_UNSET) return 60_000L
                return (dur - exo.currentPosition).coerceAtLeast(1_000L)
            }

            override fun queueRemainingMs(): Long {
                var total = trackRemainingMs()
                val current = exo.currentMediaItemIndex
                val count = exo.mediaItemCount
                for (i in (current + 1) until count) {
                    // Unknown per-item durations without probing; estimate 3.5 min each
                    total += 210_000L
                }
                return total.coerceAtLeast(1_000L)
            }

            override fun audioSessionId(): Int = exo.audioSessionId

            override fun currentPositionMs(): Long =
                exo.currentPosition.coerceAtLeast(0L)

            override fun currentMediaId(): String =
                exo.currentMediaItem?.mediaId.orEmpty()

            override fun currentTitle(): String =
                exo.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, CarModeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        librarySession = MediaLibrarySession.Builder(this, exo, LibraryCallback())
            .setSessionActivity(sessionActivity)
            .setId("apollo_auto_session")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        librarySession

    override fun onDestroy() {
        if (ApolloApp.instance.playbackBridge != null) {
            ApolloApp.instance.playbackBridge = null
        }
        ApolloApp.instance.visualizerController.release()
        ApolloApp.instance.equalizerController.release()
        librarySession?.release()
        librarySession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val automotive = AndroidAutoBrowse.isAutomotiveController(controller.packageName)
            Log.i(TAG, "onConnect pkg=${controller.packageName} automotive=$automotive")

            // Keep transport commands available; AA hides Next when the timeline has <2 items
            // (see MediaQueueExpand) — not when commands are missing from this list.
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS

            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .apply {
                    // One car-safe custom action. Extra customs crowd the small Now Playing row
                    // and can displace Next on compact HUDs.
                    add(SessionCommand(MediaIds.CMD_SAVE_CAR_FAV, Bundle.EMPTY))
                    if (!automotive) {
                        add(SessionCommand(MediaIds.CMD_NEXT_VISUAL, Bundle.EMPTY))
                        add(SessionCommand(MediaIds.CMD_TOGGLE_DRIVE, Bundle.EMPTY))
                    }
                }
                .build()

            // Do not set a customLayout here: on compact AA Now Playing rows, extra
            // custom buttons can crowd out the host Next control. Car Fav remains
            // available via session commands / browse → Car Favorites.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(playerCommands)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val trackCount = ApolloApp.instance.libraryStore.tracks.value.size
            val root = MediaItem.Builder()
                .setMediaId(MediaIds.ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Apollo II")
                        .setSubtitle("$trackCount tracks")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val store = ApolloApp.instance.libraryStore
            store.setQuery(query)
            val items = ImmutableList.copyOf(store.filteredTracks().map { it.toPlayable() })
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val app = ApolloApp.instance
            val store = app.libraryStore
            val playlists = app.playlistStore.playlists.value
            val tracks = store.tracks.value
            val items = when {
                parentId == MediaIds.ROOT -> {
                    val car = app.playlistStore.carFavorites()
                    ImmutableList.copyOf(
                        AndroidAutoBrowse.rootNodes(
                            favoritesMediaId = MediaIds.PLAYLIST_PREFIX + PlaylistStore.CAR_FAVORITES_ID,
                            favoritesCount = car.trackIds.size,
                            playlistCount = playlists.size,
                            trackCount = tracks.size
                        ).map { folderItem(it.id, it.title, it.subtitle) }
                    )
                }
                parentId == MediaIds.MORE -> {
                    ImmutableList.copyOf(
                        AndroidAutoBrowse.moreNodes(store.folders().size)
                            .map { folderItem(it.id, it.title, it.subtitle) }
                    )
                }
                parentId == MediaIds.PLAYLISTS -> {
                    ImmutableList.copyOf(
                        playlists.map {
                            folderItem(
                                MediaIds.PLAYLIST_PREFIX + it.id,
                                it.name,
                                "${it.trackIds.size} tracks"
                            )
                        }
                    )
                }
                parentId == MediaIds.FOLDERS -> {
                    ImmutableList.copyOf(store.folders().map { folderBrowsable(it) })
                }
                parentId == MediaIds.RECENTLY_ADDED -> {
                    ImmutableList.copyOf(
                        com.apolloauto.player.data.SmartPlaylistQueries.recentlyAdded(
                            tracks,
                            app.trackFirstSeenStore.map.value
                        ).map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.UNPLAYED -> {
                    ImmutableList.copyOf(
                        com.apolloauto.player.data.SmartPlaylistQueries.unplayed(
                            tracks,
                            app.playHistoryStore.stats.value
                        ).map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.RECENT -> {
                    ImmutableList.copyOf(
                        app.playHistoryStore.recentlyPlayed(tracks).map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.MOST_PLAYED -> {
                    ImmutableList.copyOf(
                        app.playHistoryStore.mostPlayed(tracks).map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.ARTISTS -> {
                    val artists = tracks.map { it.artist.ifBlank { "Unknown" } }.distinct().sorted()
                    ImmutableList.copyOf(
                        artists.map { folderItem(MediaIds.ARTIST_PREFIX + it, it) }
                    )
                }
                parentId.startsWith(MediaIds.ARTIST_PREFIX) -> {
                    val artist = parentId.removePrefix(MediaIds.ARTIST_PREFIX)
                    ImmutableList.copyOf(
                        tracks.filter { it.artist.ifBlank { "Unknown" } == artist }.map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.ALBUMS -> {
                    val albums = tracks.map { it.album.ifBlank { "Unknown Album" } }.distinct().sorted()
                    ImmutableList.copyOf(
                        albums.map { folderItem(MediaIds.ALBUM_PREFIX + it, it) }
                    )
                }
                parentId.startsWith(MediaIds.ALBUM_PREFIX) -> {
                    val album = parentId.removePrefix(MediaIds.ALBUM_PREFIX)
                    ImmutableList.copyOf(
                        tracks.filter { it.album.ifBlank { "Unknown Album" } == album }.map { it.toPlayable() }
                    )
                }
                parentId == MediaIds.VISUALIZERS -> {
                    val enabled = app.themeStore.enabledVisualizerModes.value
                    ImmutableList.copyOf(
                        VisualizerMode.entries.filter { it in enabled }.map { mode ->
                            folderItem(
                                MediaIds.VIZ_PREFIX + mode.name,
                                mode.name.replace('_', ' '),
                                "Phone Immersive only"
                            )
                        }
                    )
                }
                parentId.startsWith(MediaIds.VIZ_PREFIX) -> {
                    val name = parentId.removePrefix(MediaIds.VIZ_PREFIX)
                    runCatching {
                        val mode = VisualizerMode.valueOf(name)
                        app.themeStore.setVisualizerMode(mode)
                        app.visualizerController.setMode(mode)
                        app.themeStore.speak(com.apolloauto.player.companion.CompanionVoice.vizLine())
                    }
                    ImmutableList.of()
                }
                parentId == MediaIds.ALL -> {
                    ImmutableList.copyOf(tracks.map { it.toPlayable() })
                }
                parentId.startsWith(MediaIds.PLAYLIST_PREFIX) -> {
                    val id = parentId.removePrefix(MediaIds.PLAYLIST_PREFIX)
                    ImmutableList.copyOf(
                        app.playlistStore.resolveById(id, tracks).map { it.toPlayable() }
                    )
                }
                parentId.startsWith(MediaIds.FOLDER_PREFIX) -> {
                    val folder = parentId.removePrefix(MediaIds.FOLDER_PREFIX)
                    ImmutableList.copyOf(store.tracksInFolder(folder).map { it.toPlayable() })
                }
                else -> ImmutableList.of()
            }
            // Cache playable lists so a later single-item play expands to that collection.
            browsePlayContext.remember(parentId, items)
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val store = ApolloApp.instance.libraryStore
            val catalog = store.tracks.value
            var resolved = mediaItems.map { item ->
                val track = catalog.find { it.id == item.mediaId }
                track?.toPlayable() ?: item
            }
            if (resolved.size == 1) {
                val expanded = expandForPlayback(resolved[0], catalog)
                if (expanded.items.size > 1) {
                    Log.i(
                        TAG,
                        "onAddMediaItems expand -> ${expanded.items.size} " +
                            "(browse=${browsePlayContext.debugParent()})"
                    )
                    resolved = expanded.items
                }
            }
            return Futures.immediateFuture(resolved.toMutableList())
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val store = ApolloApp.instance.libraryStore
            val catalog = store.tracks.value
            val resolved = mediaItems.mapNotNull { item ->
                catalog.find { it.id == item.mediaId }?.toPlayable()
                    ?: if (item.localConfiguration != null) item else null
            }

            if (resolved.isEmpty() && catalog.isNotEmpty()) {
                val all = catalog.map { it.toPlayable() }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(all, 0, C.TIME_UNSET)
                )
            }

            // Single-track taps from AA browse must expand or Next/Queue stay empty.
            if (resolved.size == 1) {
                val expanded = expandForPlayback(resolved[0], catalog)
                Log.i(
                    TAG,
                    "onSetMediaItems expand -> ${expanded.items.size} start=${expanded.startIndex} " +
                        "(browse=${browsePlayContext.debugParent()})"
                )
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        expanded.items,
                        expanded.startIndex,
                        startPositionMs
                    )
                )
            }

            val safeIndex = startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs)
            )
        }

        private fun expandForPlayback(
            selected: MediaItem,
            catalog: List<com.apolloauto.player.data.Track>
        ): MediaQueueExpand.ExpandedQueue {
            val byId = catalog.associateBy { it.id }
            val browseIds = browsePlayContext.expandIdsFromBrowse(selected.mediaId)
            val browseSiblings = browseIds?.first?.mapNotNull { id ->
                byId[id]?.toPlayable()
            }
            return MediaQueueExpand.expandSingle(
                selected = selected,
                catalog = catalog,
                toPlayable = { it.toPlayable() },
                browseSiblings = browseSiblings?.takeIf { it.size > 1 }
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val app = ApolloApp.instance
            when (customCommand.customAction) {
                MediaIds.CMD_NEXT_VISUAL -> {
                    val enabled = app.themeStore.enabledVisualizerModes.value
                    val current = app.visualizerController.mode.value
                    val next = com.apolloauto.player.ui.VisualizerModeCatalog.nextMode(current, enabled)
                    app.themeStore.setVisualizerMode(next)
                    app.visualizerController.setMode(next)
                    app.themeStore.speak(com.apolloauto.player.companion.CompanionVoice.vizLine())
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                MediaIds.CMD_TOGGLE_DRIVE -> {
                    val on = app.themeStore.toggleDriveMode()
                    app.themeStore.speak(
                        if (on) com.apolloauto.player.companion.CompanionVoice.driveOnLine()
                        else com.apolloauto.player.companion.CompanionVoice.driveOffLine()
                    )
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                MediaIds.CMD_SAVE_CAR_FAV -> {
                    val id = player?.currentMediaItem?.mediaId
                    if (!id.isNullOrBlank()) {
                        app.playlistStore.toggleCarFavorite(id)
                        app.themeStore.speak(com.apolloauto.player.companion.CompanionVoice.favoriteLine())
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val app = ApolloApp.instance
            val saved = app.playbackSessionStore.load()
            if (saved != null) {
                val items = saved.toMediaItems().filter { it.localConfiguration?.uri != null }
                if (items.isNotEmpty()) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            items,
                            saved.index.coerceIn(0, items.lastIndex),
                            saved.positionMs
                        )
                    )
                }
            }
            val car = app.playlistStore.resolveById(
                PlaylistStore.CAR_FAVORITES_ID,
                app.libraryStore.tracks.value
            )
            val tracks = (car.ifEmpty { app.libraryStore.tracks.value }).map { it.toPlayable() }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(tracks, 0, C.TIME_UNSET)
            )
        }
    }

    private fun folderItem(id: String, title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .apply { if (!subtitle.isNullOrBlank()) setSubtitle(subtitle) }
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()

    private fun folderBrowsable(name: String): MediaItem =
        folderItem(MediaIds.FOLDER_PREFIX + name, name)

    private fun Track.toPlayable(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

    companion object {
        private const val TAG = "ApolloAASession"
    }
}
