package com.apolloauto.player.echohunt

import com.apolloauto.player.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EchoHuntEngineStatus {
    READY_FOR_KEY,
    KEY_CONFIGURED,
    NATIVE_FINGERPRINT_MISSING,
    SCANNING,
    IDLE_WITH_RESULTS,
    ERROR
}

data class EchoHuntScanState(
    val status: EchoHuntEngineStatus = EchoHuntEngineStatus.READY_FOR_KEY,
    val enabled: Boolean = false,
    val queueTotal: Int = 0,
    val queueDone: Int = 0,
    val lastMessage: String = "Enter an AcoustID application API key to enable identification.",
    val lastError: String? = null,
    val recent: List<DiscoveryEntry> = emptyList()
)

/**
 * UI-facing scan coordinator. Chromaprint native is not packaged yet —
 * scans surface an honest status rather than fake matches.
 */
class EchoHuntScanController(
    private val credentials: EchoHuntCredentialStore,
    private val scope: CoroutineScope,
    private val chromaprint: ChromaprintEngine = UnimplementedChromaprintEngine(),
    private val acoustId: AcoustIdClient = StubAcoustIdClient(credentials)
) {
    private val _state = MutableStateFlow(EchoHuntScanState())
    val state: StateFlow<EchoHuntScanState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val key = credentials.acoustIdApplicationKey.value
        _state.value = _state.value.copy(
            status = when {
                key.isNullOrBlank() -> EchoHuntEngineStatus.READY_FOR_KEY
                else -> EchoHuntEngineStatus.KEY_CONFIGURED
            },
            lastMessage = when {
                key.isNullOrBlank() ->
                    "Enter an AcoustID application API key (acoustid.org/webservice)."
                else ->
                    "API key saved. Native Chromaprint fingerprinting is not packaged in this build yet."
            },
            lastError = null
        )
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        if (!enabled) cancel()
        refreshStatus()
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(
            status = if (credentials.acoustIdApplicationKey.value.isNullOrBlank()) {
                EchoHuntEngineStatus.READY_FOR_KEY
            } else {
                EchoHuntEngineStatus.KEY_CONFIGURED
            },
            lastMessage = "Scan cancelled."
        )
    }

    /**
     * Queue local library tracks for identification.
     * Until Chromaprint is packaged, reports NATIVE_FINGERPRINT_MISSING after a dry-run.
     */
    fun startLibraryScan(tracks: List<Track>) {
        if (!_state.value.enabled) {
            _state.value = _state.value.copy(
                lastMessage = "Enable ECHOHUNT scanning first.",
                lastError = null
            )
            return
        }
        val key = credentials.acoustIdApplicationKey.value
        if (key.isNullOrBlank()) {
            _state.value = _state.value.copy(
                status = EchoHuntEngineStatus.READY_FOR_KEY,
                lastMessage = "AcoustID application API key required.",
                lastError = "Missing API key"
            )
            return
        }
        if (tracks.isEmpty()) {
            _state.value = _state.value.copy(
                lastMessage = "No library tracks to scan. Add a phone music folder first.",
                lastError = null
            )
            return
        }
        job?.cancel()
        job = scope.launch {
            _state.value = _state.value.copy(
                status = EchoHuntEngineStatus.SCANNING,
                queueTotal = tracks.size,
                queueDone = 0,
                lastMessage = "Preparing fingerprint pipeline…",
                lastError = null
            )
            val sample = tracks.first()
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    chromaprint.fingerprintPcm(
                        pcm16leMono = ShortArray(0),
                        sampleRateHz = 44_100,
                        trackId = sample.id,
                        durationSec = (sample.durationMs / 1000.0).coerceAtLeast(1.0)
                    )
                }
            }
            val err = result.exceptionOrNull()
            if (err is UnsupportedOperationException) {
                _state.value = _state.value.copy(
                    status = EchoHuntEngineStatus.NATIVE_FINGERPRINT_MISSING,
                    queueDone = 0,
                    lastMessage =
                        "AcoustID key ready. Chromaprint native library is not in this APK yet — " +
                            "UI + credentials are wired; fingerprint engine lands next.",
                    lastError = err.message
                )
                return@launch
            }
            if (err != null) {
                _state.value = _state.value.copy(
                    status = EchoHuntEngineStatus.ERROR,
                    lastMessage = "Scan failed.",
                    lastError = err.message
                )
                return@launch
            }
            // Future: loop tracks → fingerprint → acoustId.lookup → discovery history
            _state.value = _state.value.copy(
                status = EchoHuntEngineStatus.IDLE_WITH_RESULTS,
                queueDone = tracks.size,
                lastMessage = "Scan complete."
            )
        }
    }
}

class StubAcoustIdClient(
    private val credentials: EchoHuntCredentialStore
) : AcoustIdClient {
    override suspend fun lookup(fingerprint: FingerprintResult): List<AcoustIdCandidate> {
        val key = credentials.acoustIdApplicationKey.value
            ?: throw IllegalStateException("AcoustID application key not configured")
        // Network client lands with Chromaprint packaging — keep seam only.
        throw UnsupportedOperationException(
            "AcoustID HTTP client pending (key length=${key.length})"
        )
    }
}
