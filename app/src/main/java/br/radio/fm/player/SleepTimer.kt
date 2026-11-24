package br.radio.fm.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Temporizador para dormir. O relógio é injetável para facilitar os testes. */
class SleepTimer(
    private val scope: CoroutineScope,
    private val now: () -> Long = { SystemClock.elapsedRealtime() },
    private val onFinish: () -> Unit,
) {
    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()
    private var job: Job? = null

    fun start(minutes: Int) {
        require(minutes > 0) { "O tempo deve ser maior que zero" }
        cancel()
        val end = now() + minutes * 60_000L
        job = scope.launch {
            while (true) {
                val left = end - now()
                if (left <= 0) break
                _remainingMs.value = left
                delay(minOf(1_000L, left))
            }
            _remainingMs.value = null
            onFinish()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
    }
}
