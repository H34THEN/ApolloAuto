package com.apolloauto.player.data

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val folder: String,
    val durationMs: Long = 0L,
    val artworkUri: Uri? = null,
    /** Sidecar `.lrc` URI when discovered next to the audio file. */
    val lyricsUri: Uri? = null,
    val albumId: Long = -1L,
    val sourceId: String = MusicSourceStore.PHONE_MEDIASTORE_ID,
    val sourceKind: MusicSourceKind = MusicSourceKind.PHONE_MEDIASTORE,
    /** Path relative to the SAF root (forward slashes), used for M3U resolution. */
    val relativePath: String = "",
    val available: Boolean = true
)
