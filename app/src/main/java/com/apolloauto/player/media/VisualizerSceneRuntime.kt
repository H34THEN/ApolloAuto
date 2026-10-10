package com.apolloauto.player.media

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent visualizer session diagnostics — scene identity must not recreate
 * on ordinary audio/UI updates. Logging is gated by [debugEnabled].
 */
class VisualizerSceneRuntime(
    private val nowMs: () -> Long = {
        runCatching { android.os.SystemClock.elapsedRealtime() }
            .getOrElse { System.currentTimeMillis() }
    }
) {
    @Volatile
    var debugEnabled: Boolean = false

    private var sceneCreatedAt = 0L
    private var renderStartedAt = 0L
    private var createCount = 0
    private var initCount = 0
    private var resetCount = 0
    private var modeChangeCount = 0
    private var autoDjTransitionCount = 0
    private var lastMode: VisualizerMode? = null
    private var lastChangeReason = "boot"

    private val _diag = MutableStateFlow(SceneDiagnostics())
    val diagnostics: StateFlow<SceneDiagnostics> = _diag.asStateFlow()

    fun onModeSelected(mode: VisualizerMode, reason: String) {
        val now = nowMs()
        if (lastMode == null) {
            createCount++
            initCount++
            sceneCreatedAt = now
            renderStartedAt = now
            log("SCENE_CREATE mode=$mode reason=$reason")
        } else if (lastMode != mode) {
            modeChangeCount++
            if (reason.startsWith("autodj")) autoDjTransitionCount++
            // Intentional scene change — keep createCount; bump init for new mode session.
            initCount++
            sceneCreatedAt = now
            log("SCENE_CHANGE from=$lastMode to=$mode reason=$reason")
        } else {
            // Same mode re-selected — do NOT count as reset/recreate.
            log("SCENE_RESELECT mode=$mode reason=$reason (no recreate)")
        }
        lastMode = mode
        lastChangeReason = reason
        publish(now)
    }

    /** Unexpected recreation (should stay near zero during long playback). */
    fun markUnexpectedReset(reason: String) {
        resetCount++
        log("SCENE_RESET_UNEXPECTED reason=$reason count=$resetCount")
        publish(nowMs())
    }

    fun ensureRenderClock() {
        if (renderStartedAt == 0L) {
            renderStartedAt = nowMs()
            publish(renderStartedAt)
        }
    }

    fun sceneUptimeMs(): Long {
        if (sceneCreatedAt == 0L) return 0L
        return nowMs() - sceneCreatedAt
    }

    fun renderUptimeMs(): Long {
        if (renderStartedAt == 0L) return 0L
        return nowMs() - renderStartedAt
    }

    private fun publish(now: Long) {
        _diag.value = SceneDiagnostics(
            activeMode = lastMode,
            createCount = createCount,
            initCount = initCount,
            unexpectedResetCount = resetCount,
            modeChangeCount = modeChangeCount,
            autoDjTransitionCount = autoDjTransitionCount,
            sceneUptimeMs = if (sceneCreatedAt == 0L) 0L else now - sceneCreatedAt,
            renderUptimeMs = if (renderStartedAt == 0L) 0L else now - renderStartedAt,
            lastChangeReason = lastChangeReason
        )
    }

    private fun log(msg: String) {
        if (debugEnabled) Log.d(TAG, msg)
    }

    companion object {
        private const val TAG = "ApolloVizScene"
    }
}

data class SceneDiagnostics(
    val activeMode: VisualizerMode? = null,
    val createCount: Int = 0,
    val initCount: Int = 0,
    val unexpectedResetCount: Int = 0,
    val modeChangeCount: Int = 0,
    val autoDjTransitionCount: Int = 0,
    val sceneUptimeMs: Long = 0L,
    val renderUptimeMs: Long = 0L,
    val lastChangeReason: String = ""
)
