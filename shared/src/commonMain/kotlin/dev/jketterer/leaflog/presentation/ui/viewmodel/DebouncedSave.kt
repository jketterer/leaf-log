package dev.jketterer.leaflog.presentation.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds a save back until edits pause, for a text field who's every keystroke would otherwise
 * write. Each [schedule] replaces the save still waiting. Runs on [scope], the app's rather than a
 * ViewModel's, so a save still lands after its screen is gone.
 */
class DebouncedSave(
    private val scope: CoroutineScope,
    private val pause: Duration = 500.milliseconds,
) {
    private var job: Job? = null

    @Volatile
    private var waiting: (suspend () -> Unit)? = null

    fun schedule(save: suspend () -> Unit) {
        waiting = save
        job?.cancel()
        job = scope.launch {
            delay(pause)
            runWaiting()
        }
    }

    /** Runs the waiting save now, or waits out one already running, so what follows sees it. */
    suspend fun flush() {
        if (waiting != null) {
            job?.cancel()
            runWaiting()
        } else {
            job?.join()
        }
    }

    /** Drops the waiting save, for an edit whose target is going away. */
    fun cancel() {
        waiting = null
        job?.cancel()
    }

    private suspend fun runWaiting() {
        val save = waiting ?: return
        waiting = null
        save()
    }
}
