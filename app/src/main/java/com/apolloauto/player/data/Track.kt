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
    val albumId: Long = -1L
)
