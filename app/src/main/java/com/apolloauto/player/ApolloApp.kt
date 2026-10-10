package com.apolloauto.player

import android.app.Application
import com.apolloauto.player.data.DeviceMusicScanner
import com.apolloauto.player.data.FavoritesStore
import com.apolloauto.player.data.LibraryScanCoordinator
import com.apolloauto.player.data.LibraryStore
import com.apolloauto.player.companion.CompanionVoice
import com.apolloauto.player.data.M3uPlaylistService
import com.apolloauto.player.data.MusicSourceStore
import com.apolloauto.player.data.PlayHistoryStore
import com.apolloauto.player.data.TrackFirstSeenStore
import com.apolloauto.player.echohunt.EchoHuntCredentialStore
import com.apolloauto.player.echohunt.EchoHuntScanController
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.data.SafLibraryScanner
import com.apolloauto.player.media.AlbumArtColorStore
import com.apolloauto.player.media.AudioVisualizerController
import com.apolloauto.player.media.EqualizerController
import com.apolloauto.player.media.PlaybackSessionStore
import com.apolloauto.player.media.SleepTimerController
import com.apolloauto.player.media.StablePlaybackQueue
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.ThemeStore
import com.apolloauto.player.usb.UsbMusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

class ApolloApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var libraryStore: LibraryStore
        private set
    lateinit var musicSourceStore: MusicSourceStore
        private set
    lateinit var safLibraryScanner: SafLibraryScanner
        private set
    lateinit var libraryScanCoordinator: LibraryScanCoordinator
        private set
    lateinit var m3uPlaylistService: M3uPlaylistService
        private set
    lateinit var usbScanner: UsbMusicScanner
        private set
    lateinit var deviceScanner: DeviceMusicScanner
        private set
    lateinit var favoritesStore: FavoritesStore
        private set
    lateinit var playlistStore: PlaylistStore
        private set
    lateinit var themeStore: ThemeStore
        private set
    lateinit var equalizerController: EqualizerController
        private set
    lateinit var sleepTimerController: SleepTimerController
        private set
    lateinit var visualizerController: AudioVisualizerController
        private set
    lateinit var stablePlaybackQueue: StablePlaybackQueue
        private set
    lateinit var albumArtColors: AlbumArtColorStore
        private set
    lateinit var playHistoryStore: PlayHistoryStore
        private set
    lateinit var trackFirstSeenStore: TrackFirstSeenStore
        private set
    lateinit var playbackSessionStore: PlaybackSessionStore
        private set
    lateinit var echoHuntCredentials: EchoHuntCredentialStore
        private set
    lateinit var echoHuntScan: EchoHuntScanController
        private set

    /** Bound by playback service when live. */
    @Volatile
    var playbackBridge: PlaybackBridge? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        libraryStore = LibraryStore(this)
        playlistStore = PlaylistStore(this)
        trackFirstSeenStore = TrackFirstSeenStore(this)
        deviceScanner = DeviceMusicScanner(this, libraryStore)
        musicSourceStore = MusicSourceStore(this, libraryStore, trackFirstSeenStore)
        m3uPlaylistService = M3uPlaylistService(this, playlistStore)
        safLibraryScanner = SafLibraryScanner(this, musicSourceStore, libraryStore)
        libraryScanCoordinator = LibraryScanCoordinator(
            context = this,
            sourceStore = musicSourceStore,
            deviceScanner = deviceScanner,
            safScanner = safLibraryScanner,
            libraryStore = libraryStore,
            m3uService = m3uPlaylistService,
            playlistStore = playlistStore
        )
        usbScanner = UsbMusicScanner(this, libraryStore)
        favoritesStore = FavoritesStore(this)
        themeStore = ThemeStore(this)
        themeStore.ensureSourcePackModesEnabled()
        equalizerController = EqualizerController(this)
        visualizerController = AudioVisualizerController().also {
            it.setMode(themeStore.visualizerMode.value, reason = "boot")
            it.debugLogging = false
        }
        stablePlaybackQueue = StablePlaybackQueue()
        albumArtColors = AlbumArtColorStore(this, appScope)
        playHistoryStore = PlayHistoryStore(this)
        playbackSessionStore = PlaybackSessionStore(this)
        echoHuntCredentials = EchoHuntCredentialStore(this)
        echoHuntScan = EchoHuntScanController(echoHuntCredentials, appScope)
        // Retire broken / removed scenes if persisted as active mode.
        when (themeStore.visualizerMode.value) {
            VisualizerMode.XR3_SIERPINSKI -> {
                themeStore.setVisualizerMode(VisualizerMode.XR3_POLY)
                visualizerController.setMode(VisualizerMode.XR3_POLY, reason = "retire_sier")
            }
            VisualizerMode.PRISM_RAIN -> {
                themeStore.setVisualizerMode(VisualizerMode.TOPO_CONTOUR)
                visualizerController.setMode(VisualizerMode.TOPO_CONTOUR, reason = "retire_rain")
            }
            else -> Unit
        }
        themeStore.speak(CompanionVoice.bootLine())
        sleepTimerController = SleepTimerController(
            context = this,
            scope = appScope,
            pausePlayback = { playbackBridge?.pause() },
            fadeAndPause = { fadeMs ->
                val bridge = playbackBridge
                if (bridge == null) return@SleepTimerController
                val steps = 16
                val step = fadeMs / steps
                val startVol = bridge.volume()
                for (i in 1..steps) {
                    bridge.setVolume((startVol * (1f - i / steps.toFloat())).coerceIn(0f, 1f))
                    delay(step)
                }
                bridge.pause()
                bridge.setVolume(startVol)
            }
        )
    }

    companion object {
        lateinit var instance: ApolloApp
            private set
    }
}

interface PlaybackBridge {
    fun pause()
    fun volume(): Float
    fun setVolume(volume: Float)
    fun trackRemainingMs(): Long
    fun queueRemainingMs(): Long
    fun audioSessionId(): Int
    fun currentPositionMs(): Long = 0L
    fun currentMediaId(): String = ""
    fun currentTitle(): String = ""
}
