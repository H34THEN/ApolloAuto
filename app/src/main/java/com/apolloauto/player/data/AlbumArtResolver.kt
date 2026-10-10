package com.apolloauto.player.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Resolves album artwork with MediaStore album-art URIs, then embedded pictures,
 * then folder sidecars. Missing/broken albumart content URIs are the usual cause
 * of intermittent "NO ART" thumbnails on phone scans.
 */
object AlbumArtResolver {
    private val COVER_NAMES = setOf(
        "cover.jpg", "cover.jpeg", "cover.png", "cover.webp",
        "folder.jpg", "folder.jpeg", "folder.png",
        "album.jpg", "album.jpeg", "album.png",
        "albumart.jpg", "albumart.jpeg", "albumart.png",
        "front.jpg", "front.jpeg", "front.png"
    )

    fun resolvePhoneArt(
        context: Context,
        albumId: Long,
        mediaUri: Uri,
        trackKey: String,
        cache: MutableMap<Long, Uri?> = mutableMapOf()
    ): Uri? {
        if (albumId > 0) {
            if (cache.containsKey(albumId)) return cache[albumId]
            val fromAlbum = openableAlbumArt(context, albumId)
                ?: albumContentUri(albumId).takeIf { canOpen(context, it) }
            if (fromAlbum != null) {
                cache[albumId] = fromAlbum
                return fromAlbum
            }
        }
        val embedded = extractEmbedded(context, mediaUri, trackKey)
        if (albumId > 0) cache[albumId] = embedded
        return embedded
    }

    fun isCoverFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name.lowercase(Locale.US) in COVER_NAMES
    }

    fun extractEmbedded(context: Context, mediaUri: Uri, trackKey: String): Uri? {
        val cacheFile = embeddedCacheFile(context, trackKey)
        if (cacheFile.isFile && cacheFile.length() > 32) {
            return Uri.fromFile(cacheFile)
        }
        val bytes = runCatching {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, mediaUri)
                mmr.embeddedPicture
            } finally {
                mmr.release()
            }
        }.getOrNull() ?: return null
        return runCatching {
            FileOutputStream(cacheFile).use { it.write(bytes) }
            Uri.fromFile(cacheFile)
        }.getOrNull()
    }

    fun loadThumbnailUri(context: Context, mediaUri: Uri, trackKey: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val cacheFile = File(artDir(context), "thumb_${sanitize(trackKey)}.jpg")
        if (cacheFile.isFile && cacheFile.length() > 32) return Uri.fromFile(cacheFile)
        return runCatching {
            val bmp = context.contentResolver.loadThumbnail(mediaUri, Size(512, 512), null)
            FileOutputStream(cacheFile).use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            }
            bmp.recycle()
            Uri.fromFile(cacheFile)
        }.getOrNull()
    }

    private fun openableAlbumArt(context: Context, albumId: Long): Uri? {
        val uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
        if (canOpen(context, uri)) return uri
        // Some OEMs only answer via the albums table.
        val albumUri = ContentUris.withAppendedId(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            },
            albumId
        )
        return albumUri.takeIf { canOpen(context, it) }
    }

    private fun albumContentUri(albumId: Long): Uri =
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )

    private fun canOpen(context: Context, uri: Uri): Boolean =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.read() >= 0 } == true
        }.getOrDefault(false)

    private fun artDir(context: Context): File =
        File(context.cacheDir, "album_art").also { it.mkdirs() }

    private fun embeddedCacheFile(context: Context, trackKey: String): File =
        File(artDir(context), "emb_${sanitize(trackKey)}.jpg")

    private fun sanitize(key: String): String =
        key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
}
