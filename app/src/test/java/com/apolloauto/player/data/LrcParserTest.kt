package com.apolloauto.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesTimedLines() {
        val doc = LrcParser.parse(
            """
            [ti:Test]
            [00:12.00]First line
            [00:15.50]Second line
            [01:02.100]Third
            """.trimIndent()
        )
        assertEquals(3, doc.lines.size)
        assertEquals(12_000L, doc.lines[0].timeMs)
        assertEquals("First line", doc.lines[0].text)
        assertEquals(15_500L, doc.lines[1].timeMs)
        assertEquals(62_100L, doc.lines[2].timeMs)
    }

    @Test
    fun multiTagLineExpands() {
        val doc = LrcParser.parse("[00:10.00][00:20.00]Chorus")
        assertEquals(2, doc.lines.size)
        assertEquals("Chorus", doc.lines[0].text)
        assertEquals("Chorus", doc.lines[1].text)
        assertEquals(10_000L, doc.lines[0].timeMs)
        assertEquals(20_000L, doc.lines[1].timeMs)
    }

    @Test
    fun activeIndexTracksPosition() {
        val doc = LrcParser.parse(
            """
            [00:00.00]A
            [00:10.00]B
            [00:20.00]C
            """.trimIndent()
        )
        assertEquals(-1, doc.activeIndex(-1))
        assertEquals(0, doc.activeIndex(0))
        assertEquals(0, doc.activeIndex(9_999))
        assertEquals(1, doc.activeIndex(10_000))
        assertEquals(2, doc.activeIndex(25_000))
        assertTrue(doc.lines.isNotEmpty())
    }
}
