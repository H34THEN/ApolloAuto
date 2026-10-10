package com.apolloauto.player.usb

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.apolloauto.player.data.LibraryStore
import com.apolloauto.player.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class UsbMusicScanner(
    private val context: Context,
    private val store: LibraryStore
) {
    suspend fun scanTree(treeUri: Uri): List<Track> = withContext(Dispatchers.IO) {
        store.setScanning(true)
        store.setStatus("Scanning USB SSD…")
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext emptyList()
            val found = ArrayList<Track>(512)
            walk(root, root.name ?: "USB", found)
            store.setTracks(found)
            store.setStatus("${found.size} tracks on USB")
            found
        } finally {
            store.setScanning(false)
        }
    }

    private fun walk(dir: DocumentFile, folderLabel: String, out: MutableList<Track>) {
        val children = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                val name = child.name ?: continue
                if (name.startsWith(".")) continue
                walk(child, name, out)
            } else if (child.isFile && isAudio(child.name)) {
                val name = child.name ?: continue
                val title = name.substringBeforeLast('.')
                out += Track(
                    id = child.uri.toString(),
                    title = title,
                    // Keep folder as album context; do not duplicate into artist (D3 metadata policy).
                    artist = "Unknown Artist",
                    album = folderLabel,
                    uri = child.uri,
                    folder = folderLabel
                )
            }
        }
    }

    private fun isAudio(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase(Locale.US)
        return AUDIO_EXT.any { lower.endsWith(it) }
    }

    companion object {
        private val AUDIO_EXT = listOf(
            ".mp3", ".flac", ".m4a", ".aac", ".wav", ".ogg", ".opus", ".wma", ".aiff", ".alac"
        )
    }
}
