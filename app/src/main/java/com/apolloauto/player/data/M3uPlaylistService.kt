package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedWriter
import java.io.OutputStreamWriter

data class M3uImportPreview(
    val title: String,
    val uri: Uri,
    val totalEntries: Int,
    val resolved: Int,
    val missing: Int,
    val ambiguous: Int,
    val result: M3uResolveResult,
    val encoding: String
)

class M3uPlaylistService(
    private val context: Context,
    private val playlistStore: PlaylistStore
) {
    fun preview(uri: Uri, catalog: List<Track>, playlistDir: String = ""): M3uImportPreview {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot read playlist")
        val parsed = M3uParser.parse(bytes, preferUtf8 = true)
        val resolved = M3uPathResolver.resolve(parsed, catalog, playlistDir)
        val title = displayName(uri)?.substringBeforeLast('.')?.ifBlank { null }
            ?: "Imported playlist"
        return M3uImportPreview(
            title = title,
            uri = uri,
            totalEntries = parsed.entries.size,
            resolved = resolved.resolvedCount,
            missing = resolved.missingCount,
            ambiguous = resolved.ambiguousCount,
            result = resolved,
            encoding = parsed.encodingName
        )
    }

    fun importConfirmed(preview: M3uImportPreview): Playlist {
        val trackIds = preview.result.entries.map { entry ->
            entry.trackId ?: entry.track?.id ?: "unresolved:${entry.original.location.hashCode()}"
        }
        val refs = preview.result.entries.map { it.original.location }
        return playlistStore.createImported(
            name = preview.title,
            trackIds = trackIds,
            entryRefs = refs,
            importUri = preview.uri.toString(),
            unresolvedCount = preview.missing + preview.ambiguous
        )
    }

    fun exportM3u8(playlist: Playlist, tracks: List<Track>, dest: Uri) {
        val map = tracks.associateBy { it.id }
        context.contentResolver.openOutputStream(dest, "wt")?.use { os ->
            BufferedWriter(OutputStreamWriter(os, Charsets.UTF_8)).use { w ->
                w.appendLine("#EXTM3U")
                w.appendLine("#PLAYLIST:${playlist.name}")
                playlist.trackIds.forEachIndexed { index, id ->
                    val track = map[id]
                    val ref = playlist.entryRefs.getOrNull(index)
                        ?: track?.relativePath?.ifBlank { null }
                        ?: track?.uri?.toString()
                        ?: return@forEachIndexed
                    val title = track?.let { "${it.artist} - ${it.title}" } ?: ref
                    val dur = track?.durationMs?.let { (it / 1000L).toInt() } ?: -1
                    w.appendLine("#EXTINF:$dur,$title")
                    w.appendLine(ref)
                }
            }
        } ?: error("Cannot write playlist")
    }

    private fun displayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        return uri.lastPathSegment
    }
}
