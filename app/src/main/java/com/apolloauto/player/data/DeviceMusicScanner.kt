package com.apolloauto.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceMusicScanner(
    private val context: Context,
    private val store: LibraryStore
) {
    /**
     * @param publishDirectly when true (legacy), replaces the entire library with MediaStore results.
     * Prefer false and publish via [MusicSourceStore.recordScanResult].
     * When false, does **not** toggle [LibraryStore.setScanning] (coordinator owns that flag).
     */
    suspend fun scanPhoneLibrary(publishDirectly: Boolean = true): List<Track> = withContext(Dispatchers.IO) {
        if (publishDirectly) store.setScanning(true)
        store.setStatus("Scanning phone library…")
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.DATE_ADDED
            )
            // Exclude ringtones / notifications / alarms even if MediaStore source is enabled.
            val selection = buildString {
                append("${MediaStore.Audio.Media.IS_MUSIC}!=0")
                append(" AND ${MediaStore.Audio.Media.IS_RINGTONE}==0")
                append(" AND ${MediaStore.Audio.Media.IS_NOTIFICATION}==0")
                append(" AND ${MediaStore.Audio.Media.IS_ALARM}==0")
            }
            val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            val found = ArrayList<Track>(256)
            val artByAlbum = HashMap<Long, Uri?>()
            context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val relCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                var skippedSystemAudio = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                    val displayName = cursor.getString(displayCol)
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dateAddedMs = if (dateCol >= 0) {
                        cursor.getLong(dateCol).coerceAtLeast(0L) * 1000L
                    } else {
                        0L
                    }
                    val folder = cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Phone"
                    val relBase = if (relCol >= 0) {
                        cursor.getString(relCol)?.trimEnd('/') ?: ""
                    } else {
                        ""
                    }
                    val relPath = listOf(relBase, displayName.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString("/")
                    if (!MediaStoreMusicFilter.isLikelyMusic(
                            relativePath = relBase.ifBlank { relPath },
                            bucketDisplayName = folder,
                            displayName = displayName,
                            title = rawTitle,
                            durationMs = duration
                        )
                    ) {
                        skippedSystemAudio++
                        continue
                    }
                    val (title, artist, albumFromName) = FilenameMetadataParser.resolve(
                        embeddedTitle = rawTitle,
                        embeddedArtist = rawArtist,
                        displayName = displayName,
                        folderFallback = rawAlbum ?: folder
                    )
                    val album = rawAlbum?.takeUnless {
                        it.equals(rawTitle, ignoreCase = true)
                    } ?: albumFromName
                    val uri = ContentUris.withAppendedId(collection, id)
                    val trackKey = "phone:$id"
                    // Prefer MediaStore albumart URI only during scan — skip embedded/thumbnail
                    // extraction so large libraries appear quickly.
                    val art = AlbumArtResolver.resolvePhoneArt(
                        context = context,
                        albumId = albumId,
                        mediaUri = uri,
                        trackKey = trackKey,
                        cache = artByAlbum,
                        allowEmbedded = false
                    )
                    found += Track(
                        id = trackKey,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        folder = folder,
                        durationMs = duration,
                        artworkUri = art,
                        albumId = albumId,
                        sourceId = MusicSourceStore.PHONE_MEDIASTORE_ID,
                        sourceKind = MusicSourceKind.PHONE_MEDIASTORE,
                        relativePath = relPath,
                        available = true,
                        dateAddedMs = dateAddedMs
                    )
                }
                if (skippedSystemAudio > 0) {
                    Log.i(TAG, "Skipped $skippedSystemAudio MediaStore system/ringtone clips")
                }
            }

            if (publishDirectly) {
                store.setTracks(found)
            }
            store.setStatus(
                if (found.isEmpty()) "No music on phone yet — add a folder in Settings → Library"
                else "${found.size} phone tracks ready"
            )
            Log.i(TAG, "MediaStore scan complete: ${found.size} tracks")
            found
        } catch (t: Throwable) {
            Log.e(TAG, "Phone scan failed", t)
            store.setStatus("Phone scan failed: ${t.message ?: "permission?"}")
            emptyList()
        } finally {
            if (publishDirectly) store.setScanning(false)
        }
    }

    companion object {
        private const val TAG = "ApolloPhoneScan"
    }
}
