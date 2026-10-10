package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Recursive SAF tree scanner for phone folders, USB SSD, and SD card roots.
 * Discovers audio files and M3U/M3U8 playlists. Does not delete files.
 */
class SafLibraryScanner(
    private val context: Context,
    private val sourceStore: MusicSourceStore,
    private val libraryStore: LibraryStore
) {
    suspend fun scanSource(source: MusicSource): ScanOutcome = withContext(Dispatchers.IO) {
        val uriStr = source.treeUri ?: return@withContext ScanOutcome(emptyList(), emptyList(), "Missing folder URI")
        libraryStore.setScanning(true)
        libraryStore.setStatus("Scanning ${source.label}…")
        try {
            val treeUri = Uri.parse(uriStr)
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext ScanOutcome(emptyList(), emptyList(), "Cannot open folder", connected = false)
            val tracks = ArrayList<Track>(512)
            val playlists = ArrayList<DiscoveredPlaylistFile>(32)
            walk(root, root, "", source, tracks, playlists)
            if (!coroutineContext.isActive) {
                return@withContext ScanOutcome(tracks, playlists, "Scan cancelled")
            }
            sourceStore.recordScanResult(source.id, tracks, playlists, error = null, connected = true)
            libraryStore.setStatus("${tracks.size} tracks · ${playlists.size} playlists · ${source.label}")
            ScanOutcome(tracks, playlists)
        } catch (t: Throwable) {
            val msg = t.message ?: "scan failed"
            // Keep last good catalog — wiping to [] made static folders "disappear" after a bad rescan.
            sourceStore.recordScanFailure(source.id, msg, connected = false)
            libraryStore.setStatus("Scan failed: $msg (keeping previous library)")
            ScanOutcome(emptyList(), emptyList(), msg, connected = false)
        } finally {
            libraryStore.setScanning(false)
        }
    }

    private fun walk(
        root: DocumentFile,
        dir: DocumentFile,
        rel: String,
        source: MusicSource,
        tracks: MutableList<Track>,
        playlists: MutableList<DiscoveredPlaylistFile>
    ) {
        val children = dir.listFiles()
        val lyricsByBase = HashMap<String, Uri>(8)
        var folderCover: Uri? = null
        for (child in children) {
            val name = child.name ?: continue
            if (!child.isFile || name.startsWith(".")) continue
            if (LyricsLoader.isLyricsFile(name)) {
                val base = name.substringBeforeLast('.').lowercase(Locale.US)
                lyricsByBase[base] = child.uri
            } else if (folderCover == null && AlbumArtResolver.isCoverFileName(name)) {
                folderCover = child.uri
            }
        }
        for (child in children) {
            val name = child.name ?: continue
            if (name.startsWith(".")) continue
            val childRel = if (rel.isEmpty()) name else "$rel/$name"
            if (child.isDirectory) {
                walk(root, child, childRel, source, tracks, playlists)
            } else if (child.isFile) {
                when {
                    isAudio(name) -> {
                        val folderLabel = rel.substringAfterLast('/', missingDelimiterValue = root.name ?: source.label)
                            .ifBlank { root.name ?: source.label }
                        val (title, artist, album) = FilenameMetadataParser.resolve(
                            embeddedTitle = null,
                            embeddedArtist = null,
                            displayName = name,
                            folderFallback = folderLabel
                        )
                        val base = name.substringBeforeLast('.').lowercase(Locale.US)
                        val trackId = "saf:${source.id}:${child.uri}"
                        val art = folderCover
                            ?: AlbumArtResolver.extractEmbedded(context, child.uri, trackId)
                        tracks += Track(
                            id = trackId,
                            title = title,
                            artist = artist,
                            album = album,
                            uri = child.uri,
                            folder = folderLabel,
                            artworkUri = art,
                            sourceId = source.id,
                            sourceKind = source.kind,
                            relativePath = childRel.replace('\\', '/'),
                            lyricsUri = lyricsByBase[base],
                            available = true,
                            dateAddedMs = child.lastModified().coerceAtLeast(0L)
                        )
                    }
                    isPlaylist(name) -> {
                        playlists += DiscoveredPlaylistFile(
                            sourceId = source.id,
                            name = name.substringBeforeLast('.'),
                            uri = child.uri,
                            relativePath = childRel.replace('\\', '/')
                        )
                    }
                }
            }
        }
    }

    data class ScanOutcome(
        val tracks: List<Track>,
        val playlists: List<DiscoveredPlaylistFile>,
        val error: String? = null,
        val connected: Boolean = true
    )

    companion object {
        private val AUDIO_EXT = listOf(
            ".mp3", ".flac", ".m4a", ".aac", ".wav", ".ogg", ".opus", ".wma", ".aiff", ".alac"
        )

        fun isAudio(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val lower = name.lowercase(Locale.US)
            return AUDIO_EXT.any { lower.endsWith(it) }
        }

        fun isPlaylist(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val lower = name.lowercase(Locale.US)
            return lower.endsWith(".m3u") || lower.endsWith(".m3u8")
        }
    }
}
