package com.apolloauto.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val locked: Boolean = false
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
            locked = false
        )
        persist(_playlists.value + playlist)
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
        return playlist.trackIds.mapNotNull { map[it] }
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
                    .put("tracks", JSONArray(pl.trackIds))
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
                    add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            trackIds = ids,
                            locked = obj.optBoolean("locked", obj.getString("id") == CAR_FAVORITES_ID)
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
