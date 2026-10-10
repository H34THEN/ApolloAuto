package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoBrowseTest {
    @Test
    fun rootIsShallowAndStartsWithFavorites() {
        val root = AndroidAutoBrowse.rootNodes(
            favoritesMediaId = "playlist:car_favorites",
            favoritesCount = 3,
            playlistCount = 4,
            trackCount = 100
        )
        assertEquals(8, root.size)
        assertEquals("Favorites", root.first().title)
        assertEquals("More", root.last().title)
        assertEquals("Songs", root.first { it.id == MediaIds.ALL }.title)
        assertEquals("New", root.first { it.id == MediaIds.RECENTLY_ADDED }.title)
        assertEquals("Recent", root.first { it.id == MediaIds.RECENT }.title)
        assertTrue(root.none { it.title.contains("(") })
        assertFalse(root.any { it.id == MediaIds.MOST_PLAYED })
    }

    @Test
    fun moreHoldsSecondaryCategories() {
        val more = AndroidAutoBrowse.moreNodes(12)
        assertEquals("Top Played", more.first { it.id == MediaIds.MOST_PLAYED }.title)
        assertTrue(more.any { it.id == MediaIds.UNPLAYED })
        assertTrue(more.any { it.id == MediaIds.FOLDERS })
        assertTrue(more.any { it.id == MediaIds.VISUALIZERS })
    }

    @Test
    fun detectsAutomotivePackages() {
        assertTrue(AndroidAutoBrowse.isAutomotiveController("com.google.android.projection.gearhead"))
        assertFalse(AndroidAutoBrowse.isAutomotiveController("com.apolloauto.player"))
    }
}
