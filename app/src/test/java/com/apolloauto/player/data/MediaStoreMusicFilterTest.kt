package com.apolloauto.player.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreMusicFilterTest {
    @Test
    fun keepsNormalMusicPaths() {
        assertTrue(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "Music/Signal/",
                bucketDisplayName = "Signal",
                displayName = "track.mp3",
                title = "Track",
                durationMs = 180_000L
            )
        )
        assertTrue(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "Download/",
                bucketDisplayName = "Download",
                displayName = "song.flac",
                title = "Song",
                durationMs = 240_000L
            )
        )
    }

    @Test
    fun dropsNothingOemGeneratedTones() {
        assertFalse(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "NTGenerateNotification/GenerateNotification/",
                bucketDisplayName = "GenerateNotification",
                displayName = "gen_noti_2_1.ogg",
                title = "gen_noti_2_1",
                durationMs = 1010L
            )
        )
        assertFalse(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "NTGenerateRingtone/GenerateRingtones/",
                bucketDisplayName = "GenerateRingtones",
                displayName = "gen_nothing_2_1.ogg",
                title = "gen_nothing_2_1",
                durationMs = 1595L
            )
        )
    }

    @Test
    fun dropsStandardAndroidToneFolders() {
        assertFalse(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "Ringtones/",
                bucketDisplayName = "Ringtones",
                displayName = "tone.ogg",
                title = "Tone",
                durationMs = 30_000L
            )
        )
        assertFalse(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "Notifications/",
                bucketDisplayName = "Notifications",
                displayName = "ping.ogg",
                title = "Ping",
                durationMs = 30_000L
            )
        )
    }

    @Test
    fun dropsVeryShortClipsEvenInMusicFolder() {
        assertFalse(
            MediaStoreMusicFilter.isLikelyMusic(
                relativePath = "Music/",
                bucketDisplayName = "Music",
                displayName = "blip.mp3",
                title = "blip",
                durationMs = 2_500L
            )
        )
    }
}
