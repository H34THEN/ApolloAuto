package com.apolloauto.player.media

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlin.random.Random

/**
 * Owns a stable playback order for the current queue session.
 *
 * Media3 [Player.shuffleModeEnabled] stays OFF. Shuffle ON physically reorders once;
 * [remix] reshuffles while keeping shuffle ON; NEXT never regenerates order.
 */
class StablePlaybackQueue {
    private var original: List<MediaItem> = emptyList()

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

    /**
     * Toggle shuffle mode only.
     * ON → shuffle once (current first). OFF → restore original order.
     * Does nothing if already in the requested state except when enabling from off.
     */
    fun toggleShuffle(
        currentItems: List<MediaItem>,
        currentIndex: Int
    ): QueueApply? {
        if (currentItems.isEmpty()) return null
        if (original.isEmpty()) original = currentItems.toList()
        val idx = currentIndex.coerceIn(0, currentItems.lastIndex)
        return if (!shuffleEnabled) {
            shuffleEnabled = true
            generation += 1
            buildShuffled(currentItems, idx, Random(generation))
        } else {
            shuffleEnabled = false
            val currentId = currentItems[idx].mediaId
            val restore = if (original.isNotEmpty()) original else currentItems
            val newIndex = restore.indexOfFirst { it.mediaId == currentId }.takeIf { it >= 0 } ?: 0
            QueueApply(restore, newIndex)
        }
    }

    /**
     * Reshuffle upcoming tracks while staying in shuffle mode.
     * If shuffle was off, turns it on.
     */
    fun remix(
        currentItems: List<MediaItem>,
        currentIndex: Int
    ): QueueApply? {
        if (currentItems.isEmpty()) return null
        if (original.isEmpty()) original = currentItems.toList()
        shuffleEnabled = true
        generation += 1
        val idx = currentIndex.coerceIn(0, currentItems.lastIndex)
        return buildShuffled(currentItems, idx, Random(generation))
    }

    fun syncFromPlayerShuffleFlag(
        playerWantsShuffle: Boolean,
        currentItems: List<MediaItem>,
        currentIndex: Int
    ): QueueApply? {
        if (playerWantsShuffle == shuffleEnabled) return null
        // Only auto-enable from remote; never auto-remix on every sync.
        return if (playerWantsShuffle && !shuffleEnabled) {
            toggleShuffle(currentItems, currentIndex)
        } else if (!playerWantsShuffle && shuffleEnabled) {
            toggleShuffle(currentItems, currentIndex)
        } else null
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
