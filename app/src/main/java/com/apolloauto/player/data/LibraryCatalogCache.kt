package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists per-source track catalogs so the library survives process death.
 * Sources alone are not enough — [MusicSourceStore] keeps tracks in memory unless cached here.
 */
class LibraryCatalogCache(context: Context) {
    private val dir = File(context.filesDir, "library_cache").also { it.mkdirs() }

    fun loadAll(sourceIds: Collection<String>): Map<String, List<Track>> {
        val out = LinkedHashMap<String, List<Track>>()
        for (id in sourceIds) {
            val tracks = load(id)
            if (tracks.isNotEmpty()) out[id] = tracks
        }
        return out
    }

    fun load(sourceId: String): List<Track> {
        val file = fileFor(sourceId)
        if (!file.isFile) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    parseTrack(arr.getJSONObject(i))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(sourceId: String, tracks: List<Track>) {
        val file = fileFor(sourceId)
        val tmp = File(file.parentFile, "${file.name}.tmp")
        runCatching {
            val arr = JSONArray()
            tracks.forEach { arr.put(toJson(it)) }
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }.onFailure {
            tmp.delete()
        }
    }

    fun remove(sourceId: String) {
        fileFor(sourceId).delete()
    }

    private fun fileFor(sourceId: String): File {
        val safe = sourceId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.json")
    }

    private fun toJson(t: Track): JSONObject = JSONObject()
        .put("id", t.id)
        .put("title", t.title)
        .put("artist", t.artist)
        .put("album", t.album)
        .put("uri", t.uri.toString())
        .put("folder", t.folder)
        .put("durationMs", t.durationMs)
        .put("artworkUri", t.artworkUri?.toString())
        .put("lyricsUri", t.lyricsUri?.toString())
        .put("albumId", t.albumId)
        .put("sourceId", t.sourceId)
        .put("sourceKind", t.sourceKind.name)
        .put("relativePath", t.relativePath)
        .put("available", t.available)

    private fun parseTrack(o: JSONObject): Track? = runCatching {
        val uriStr = o.getString("uri")
        Track(
            id = o.getString("id"),
            title = o.optString("title", "Unknown"),
            artist = o.optString("artist", "Unknown"),
            album = o.optString("album", ""),
            uri = Uri.parse(uriStr),
            folder = o.optString("folder", "Library"),
            durationMs = o.optLong("durationMs", 0L),
            artworkUri = o.optString("artworkUri").takeIf { it.isNotBlank() && it != "null" }?.let(Uri::parse),
            lyricsUri = o.optString("lyricsUri").takeIf { it.isNotBlank() && it != "null" }?.let(Uri::parse),
            albumId = o.optLong("albumId", -1L),
            sourceId = o.optString("sourceId", MusicSourceStore.PHONE_MEDIASTORE_ID),
            sourceKind = runCatching {
                MusicSourceKind.valueOf(o.optString("sourceKind", MusicSourceKind.PHONE_MEDIASTORE.name))
            }.getOrDefault(MusicSourceKind.PHONE_MEDIASTORE),
            relativePath = o.optString("relativePath", ""),
            available = o.optBoolean("available", true)
        )
    }.getOrNull()
}
