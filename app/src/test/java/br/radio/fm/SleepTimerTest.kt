package br.radio.fm

import br.radio.fm.player.SleepTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {
    @Test
    fun disparaSomenteAoFinal() = runTest {
        var done = false
        val timer = SleepTimer(this, { testScheduler.currentTime }) { done = true }
        timer.start(1)
        advanceTimeBy(59_000)
        runCurrent()
        assertFalse(done)
        assertNotNull(timer.remainingMs.value)
        advanceUntilIdle()
        assertTrue(done)
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun cancelarImpedeDesligamento() = runTest {
        var done = false
        val timer = SleepTimer(this, { testScheduler.currentTime }) { done = true }
        timer.start(5)
        advanceTimeBy(60_000)
        timer.cancel()
        advanceUntilIdle()
        assertFalse(done)
        assertNull(timer.remainingMs.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaTempoInvalido() = runTest {
        SleepTimer(this, { 0L }) { }.start(0)
    }
}
