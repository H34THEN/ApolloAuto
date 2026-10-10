package com.apolloauto.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class PlaylistOrigin {
    APOLLO,
    IMPORTED_M3U,
    SMART
}

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val locked: Boolean = false,
    val origin: PlaylistOrigin = PlaylistOrigin.APOLLO,
    val importUri: String? = null,
    /** Original M3U location lines parallel to [trackIds] when imported. */
    val entryRefs: List<String> = emptyList(),
    val unresolvedCount: Int = 0
)

class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _playlists = MutableStateFlow(normalize(load()))
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        ensureCarFavorites()
    }

    fun ensureCarFavorites(): Playlist {
        val existing = _playlists.value.find { it.id == CAR_FAVORITES_ID }
        if (existing != null) {
            if (!existing.locked || existing.name != CAR_FAVORITES_NAME) {
                val fixed = existing.copy(name = CAR_FAVORITES_NAME, locked = true)
                persist(_playlists.value.map { if (it.id == CAR_FAVORITES_ID) fixed else it })
                return fixed
            }
            return existing
        }
        val created = Playlist(
            id = CAR_FAVORITES_ID,
            name = CAR_FAVORITES_NAME,
            trackIds = emptyList(),
            locked = true
        )
        persist(listOf(created) + _playlists.value)
        return created
    }

    fun carFavorites(): Playlist = ensureCarFavorites()

    fun isInCarFavorites(trackId: String): Boolean =
        carFavorites().trackIds.contains(trackId)

    fun toggleCarFavorite(trackId: String): Boolean {
        return if (isInCarFavorites(trackId)) {
            removeTrack(CAR_FAVORITES_ID, trackId)
            false
        } else {
            addTrack(CAR_FAVORITES_ID, trackId)
            true
        }
    }

    fun contains(playlistId: String, trackId: String): Boolean =
        _playlists.value.find { it.id == playlistId }?.trackIds?.contains(trackId) == true

    fun create(name: String): Playlist {
        val playlist = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = name.ifBlank { "Deck ${_playlists.value.size}" },
            trackIds = emptyList(),
            locked = false,
            origin = PlaylistOrigin.APOLLO
        )
        persist(_playlists.value + playlist)
        return playlist
    }

    /**
     * Import M3U as a first-class playlist. Never overwrites Car Favorites.
     * Dedupes by importUri so rescans refresh instead of cloning.
     */
    fun createImported(
        name: String,
        trackIds: List<String>,
        entryRefs: List<String>,
        importUri: String?,
        unresolvedCount: Int
    ): Playlist {
        val existing = importUri?.let { uri ->
            _playlists.value.find { it.importUri == uri && it.origin == PlaylistOrigin.IMPORTED_M3U }
        }
        val playlist = Playlist(
            id = existing?.id ?: "m3u_${System.currentTimeMillis()}",
            name = name.ifBlank { "Imported" },
            trackIds = trackIds,
            locked = false,
            origin = PlaylistOrigin.IMPORTED_M3U,
            importUri = importUri,
            entryRefs = entryRefs,
            unresolvedCount = unresolvedCount
        )
        persist(
            if (existing != null) {
                _playlists.value.map { if (it.id == existing.id) playlist else it }
            } else {
                _playlists.value + playlist
            }
        )
        return playlist
    }

    fun rename(id: String, name: String) {
        if (id == CAR_FAVORITES_ID) return
        persist(_playlists.value.map {
            if (it.id == id) it.copy(name = name) else it
        })
    }

    fun delete(id: String) {
        if (id == CAR_FAVORITES_ID) return
        persist(_playlists.value.filterNot { it.id == id })
    }

    fun addTrack(playlistId: String, trackId: String) {
        if (trackId.isBlank()) return
        persist(_playlists.value.map { pl ->
            if (pl.id != playlistId || trackId in pl.trackIds) pl
            else pl.copy(trackIds = pl.trackIds + trackId)
        })
    }

    fun removeTrack(playlistId: String, trackId: String) {
        persist(_playlists.value.map { pl ->
            if (pl.id != playlistId) pl
            else pl.copy(trackIds = pl.trackIds.filterNot { it == trackId })
        })
    }

    fun toggleTrack(playlistId: String, trackId: String): Boolean {
        return if (contains(playlistId, trackId)) {
            removeTrack(playlistId, trackId)
            false
        } else {
            addTrack(playlistId, trackId)
            true
        }
    }

    fun resolve(playlist: Playlist, all: List<Track>): List<Track> {
        val map = all.associateBy { it.id }
        // Preserve order and intentional duplicates; skip unresolved placeholders.
        return playlist.trackIds.mapNotNull { id ->
            if (id.startsWith("unresolved:")) null else map[id]
        }
    }

    fun resolveById(playlistId: String, all: List<Track>): List<Track> {
        val pl = _playlists.value.find { it.id == playlistId } ?: return emptyList()
        return resolve(pl, all)
    }

    private fun normalize(list: List<Playlist>): List<Playlist> {
        val others = list.filterNot { it.id == CAR_FAVORITES_ID }
        val car = list.find { it.id == CAR_FAVORITES_ID }?.copy(
            name = CAR_FAVORITES_NAME,
            locked = true
        ) ?: Playlist(CAR_FAVORITES_ID, CAR_FAVORITES_NAME, emptyList(), locked = true)
        return listOf(car) + others
    }

    private fun persist(list: List<Playlist>) {
        val normalized = normalize(list)
        _playlists.value = normalized
        val arr = JSONArray()
        normalized.forEach { pl ->
            arr.put(
                JSONObject()
                    .put("id", pl.id)
                    .put("name", pl.name)
                    .put("locked", pl.locked)
                    .put("origin", pl.origin.name)
                    .put("importUri", pl.importUri)
                    .put("unresolvedCount", pl.unresolvedCount)
                    .put("tracks", JSONArray(pl.trackIds))
                    .put("entryRefs", JSONArray(pl.entryRefs))
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun load(): List<Playlist> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val tracks = obj.getJSONArray("tracks")
                    val ids = buildList {
                        for (t in 0 until tracks.length()) add(tracks.getString(t))
                    }
                    val refsArr = obj.optJSONArray("entryRefs")
                    val refs = buildList {
                        if (refsArr != null) {
                            for (r in 0 until refsArr.length()) add(refsArr.getString(r))
                        }
                    }
                    add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            trackIds = ids,
                            locked = obj.optBoolean("locked", obj.getString("id") == CAR_FAVORITES_ID),
                            origin = runCatching {
                                PlaylistOrigin.valueOf(obj.optString("origin", PlaylistOrigin.APOLLO.name))
                            }.getOrDefault(
                                if (obj.optString("importUri").isNotBlank()) PlaylistOrigin.IMPORTED_M3U
                                else PlaylistOrigin.APOLLO
                            ),
                            importUri = obj.optString("importUri")
                                .takeIf { it.isNotBlank() && it != "null" },
                            entryRefs = refs,
                            unresolvedCount = obj.optInt("unresolvedCount", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val CAR_FAVORITES_ID = "car_favorites"
        const val CAR_FAVORITES_NAME = "Car Favorites"
        private const val PREFS = "apollo_playlists"
        private const val KEY = "json"
    }
}
