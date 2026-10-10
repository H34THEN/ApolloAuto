package com.apolloauto.player.media

import androidx.media3.common.MediaItem
import com.apolloauto.player.data.Track

/**
 * Android Auto / Media3 hide SEEK_TO_NEXT when the player timeline has only one item.
 * Expanding a single tapped track into a contextual playlist restores Next/Previous
 * and populates the host Queue screen.
 */
object MediaQueueExpand {
    data class Candidate(
        val id: String,
        val album: String,
        val artist: String,
        val folder: String
    )

    data class ExpandedIds(
        val ids: List<String>,
        val startIndex: Int
    )

    data class ExpandedQueue(
        val items: List<MediaItem>,
        val startIndex: Int
    )

    fun expandIds(selectedId: String, catalog: List<Candidate>): ExpandedIds {
        val track = catalog.find { it.id == selectedId }
            ?: return ExpandedIds(emptyList(), 0)

        fun from(list: List<Candidate>): ExpandedIds? {
            if (list.size <= 1) return null
            val index = list.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: return null
            return ExpandedIds(list.map { it.id }, index)
        }

        val album = track.album.trim()
        val artist = track.artist.trim()
        if (album.isNotBlank() && !album.equals("Unknown Album", ignoreCase = true)) {
            val albumMates = catalog.filter {
                it.album.equals(album, ignoreCase = true) &&
                    (artist.isBlank() ||
                        it.artist.equals(artist, ignoreCase = true) ||
                        it.artist.isBlank())
            }
            from(albumMates)?.let { return it }
        }

        val folder = track.folder.trim()
        if (folder.isNotBlank()) {
            from(catalog.filter { it.folder.equals(folder, ignoreCase = true) })?.let { return it }
        }

        if (artist.isNotBlank() && !artist.equals("Unknown", ignoreCase = true)) {
            from(catalog.filter { it.artist.equals(artist, ignoreCase = true) })?.let { return it }
        }

        from(catalog)?.let { return it }
        return ExpandedIds(listOf(track.id), 0)
    }

    fun expandSingle(
        selected: MediaItem,
        catalog: List<Track>,
        toPlayable: (Track) -> MediaItem,
        browseSiblings: List<MediaItem>? = null
    ): ExpandedQueue {
        // Prefer the last browsed collection (playlist/album folder) when available.
        if (browseSiblings != null && browseSiblings.size > 1) {
            val index = browseSiblings.indexOfFirst { it.mediaId == selected.mediaId }
            if (index >= 0) {
                return ExpandedQueue(browseSiblings, index)
            }
        }
        val byId = catalog.associateBy { it.id }
        val candidates = catalog.map {
            Candidate(it.id, it.album, it.artist, it.folder)
        }
        val expanded = expandIds(selected.mediaId, candidates)
        if (expanded.ids.isEmpty()) {
            return ExpandedQueue(listOf(selected), 0)
        }
        val items = expanded.ids.mapNotNull { id -> byId[id]?.let(toPlayable) }
        if (items.isEmpty()) return ExpandedQueue(listOf(selected), 0)
        val start = expanded.startIndex.coerceIn(0, items.lastIndex)
        return ExpandedQueue(items, start)
    }
}
