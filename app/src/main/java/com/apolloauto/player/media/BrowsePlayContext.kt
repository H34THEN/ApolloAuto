package com.apolloauto.player.media

import androidx.media3.common.MediaItem

/**
 * Remembers the last browsed list of **playable** media ids so a single-track tap
 * from Android Auto can expand into that collection (playlist/album/folder),
 * not just album-mates inferred from metadata.
 */
class BrowsePlayContext {
    @Volatile
    private var lastParentId: String? = null

    @Volatile
    private var lastIds: List<String> = emptyList()

    fun remember(parentId: String, children: List<MediaItem>) {
        val ids = children.mapNotNull { item ->
            if (item.mediaMetadata.isPlayable == true) item.mediaId else null
        }
        rememberIds(parentId, ids)
    }

    /** Testable / pure entry — stores playable ids for [parentId]. */
    fun rememberIds(parentId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        lastParentId = parentId
        lastIds = ids
    }

    /**
     * If [selectedId] appears in the last browsed playable list and that list has
     * more than one item, return sibling ids with the matching start index.
     */
    fun expandIdsFromBrowse(selectedId: String): Pair<List<String>, Int>? {
        val list = lastIds
        if (list.size <= 1) return null
        val index = list.indexOf(selectedId)
        if (index < 0) return null
        return list to index
    }

    fun debugParent(): String? = lastParentId
}
