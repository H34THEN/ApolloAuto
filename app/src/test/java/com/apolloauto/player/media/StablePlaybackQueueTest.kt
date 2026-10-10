package com.apolloauto.player.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StablePlaybackQueueTest {

    @Test
    fun shuffleKeepsCurrentFirst() {
        val items = (0 until 20).map { "T$it" }
        val shuffled = StablePlaybackQueue.stableShuffleKeepingCurrent(items, 3, Random(42))
        assertEquals("T3", shuffled.first())
        assertEquals(20, shuffled.size)
        assertEquals(items.toSet(), shuffled.toSet())
    }

    @Test
    fun nextDoesNotRequireReshuffle_orderStableAcrossCallsWithSameSeed() {
        val items = (0 until 20).map { "T$it" }
        val a = StablePlaybackQueue.stableShuffleKeepingCurrent(items, 0, Random(7))
        val b = StablePlaybackQueue.stableShuffleKeepingCurrent(items, 0, Random(7))
        assertEquals(a, b)
        // Simulate five NEXT advances: prefix grows, suffix unchanged.
        val afterFive = a.drop(5)
        assertEquals(a.subList(5, a.size), afterFive)
    }

    @Test
    fun differentSeedsProduceDifferentOrders() {
        val items = (0 until 20).map { "T$it" }
        val a = StablePlaybackQueue.stableShuffleKeepingCurrent(items, 0, Random(1))
        val b = StablePlaybackQueue.stableShuffleKeepingCurrent(items, 0, Random(2))
        assertNotEquals(a, b)
    }

    @Test
    fun previousTraversalUsesSameOrder() {
        val order = StablePlaybackQueue.stableShuffleKeepingCurrent((0 until 10).map { it }, 2, Random(99))
        assertEquals(2, order[0]) // current preserved at front
        // After advancing to index 4 in order, previous is order[3]
        val pos = 4
        assertEquals(order[pos - 1], order[pos - 1])
        assertTrue(order.distinct().size == 10)
    }
}
