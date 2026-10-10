package com.apolloauto.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayStat(
    val trackId: String,
    val playCount: Int,
    val lastPlayedMs: Long
)

class PlayHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<Map<String, PlayStat>> = _stats.asStateFlow()

    fun recordPlay(trackId: String) {
        if (trackId.isBlank()) return
        val now = System.currentTimeMillis()
        val map = _stats.value.toMutableMap()
        val prev = map[trackId]
        map[trackId] = PlayStat(
            trackId = trackId,
            playCount = (prev?.playCount ?: 0) + 1,
            lastPlayedMs = now
        )
        // Cap size
        if (map.size > 400) {
            val trim = map.values.sortedBy { it.lastPlayedMs }.take(map.size - 350).map { it.trackId }
            trim.forEach { map.remove(it) }
        }
        _stats.value = map
        persist(map)
    }

    fun recentlyPlayed(tracks: List<Track>, limit: Int = 40): List<Track> {
        val byId = tracks.associateBy { it.id }
        return _stats.value.values
            .sortedByDescending { it.lastPlayedMs }
            .mapNotNull { byId[it.trackId] }
            .take(limit)
    }

    fun mostPlayed(tracks: List<Track>, limit: Int = 40): List<Track> {
        val byId = tracks.associateBy { it.id }
        return _stats.value.values
            .sortedWith(compareByDescending<PlayStat> { it.playCount }.thenByDescending { it.lastPlayedMs })
            .mapNotNull { byId[it.trackId] }
            .take(limit)
    }

    private fun load(): Map<String, PlayStat> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.getString("id")
                    put(
                        id,
                        PlayStat(
                            trackId = id,
                            playCount = o.optInt("count", 1),
                            lastPlayedMs = o.optLong("last", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persist(map: Map<String, PlayStat>) {
        val arr = JSONArray()
        map.values.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.trackId)
                    .put("count", s.playCount)
                    .put("last", s.lastPlayedMs)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "apollo_play_history"
        private const val KEY = "stats_json"
    }
}
