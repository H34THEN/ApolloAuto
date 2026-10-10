package com.apolloauto.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceMusicScanner(
    private val context: Context,
    private val store: LibraryStore
) {
    suspend fun scanPhoneLibrary(): List<Track> = withContext(Dispatchers.IO) {
        store.setScanning(true)
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
                MediaStore.Audio.Media.DISPLAY_NAME
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC}!=0"
            val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            val found = ArrayList<Track>(256)
            context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                    val displayName = cursor.getString(displayCol)
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val folder = cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Phone"
                    val (title, artist, albumFromName) = FilenameMetadataParser.resolve(
                        embeddedTitle = rawTitle,
                        embeddedArtist = rawArtist,
                        displayName = displayName,
                        folderFallback = rawAlbum ?: folder
                    )
                    val album = rawAlbum?.takeUnless {
                        // If album duplicates unresolved combined title, prefer folder/parsed fallback.
                        it.equals(rawTitle, ignoreCase = true)
                    } ?: albumFromName
                    val uri = ContentUris.withAppendedId(collection, id)
                    val art = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                    } else null
                    found += Track(
                        id = "phone:$id",
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        folder = folder,
                        durationMs = duration,
                        artworkUri = art,
                        albumId = albumId
                    )
                }
            }

            store.setTracks(found)
            store.setStatus(
                if (found.isEmpty()) "No music on phone yet — add files or link USB later"
                else "${found.size} phone tracks ready"
            )
            found
        } catch (t: Throwable) {
            store.setStatus("Phone scan failed: ${t.message ?: "permission?"}")
            emptyList()
        } finally {
            store.setScanning(false)
        }
    }
}
