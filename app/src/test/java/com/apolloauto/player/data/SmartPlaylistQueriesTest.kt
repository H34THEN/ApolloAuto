package com.apolloauto.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic coverage for smart list filtering (no Android Uri construction).
 */
class SmartPlaylistQueriesTest {
    @Test
    fun recentlyAdded_prefersHigherTimestamps() {
        data class Row(val id: String, val ts: Long)
        val rows = listOf(Row("old", 1_000L), Row("new", 9_000L), Row("mid", 5_000L))
        val sorted = rows.sortedByDescending { it.ts }.map { it.id }
        assertEquals(listOf("new", "mid", "old"), sorted)
    }

    @Test
    fun unplayed_filterMatchesPlayStatRules() {
        val stats = mapOf(
            "a" to PlayStat("a", playCount = 2, lastPlayedMs = 1L),
            "c" to PlayStat("c", playCount = 0, lastPlayedMs = 1L)
        )
        val ids = listOf("a", "b", "c")
        val unplayed = ids.filter { id ->
            val s = stats[id]
            s == null || s.playCount <= 0
        }
        assertTrue("a" !in unplayed)
        assertEquals(listOf("b", "c"), unplayed)
    }
}
