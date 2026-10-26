package dev.jketterer.leaflog.presentation.ui.viewmodel

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class DebouncedSaveTest {

    @Test
    fun `saves only the last of several quick edits`() = runTest {
        val saved = mutableListOf<String>()
        val save = DebouncedSave(this)

        listOf("e", "ea", "ear").forEach { text -> save.schedule { saved += text } }
        advanceUntilIdle()

        assertEquals(listOf("ear"), saved)
    }

    @Test
    fun `waits for edits to pause before saving`() = runTest {
        val saved = mutableListOf<String>()
        val save = DebouncedSave(this, pause = 500.milliseconds)

        save.schedule { saved += "earthy" }
        advanceTimeBy(400.milliseconds)
        runCurrent()

        assertEquals(emptyList(), saved)
    }

    @Test
    fun `flush saves the waiting edit at once and only once`() = runTest {
        val saved = mutableListOf<String>()
        val save = DebouncedSave(this)

        save.schedule { saved += "earthy" }
        save.flush()
        assertEquals(listOf("earthy"), saved)

        advanceUntilIdle()
        assertEquals(listOf("earthy"), saved)
    }

    @Test
    fun `flush with nothing waiting does nothing`() = runTest {
        DebouncedSave(this).flush()
    }

    @Test
    fun `cancel drops the waiting edit`() = runTest {
        val saved = mutableListOf<String>()
        val save = DebouncedSave(this)

        save.schedule { saved += "earthy" }
        save.cancel()
        advanceUntilIdle()

        assertEquals(emptyList(), saved)
    }
}
