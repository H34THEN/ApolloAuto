package com.apolloauto.player.media

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlin.random.Random

/**
 * Owns a stable playback order for the current queue session.
 *
 * Media3 [Player.shuffleModeEnabled] is kept OFF. When the user enables shuffle we physically
 * reorder the playlist once (current track preserved) so NEXT never regenerates order and the
 * MediaItem timeline matches the UI queue (phone + Android Auto share the same ExoPlayer).
 */
class StablePlaybackQueue {
    /** Unshuffled collection order for the session. */
    private var original: List<MediaItem> = emptyList()

    /** Whether the active playlist on the player is a shuffle projection. */
    var shuffleEnabled: Boolean = false
        private set

    private var generation: Long = 0L

    val shuffleGenerationId: Long get() = generation

    fun replaceQueue(items: List<MediaItem>, startIndex: Int): QueueApply {
        original = items.toList()
        if (items.isEmpty()) {
            shuffleEnabled = false
            return QueueApply(emptyList(), 0)
        }
        val start = startIndex.coerceIn(0, items.lastIndex)
        return if (shuffleEnabled) {
            generation += 1
            buildShuffled(items, start, Random(generation))
        } else {
            QueueApply(items, start)
        }
    }

    fun toggleShuffle(
        currentItems: List<MediaItem>,
        currentIndex: Int
    ): QueueApply? {
        if (currentItems.isEmpty()) return null
        if (original.isEmpty()) {
            original = currentItems.toList()
        }
        val idx = currentIndex.coerceIn(0, currentItems.lastIndex)
        return if (!shuffleEnabled) {
            shuffleEnabled = true
            generation += 1
            // Shuffle the currently loaded items (preserves prior edits), keep current first.
            buildShuffled(currentItems, idx, Random(generation))
        } else {
            shuffleEnabled = false
            // Restore original order; keep current track.
            val currentId = currentItems[idx].mediaId
            val restore = if (original.isNotEmpty()) original else currentItems
            val newIndex = restore.indexOfFirst { it.mediaId == currentId }
                .takeIf { it >= 0 } ?: 0
            QueueApply(restore, newIndex)
        }
    }

    fun syncFromPlayerShuffleFlag(playerWantsShuffle: Boolean, currentItems: List<MediaItem>, currentIndex: Int): QueueApply? {
        if (playerWantsShuffle == shuffleEnabled) return null
        return toggleShuffle(currentItems, currentIndex)
    }

    fun snapshot(player: Player): List<QueueEntry> {
        val count = player.mediaItemCount
        if (count <= 0) return emptyList()
        val current = player.currentMediaItemIndex.coerceAtLeast(0)
        return (0 until count).map { i ->
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata
            QueueEntry(
                mediaId = item.mediaId.orEmpty(),
                title = meta.title?.toString().orEmpty(),
                artist = meta.artist?.toString().orEmpty(),
                album = meta.albumTitle?.toString().orEmpty(),
                artworkUri = meta.artworkUri,
                isCurrent = i == current
            )
        }
    }

    private fun buildShuffled(items: List<MediaItem>, currentIndex: Int, random: Random): QueueApply {
        val ordered = stableShuffleKeepingCurrent(items, currentIndex, random)
        return QueueApply(ordered, 0)
    }

    data class QueueApply(
        val items: List<MediaItem>,
        val startIndex: Int
    )

    companion object {
        /** Pure helper: current item first, remaining shuffled once. */
        fun <T> stableShuffleKeepingCurrent(items: List<T>, currentIndex: Int, random: Random): List<T> {
            if (items.isEmpty()) return emptyList()
            val idx = currentIndex.coerceIn(0, items.lastIndex)
            val current = items[idx]
            val rest = items.filterIndexed { i, _ -> i != idx }.shuffled(random)
            return listOf(current) + rest
        }
    }
}

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: android.net.Uri?,
    val isCurrent: Boolean
)
