package com.apolloauto.player.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

data class PersistedSession(
    val mediaIds: List<String>,
    val uris: List<String>,
    val titles: List<String>,
    val artists: List<String>,
    val albums: List<String>,
    val artworkUris: List<String?>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
    val visualizerMode: String,
    val driveMode: Boolean
) {
    fun toMediaItems(): List<MediaItem> =
        mediaIds.indices.map { i ->
            MediaItem.Builder()
                .setMediaId(mediaIds[i])
                .setUri(uris.getOrElse(i) { "" })
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(titles.getOrElse(i) { "" })
                        .setArtist(artists.getOrElse(i) { "" })
                        .setAlbumTitle(albums.getOrElse(i) { "" })
                        .setArtworkUri(artworkUris.getOrNull(i)?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
}

class PlaybackSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(session: PersistedSession) {
        if (session.mediaIds.isEmpty()) {
            prefs.edit().remove(KEY).apply()
            return
        }
        val obj = JSONObject()
            .put("index", session.index)
            .put("positionMs", session.positionMs)
            .put("shuffle", session.shuffle)
            .put("repeatMode", session.repeatMode)
            .put("visualizerMode", session.visualizerMode)
            .put("driveMode", session.driveMode)
            .put("ids", JSONArray(session.mediaIds))
            .put("uris", JSONArray(session.uris))
            .put("titles", JSONArray(session.titles))
            .put("artists", JSONArray(session.artists))
            .put("albums", JSONArray(session.albums))
            .put("art", JSONArray(session.artworkUris.map { it ?: "" }))
        prefs.edit().putString(KEY, obj.toString()).apply()
    }

    fun load(): PersistedSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            fun arr(name: String): List<String> {
                val a = obj.optJSONArray(name) ?: return emptyList()
                return buildList { for (i in 0 until a.length()) add(a.optString(i)) }
            }
            val ids = arr("ids")
            if (ids.isEmpty()) return null
            PersistedSession(
                mediaIds = ids,
                uris = arr("uris"),
                titles = arr("titles"),
                artists = arr("artists"),
                albums = arr("albums"),
                artworkUris = arr("art").map { it.ifBlank { null } },
                index = obj.optInt("index", 0),
                positionMs = obj.optLong("positionMs", 0L),
                shuffle = obj.optBoolean("shuffle", false),
                repeatMode = obj.optInt("repeatMode", 0),
                visualizerMode = obj.optString("visualizerMode", VisualizerMode.SPECTRUM.name),
                driveMode = obj.optBoolean("driveMode", false)
            )
        }.getOrNull()
    }

    companion object {
        private const val PREFS = "apollo_playback_session"
        private const val KEY = "session_json"
    }
}
