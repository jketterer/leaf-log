package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class CompareWithBestUseCaseTest {
    private fun done(
        id: String,
        rating: Float?,
        temperature: Double = 80.0,
        daysAgo: Int = 0,
    ): SessionWithSteeps {
        val s = previewSession(id = id, ratings = listOf(rating), status = SessionStatus.COMPLETED, temperatureCelsius = temperature)
        return s.copy(steeps = s.steeps.map { it.copy(timestamp = it.timestamp - daysAgo.days) })
    }

    @Test
    fun `an unrated session has nothing to compare`() {
        assertNull(compareWithBest(done("a", null), listOf(done("b", 5f))))
    }

    @Test
    fun `a tea with no other rated session has nothing to compare`() {
        assertNull(compareWithBest(done("a", 4f), listOf(done("a", 4f), done("b", null))))
    }

    @Test
    fun `a session rated above every other is a new best`() {
        assertEquals(VsBest.NewBest, compareWithBest(done("a", 4.5f), listOf(done("b", 4f), done("c", 3f))))
    }

    @Test
    fun `only the rows that differ are listed`() {
        val result = compareWithBest(done("a", 3f, temperature = 95.0), listOf(done("b", 4f, temperature = 80.0)))

        assertEquals(listOf(BrewDifference.TEMPERATURE), assertIs<VsBest.Versus>(result).differences)
    }

    @Test
    fun `measurement noise is not a difference`() {
        val result = compareWithBest(done("a", 3f, temperature = 80.5), listOf(done("b", 4f)))

        assertEquals(emptyList(), assertIs<VsBest.Versus>(result).differences)
    }

    @Test
    fun `a tie for best goes to the most recent`() {
        val result = compareWithBest(done("a", 3f), listOf(done("old", 5f, daysAgo = 9), done("new", 5f, daysAgo = 1)))

        assertEquals("new", assertIs<VsBest.Versus>(result).best.session.id)
    }

    @Test
    fun `a session in progress is not a best`() {
        val running = previewSession(id = "r", ratings = listOf(5f))

        assertNull(compareWithBest(done("a", 3f), listOf(running)))
    }
}
