package com.apolloauto.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMetadataFormatterTest {

    @Test
    fun identicalArtistAndAlbum_suppressesAlbum() {
        val lines = TrackMetadataFormatter.displayLines(
            "Ren - Money Game",
            "Carpe Dolor",
            "Carpe Dolor"
        )
        assertEquals(listOf("Ren - Money Game", "Carpe Dolor"), lines)
        assertNull(TrackMetadataFormatter.displaySecondaryMetadata("Carpe Dolor", "Carpe Dolor"))
    }

    @Test
    fun differentArtistAndAlbum_showsBoth() {
        val lines = TrackMetadataFormatter.displayLines("Track", "Artist A", "Album B")
        assertEquals(listOf("Track", "Artist A", "Album B"), lines)
    }

    @Test
    fun missingAlbum_artistOnly() {
        val lines = TrackMetadataFormatter.displayLines("Title", "Artist", "")
        assertEquals(listOf("Title", "Artist"), lines)
    }

    @Test
    fun missingArtist_usesUnknownOnce() {
        val lines = TrackMetadataFormatter.displayLines("Title", null, "Unknown Album")
        assertEquals(listOf("Title", TrackMetadataFormatter.UNKNOWN_ARTIST), lines)
    }

    @Test
    fun bothMissing_singleArtistFallback() {
        val lines = TrackMetadataFormatter.displayLines("Title", null, null)
        assertEquals(listOf("Title", TrackMetadataFormatter.UNKNOWN_ARTIST), lines)
    }

    @Test
    fun caseInsensitiveDuplicate() {
        assertNull(
            TrackMetadataFormatter.displaySecondaryMetadata("Carpe Dolor", "carpe dolor")
        )
    }

    @Test
    fun whitespaceOnlyDifferences_treatedAsDuplicate() {
        assertNull(
            TrackMetadataFormatter.displaySecondaryMetadata("  Carpe   Dolor ", "Carpe Dolor")
        )
    }

    @Test
    fun unicodeArtistPreserved() {
        assertEquals("坂本龍一", TrackMetadataFormatter.displayArtist("坂本龍一"))
        val lines = TrackMetadataFormatter.displayLines("Async", "坂本龍一", "BTTB")
        assertEquals(listOf("Async", "坂本龍一", "BTTB"), lines)
    }

    @Test
    fun longTitlePreserved() {
        val long = "A".repeat(200)
        assertEquals(long, TrackMetadataFormatter.displayTitle(long))
    }

    @Test
    fun storageValuesUnchangedByFormatter() {
        val title = "Ren - Money Game"
        val artist = "Carpe Dolor"
        val album = "Carpe Dolor"
        TrackMetadataFormatter.displayLines(title, artist, album)
        assertEquals("Ren - Money Game", title)
        assertEquals("Carpe Dolor", artist)
        assertEquals("Carpe Dolor", album)
        assertTrue(TrackMetadataFormatter.displayTitle(title).isNotEmpty())
    }
}
