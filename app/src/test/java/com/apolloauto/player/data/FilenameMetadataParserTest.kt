package com.apolloauto.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FilenameMetadataParserTest {
    @Test
    fun parsesArtistDashTitle() {
        val p = FilenameMetadataParser.parseBaseName("Ren - Money Game")
        assertNotNull(p)
        assertEquals("Ren", p!!.artist)
        assertEquals("Money Game", p.title)
    }

    @Test
    fun resolveWhenUnknownArtistUsesFilename() {
        val (title, artist, _) = FilenameMetadataParser.resolve(
            embeddedTitle = "Ren - Money Game",
            embeddedArtist = "<unknown>",
            displayName = "Ren - Money Game.mp3",
            folderFallback = "Downloads"
        )
        assertEquals("Money Game", title)
        assertEquals("Ren", artist)
    }

    @Test
    fun keepsRealEmbeddedArtist() {
        val (title, artist, _) = FilenameMetadataParser.resolve(
            embeddedTitle = "Money Game",
            embeddedArtist = "Ren",
            displayName = "01 Money Game.mp3",
            folderFallback = "Album"
        )
        assertEquals("Money Game", title)
        assertEquals("Ren", artist)
    }
}
