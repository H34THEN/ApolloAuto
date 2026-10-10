package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Fills missing [Track.artworkUri] after a fast library scan.
 * Folder covers are applied during SAF walk; embedded/thumbnail art is too slow
 * to do inline for multi-thousand libraries, so this runs afterward in batches.
 */
class AlbumArtEnricher(
    private val context: Context,
    private val sourceStore: MusicSourceStore,
    private val libraryStore: LibraryStore
) {
    private val mutex = Mutex()

    suspend fun enrichMissing(maxTracks: Int = Int.MAX_VALUE): Int = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) {
            Log.i(TAG, "enrich: already running — skip")
            return@withContext 0
        }
        try {
            enrichLocked(maxTracks)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun enrichLocked(maxTracks: Int): Int {
        val missing = libraryStore.tracks.value
            .asSequence()
            .filter { it.artworkUri == null && it.available }
            .take(maxTracks)
            .toList()
        if (missing.isEmpty()) {
            Log.i(TAG, "enrich: nothing missing")
            return 0
        }
        Log.i(TAG, "enrich: starting for ${missing.size} tracks")
        var filled = 0
        var processed = 0
        val pending = LinkedHashMap<String, Uri>()
        val albumCache = HashMap<Long, Uri?>()

        for (track in missing) {
            coroutineContext.ensureActive()
            processed++
            val art = resolve(track, albumCache) ?: continue
            pending[track.id] = art
            filled++
            if (pending.size >= PUBLISH_EVERY) {
                sourceStore.applyArtworkUpdates(pending)
                libraryStore.setStatus("Artwork ready for $filled tracks…")
                Log.i(TAG, "enrich: published +${pending.size} (filled=$filled / $processed)")
                pending.clear()
            }
        }
        if (pending.isNotEmpty()) {
            sourceStore.applyArtworkUpdates(pending)
        }
        libraryStore.setStatus(
            if (filled == 0) libraryStore.status.value
            else "Artwork loaded for $filled tracks"
        )
        Log.i(TAG, "enrich: done filled=$filled processed=$processed")
        return filled
    }

    private fun resolve(track: Track, albumCache: MutableMap<Long, Uri?>): Uri? {
        if (track.sourceKind == MusicSourceKind.PHONE_MEDIASTORE) {
            return AlbumArtResolver.resolvePhoneArt(
                context = context,
                albumId = track.albumId,
                mediaUri = track.uri,
                trackKey = track.id,
                cache = albumCache,
                allowEmbedded = true
            ) ?: AlbumArtResolver.loadThumbnailUri(context, track.uri, track.id)
        }
        // SAF / USB / SD: thumbnail first (usually faster), then embedded picture.
        return AlbumArtResolver.loadThumbnailUri(context, track.uri, track.id)
            ?: AlbumArtResolver.extractEmbedded(context, track.uri, track.id)
    }

    companion object {
        private const val TAG = "ApolloArtEnrich"
        private const val PUBLISH_EVERY = 24
    }
}
