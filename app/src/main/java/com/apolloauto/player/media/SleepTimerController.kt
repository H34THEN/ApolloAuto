package com.apolloauto.player.media

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepTimerMode {
    DURATION,
    END_OF_TRACK,
    END_OF_QUEUE
}

data class SleepTimerState(
    val running: Boolean = false,
    val remainingMs: Long = 0L,
    val mode: SleepTimerMode = SleepTimerMode.DURATION,
    val fadeOut: Boolean = true
)

/**
 * Service-lifecycle sleep timer. Must be started from Application/service scope,
 * not Compose, so it survives backgrounding and screen-off.
 */
class SleepTimerController(
    context: Context,
    private val scope: CoroutineScope,
    private val pausePlayback: () -> Unit,
    private val fadeAndPause: suspend (fadeMs: Long) -> Unit
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var job: Job? = null

    private val _state = MutableStateFlow(
        SleepTimerState(fadeOut = prefs.getBoolean(KEY_FADE, true))
    )
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun setFadeOut(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FADE, enabled).apply()
        _state.value = _state.value.copy(fadeOut = enabled)
    }

    fun startDuration(durationMs: Long) {
        cancel()
        if (durationMs <= 0L) return
        _state.value = _state.value.copy(
            running = true,
            remainingMs = durationMs,
            mode = SleepTimerMode.DURATION
        )
        job = scope.launch {
            var left = durationMs
            while (isActive && left > 0L) {
                delay(1000L)
                left -= 1000L
                _state.value = _state.value.copy(remainingMs = left.coerceAtLeast(0L))
            }
            if (isActive) expire()
        }
    }

    fun startEndOfTrack(trackRemainingMs: Long) {
        startDuration(trackRemainingMs.coerceAtLeast(1000L))
        _state.value = _state.value.copy(mode = SleepTimerMode.END_OF_TRACK)
    }

    fun startEndOfQueue(estimateMs: Long) {
        startDuration(estimateMs.coerceAtLeast(1000L))
        _state.value = _state.value.copy(mode = SleepTimerMode.END_OF_QUEUE)
    }

    fun extend(additionalMs: Long) {
        if (!_state.value.running) {
            startDuration(additionalMs)
            return
        }
        val next = _state.value.remainingMs + additionalMs
        startDuration(next)
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(running = false, remainingMs = 0L)
    }

    private suspend fun expire() {
        val fade = _state.value.fadeOut
        _state.value = _state.value.copy(running = false, remainingMs = 0L)
        if (fade) fadeAndPause(8_000L) else pausePlayback()
    }

    companion object {
        private const val PREFS = "apollo_sleep"
        private const val KEY_FADE = "fade"
        val PRESETS_MS = listOf(5, 10, 15, 30, 45, 60).map { it * 60_000L }
    }
}
