package com.apolloauto.player.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimer(
    private val scope: CoroutineScope,
    private val onExpire: () -> Unit
) {
    private var job: Job? = null
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    fun start(durationMs: Long) {
        cancel()
        if (durationMs <= 0L) return
        _remainingMs.value = durationMs
        job = scope.launch {
            var left = durationMs
            while (isActive && left > 0L) {
                delay(1000L)
                left -= 1000L
                _remainingMs.value = left.coerceAtLeast(0L)
            }
            if (isActive) {
                _remainingMs.value = 0L
                onExpire()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0L
    }

    fun isRunning(): Boolean = (_remainingMs.value > 0L)
}
