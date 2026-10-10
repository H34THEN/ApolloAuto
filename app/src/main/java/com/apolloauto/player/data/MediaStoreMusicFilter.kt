package com.apolloauto.player.data

/**
 * OEM MediaStore often tags ringtones/notifications as [MediaStore.Audio.Media.IS_MUSIC]
 * while leaving IS_RINGTONE / IS_NOTIFICATION at 0 (seen on Nothing / NTGenerate*).
 */
object MediaStoreMusicFilter {
    /** Shorter than this is almost never a song; OEM tones are typically 1–5s. */
    const val MIN_DURATION_MS: Long = 12_000L

    private val excludedPathTokens = listOf(
        "ringtone",
        "ringtones",
        "notification",
        "notifications",
        "alarm",
        "alarms",
        "ntgenerateringtone",
        "ntgeneratenotification",
        "generateringtones",
        "generatenotification",
        "ui/audio",
        "ui_sounds",
        "uisounds",
        "system/media/audio",
        "samsung/music/over_the_horizon",
        "ringtones/",
        "notifications/",
        "alarms/"
    )

    fun isLikelyMusic(
        relativePath: String?,
        bucketDisplayName: String?,
        displayName: String?,
        title: String?,
        durationMs: Long
    ): Boolean {
        if (durationMs in 1 until MIN_DURATION_MS) return false
        val haystack = listOf(relativePath, bucketDisplayName, displayName, title)
            .filterNotNull()
            .joinToString("/")
            .lowercase()
        if (haystack.isBlank()) return durationMs == 0L || durationMs >= MIN_DURATION_MS
        return excludedPathTokens.none { token -> haystack.contains(token) }
    }
}
