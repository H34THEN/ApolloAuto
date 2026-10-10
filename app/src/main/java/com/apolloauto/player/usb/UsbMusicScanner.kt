package com.apolloauto.player.usb

import android.content.Context
import android.net.Uri
import com.apolloauto.player.data.LibraryStore
import com.apolloauto.player.data.MusicSourceKind
import com.apolloauto.player.data.MusicSourceStore
import com.apolloauto.player.data.SafLibraryScanner
import com.apolloauto.player.data.Track

/**
 * Legacy façade — USB scans now go through [SafLibraryScanner] + [MusicSourceStore]
 * so phone SAF folders are not mislabeled as USB.
 */
class UsbMusicScanner(
    private val context: Context,
    private val store: LibraryStore
) {
    private val sourceStore: MusicSourceStore?
        get() = runCatching { com.apolloauto.player.ApolloApp.instance.musicSourceStore }.getOrNull()
    private val saf: SafLibraryScanner?
        get() = runCatching { com.apolloauto.player.ApolloApp.instance.safLibraryScanner }.getOrNull()

    suspend fun scanTree(treeUri: Uri): List<Track> {
        val sources = sourceStore
        val scanner = saf
        if (sources != null && scanner != null) {
            val source = sources.upsertSafSource(
                treeUri = treeUri,
                kind = MusicSourceKind.USB_SSD,
                label = "USB SSD",
                makePreferred = true
            )
            return scanner.scanSource(source).tracks
        }
        // Extremely early boot fallback
        store.setStatus("USB scanner unavailable")
        return emptyList()
    }
}
