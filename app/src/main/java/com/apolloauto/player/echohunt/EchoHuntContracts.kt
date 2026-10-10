package com.apolloauto.player.echohunt

import java.io.File

/**
 * Provider seams for Chromaprint → AcoustID → MusicBrainz.
 * Implementations must not embed secrets; keys come from [EchoHuntCredentials].
 */
interface ChromaprintEngine {
    suspend fun fingerprintPcm(
        pcm16leMono: ShortArray,
        sampleRateHz: Int,
        trackId: String,
        durationSec: Double
    ): FingerprintResult
}

interface AcoustIdClient {
    suspend fun lookup(fingerprint: FingerprintResult): List<AcoustIdCandidate>
}

interface MusicBrainzClient {
    suspend fun lookupRecording(recordingId: String): MusicBrainzRecording?
}

interface AudioPcmDecoder {
    /** Decode a local file/URI to mono PCM16 for fingerprinting. */
    suspend fun decodeToMonoPcm(pathOrUri: String): DecodedPcm
}

data class DecodedPcm(
    val samples: ShortArray,
    val sampleRateHz: Int,
    val durationSec: Double
)

/**
 * Credentials holder — values are read from app Settings / private prefs only.
 * Never hardcode API keys or passwords.
 */
interface EchoHuntCredentials {
    fun acoustIdApplicationKey(): String?
    fun musicBrainzUserAgent(): String
}

class DefaultEchoHuntCredentials(
    private val acoustIdKeyProvider: () -> String?,
    private val appVersion: String
) : EchoHuntCredentials {
    override fun acoustIdApplicationKey(): String? = acoustIdKeyProvider()?.trim()?.ifBlank { null }

    override fun musicBrainzUserAgent(): String =
        "ApolloAuto/$appVersion ( https://github.com/local/ApolloAuto ; contact via in-app settings )"
}

/**
 * Stub engine until native Chromaprint is packaged per-ABI.
 * Callers must treat [UnsupportedOperationException] as "unsupported on this build".
 */
class UnimplementedChromaprintEngine : ChromaprintEngine {
    override suspend fun fingerprintPcm(
        pcm16leMono: ShortArray,
        sampleRateHz: Int,
        trackId: String,
        durationSec: Double
    ): FingerprintResult {
        throw UnsupportedOperationException(
            "Native Chromaprint not packaged yet — do not ship fpcalc desktop binary in the APK"
        )
    }
}
