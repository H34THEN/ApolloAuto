package com.apolloauto.player.companion

import kotlin.random.Random

/**
 * Dry, dark-humor deck personality. Short lines only — never chatty, never clingy.
 */
object CompanionVoice {
    private val boot = listOf(
        "Deck online. Try not to crash the vibe.",
        "Systems green. Morale: optional.",
        "Apollo listening. Pretend I'm not judging the playlist.",
        "Boot complete. Your secrets are safe. Mostly.",
        "HUD awake. Traffic is the enemy. Bass is the friend."
    )

    private val skip = listOf(
        "Next. Taste is subjective. Allegedly.",
        "Skipped. The void approves.",
        "Forward. No eulogy for that track.",
        "Next up. Hope it's less… that."
    )

    private val favorite = listOf(
        "Pinned to Car Favorites. Bold choice.",
        "Saved. Future-you can regret this later.",
        "Car Favorites updated. The road remembers."
    )

    private val driveOn = listOf(
        "Drive Mode. Hands on wheel. Eyes on neon.",
        "Drive Mode engaged. I'll keep the theatrics down.",
        "Cockpit locked. Don't make me nag about safety."
    )

    private val driveOff = listOf(
        "Drive Mode off. Chaos is back on the menu.",
        "Full chrome restored. Tasteful recklessness enabled."
    )

    private val usbIn = listOf(
        "USB detected. Scanning like I get paid for it.",
        "External vault online. Don't unplug mid-drop."
    )

    private val usbOut = listOf(
        "USB gone. Ghost tracks incoming.",
        "Drive ejected. Hope you meant to do that."
    )

    private val idle = listOf(
        "Standing by. The silence is doing numbers.",
        "Queue empty enough to hear your poor decisions.",
        "Still here. Still dry. Still ready."
    )

    private val viz = listOf(
        "Visuals remixed. Don't stare too long.",
        "New paint on the void.",
        "Scene change. Blink if you're still mortal."
    )

    fun bootLine(seed: Int = Random.nextInt()): String = boot.pick(seed)
    fun skipLine(seed: Int = Random.nextInt()): String = skip.pick(seed)
    fun favoriteLine(seed: Int = Random.nextInt()): String = favorite.pick(seed)
    fun driveOnLine(seed: Int = Random.nextInt()): String = driveOn.pick(seed)
    fun driveOffLine(seed: Int = Random.nextInt()): String = driveOff.pick(seed)
    fun usbInLine(seed: Int = Random.nextInt()): String = usbIn.pick(seed)
    fun usbOutLine(seed: Int = Random.nextInt()): String = usbOut.pick(seed)
    fun idleLine(seed: Int = Random.nextInt()): String = idle.pick(seed)
    fun vizLine(seed: Int = Random.nextInt()): String = viz.pick(seed)

    fun nowPlayingLine(title: String, artist: String): String {
        val t = title.ifBlank { "Untitled signal" }
        val a = artist.ifBlank { "Unknown operator" }
        return when (Random.nextInt(3)) {
            0 -> "Now spinning: $t — $a. Try to keep up."
            1 -> "$a · $t. Volume diplomacy recommended."
            else -> "Locked on $t. $a would be proud. Or not."
        }
    }

    private fun List<String>.pick(seed: Int): String =
        this[((seed % size) + size) % size]
}
