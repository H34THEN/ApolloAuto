package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LyricsLoader {
    suspend fun load(context: Context, track: Track?): LrcDocument? = withContext(Dispatchers.IO) {
        if (track == null) return@withContext null
        val uri = track.lyricsUri ?: findSidecar(context, track.uri) ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.let { LrcParser.parse(it).takeIf { doc -> doc.lines.isNotEmpty() } }
    }

    fun findSidecar(context: Context, audioUri: Uri?): Uri? {
        if (audioUri == null) return null
        val doc = DocumentFile.fromSingleUri(context, audioUri) ?: return null
        val name = doc.name ?: return null
        val base = name.substringBeforeLast('.', missingDelimiterValue = name)
        if (base.isBlank()) return null
        val parent = doc.parentFile ?: return null
        val target = "$base.lrc"
        return parent.listFiles().firstOrNull { child ->
            child.isFile && child.name.equals(target, ignoreCase = true)
        }?.uri
    }

    fun isLyricsFile(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name.lowercase(Locale.US).endsWith(".lrc")
    }
}
