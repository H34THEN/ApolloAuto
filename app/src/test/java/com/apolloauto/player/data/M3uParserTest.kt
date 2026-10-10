package com.apolloauto.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesExtendedAndComments() {
        val text = """
            #EXTM3U
            # Comment
            #EXTINF:123,Artist - Title
            Album/Track.mp3

            #EXTINF:-1,Second
            ../Other/Song.flac
        """.trimIndent()
        val pl = M3uParser.parseText(text)
        assertTrue(pl.extended)
        assertEquals(2, pl.entries.size)
        assertEquals("Album/Track.mp3", pl.entries[0].location)
        assertEquals("Artist - Title", pl.entries[0].title)
        assertEquals(123, pl.entries[0].durationSec)
        assertEquals("../Other/Song.flac", pl.entries[1].location)
    }

    @Test
    fun resolvesRelativeAndWindowsPaths() {
        val catalog = listOf(
            item("a", "Music/Artist/Album/Track.mp3", "Track"),
            item("b", "Music/Artist/Other/Song.flac", "Song"),
            item("c", "Music/Dup/Track.mp3", "Track")
        )
        val pl = M3uParser.parseText(
            """
            #EXTM3U
            Artist/Album/Track.mp3
            D:\Music\Artist\Other\Song.flac
            Track.mp3
            Missing.mp3
            """.trimIndent()
        )
        val result = M3uPathResolver.resolveItems(pl, catalog, playlistRelativeDir = "Music")
        assertEquals(M3uResolveStatus.RESOLVED, result.entries[0].status)
        assertEquals("a", result.entries[0].trackId)
        assertEquals(M3uResolveStatus.RESOLVED, result.entries[1].status)
        assertEquals("b", result.entries[1].trackId)
        assertEquals(M3uResolveStatus.AMBIGUOUS, result.entries[2].status)
        assertEquals(M3uResolveStatus.MISSING, result.entries[3].status)
        assertEquals(1, result.missingCount)
        assertEquals(1, result.ambiguousCount)
    }

    @Test
    fun preservesDuplicateEntries() {
        val catalog = listOf(item("a", "x/one.mp3", "One"))
        val pl = M3uParser.parseText("x/one.mp3\nx/one.mp3")
        val result = M3uPathResolver.resolveItems(pl, catalog)
        assertEquals(2, result.entries.size)
        assertEquals("a", result.entries[0].trackId)
        assertEquals("a", result.entries[1].trackId)
    }

    private fun item(id: String, rel: String, title: String) =
        M3uCatalogItem(id = id, relativePath = rel, title = title, artist = "A")
}
