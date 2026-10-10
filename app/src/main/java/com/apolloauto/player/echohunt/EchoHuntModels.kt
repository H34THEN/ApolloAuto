package com.apolloauto.player.echohunt

/**
 * ECHOHUNT discovery models — scaffold only (V5.1).
 * No network calls or credentials live here.
 */
data class FingerprintResult(
    val trackId: String,
    val durationSec: Double,
    val fingerprintBase64: String,
    val algorithm: String = "chromaprint"
)

data class AcoustIdCandidate(
    val recordingId: String?,
    val score: Double,
    val title: String?,
    val artist: String?,
    val release: String?
)

data class MusicBrainzRecording(
    val id: String,
    val title: String,
    val artistCredit: String,
    val releaseTitle: String?,
    val coverArtUrl: String?
)

data class DiscoveryEntry(
    val id: String,
    val localPathOrUri: String,
    val createdAtMs: Long,
    val status: DiscoveryStatus,
    val notes: String = "",
    val wishlist: Boolean = false,
    val fingerprintCached: Boolean = false,
    val candidates: List<AcoustIdCandidate> = emptyList()
)

enum class DiscoveryStatus {
    PENDING,
    FINGERPRINTED,
    MATCHED,
    UNKNOWN,
    ERROR,
    CANCELLED
}
