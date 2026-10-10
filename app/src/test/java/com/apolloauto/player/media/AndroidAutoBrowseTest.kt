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
        assertEquals("Car Favorites", root.first().title)
        assertEquals("More", root.last().title)
        assertTrue(root.none { it.title.contains("(") })
        assertTrue(root.any { it.id == MediaIds.ALL })
        assertFalse(root.any { it.id == MediaIds.MOST_PLAYED })
    }

    @Test
    fun moreHoldsSecondaryCategories() {
        val more = AndroidAutoBrowse.moreNodes(12)
        assertTrue(more.any { it.id == MediaIds.MOST_PLAYED })
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
