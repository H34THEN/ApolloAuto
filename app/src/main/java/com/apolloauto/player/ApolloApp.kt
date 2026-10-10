package com.apolloauto.player

import android.app.Application
import com.apolloauto.player.data.DeviceMusicScanner
import com.apolloauto.player.data.FavoritesStore
import com.apolloauto.player.data.LibraryStore
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.media.AudioVisualizerController
import com.apolloauto.player.media.EqualizerController
import com.apolloauto.player.media.SleepTimerController
import com.apolloauto.player.media.StablePlaybackQueue
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

    /** Bound by playback service when live. */
    @Volatile
    var playbackBridge: PlaybackBridge? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        libraryStore = LibraryStore(this)
        usbScanner = UsbMusicScanner(this, libraryStore)
        deviceScanner = DeviceMusicScanner(this, libraryStore)
        favoritesStore = FavoritesStore(this)
        playlistStore = PlaylistStore(this)
        themeStore = ThemeStore(this)
        equalizerController = EqualizerController(this)
        visualizerController = AudioVisualizerController().also {
            it.setMode(themeStore.visualizerMode.value)
        }
        stablePlaybackQueue = StablePlaybackQueue()
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
}
