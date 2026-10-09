package com.apolloauto.player

import android.app.Application
import com.apolloauto.player.data.DeviceMusicScanner
import com.apolloauto.player.data.FavoritesStore
import com.apolloauto.player.data.LibraryStore
import com.apolloauto.player.data.PlaylistStore
import com.apolloauto.player.media.EqualizerController
import com.apolloauto.player.theme.ThemeStore
import com.apolloauto.player.usb.UsbMusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    }

    companion object {
        lateinit var instance: ApolloApp
            private set
    }
}
