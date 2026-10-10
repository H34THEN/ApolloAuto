package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowsePlayContextTest {
    @Test
    fun expandUsesLastBrowsedPlaylistOrder() {
        val ctx = BrowsePlayContext()
        ctx.rememberIds("playlist:sci-fi", listOf("t1", "t2", "t3", "t4"))
        val expanded = ctx.expandIdsFromBrowse("t3")
        assertEquals(listOf("t1", "t2", "t3", "t4"), expanded!!.first)
        assertEquals(2, expanded.second)
    }

    @Test
    fun ignoreUnknownSelection() {
        val ctx = BrowsePlayContext()
        ctx.rememberIds("playlist:x", listOf("a", "b"))
        assertNull(ctx.expandIdsFromBrowse("z"))
    }

    @Test
    fun ignoreSingleItemBrowse() {
        val ctx = BrowsePlayContext()
        ctx.rememberIds("album:solo", listOf("only"))
        assertNull(ctx.expandIdsFromBrowse("only"))
    }
}
