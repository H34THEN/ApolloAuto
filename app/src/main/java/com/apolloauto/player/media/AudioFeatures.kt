package com.apolloauto.player.media

enum class CaptureSource {
    PLAYBACK_SESSION,
    DECORATIVE
}

fun CaptureSource.shortLabel(): String = when (this) {
    CaptureSource.PLAYBACK_SESSION -> "LIVE SESSION"
    CaptureSource.DECORATIVE -> "DECORATIVE IDLE"
}

data class AudioFeatures(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f,
    val rms: Float = 0f,
    val onset: Float = 0f,
    /** 1 = fully silent / idle, 0 = loud */
    val silence: Float = 1f
)
