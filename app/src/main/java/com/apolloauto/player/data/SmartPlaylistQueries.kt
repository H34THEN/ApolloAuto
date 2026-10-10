package com.apolloauto.player.data

/**
 * Virtual smart lists (not persisted playlists).
 * Recently Added uses [Track.dateAddedMs] with optional first-seen fallback.
 * Unplayed uses [PlayHistoryStore] stats.
 */
object SmartPlaylistQueries {
    fun recentlyAdded(
        tracks: List<Track>,
        firstSeenMs: Map<String, Long> = emptyMap(),
        limit: Int = 40
    ): List<Track> {
        return tracks
            .map { t ->
                val ts = when {
                    t.dateAddedMs > 0L -> t.dateAddedMs
                    else -> firstSeenMs[t.id] ?: 0L
                }
                t to ts
            }
            .filter { it.second > 0L }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
            .ifEmpty {
                // Cold cache: show newest by title as a weak fallback so the tab isn't empty.
                tracks.sortedByDescending { it.id }.take(limit)
            }
    }

    fun unplayed(
        tracks: List<Track>,
        stats: Map<String, PlayStat>,
        limit: Int = 40
    ): List<Track> {
        return tracks
            .filter { t ->
                val s = stats[t.id]
                s == null || s.playCount <= 0
            }
            .sortedWith(
                compareByDescending<Track> { it.dateAddedMs }
                    .thenBy { it.displayTitle().lowercase() }
            )
            .take(limit)
    }
}
