package com.apolloauto.player.data

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records when Apollo first observed each track id (SAF/USB “recently added”). */
class TrackFirstSeenStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _map = MutableStateFlow(load())
    val map: StateFlow<Map<String, Long>> = _map.asStateFlow()

    fun noteSeen(trackIds: Collection<String>) {
        if (trackIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val next = _map.value.toMutableMap()
        var changed = false
        for (id in trackIds) {
            if (id.isBlank() || id in next) continue
            next[id] = now
            changed = true
        }
        if (!changed) return
        // Cap map size
        if (next.size > 2500) {
            val trim = next.entries.sortedBy { it.value }.take(next.size - 2000).map { it.key }
            trim.forEach { next.remove(it) }
        }
        _map.value = next
        persist(next)
    }

    private fun load(): Map<String, Long> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, o.optLong(k, 0L))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persist(map: Map<String, Long>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(KEY, o.toString()).apply()
    }

    companion object {
        private const val PREFS = "apollo_track_first_seen"
        private const val KEY = "first_seen_json"
    }
}
