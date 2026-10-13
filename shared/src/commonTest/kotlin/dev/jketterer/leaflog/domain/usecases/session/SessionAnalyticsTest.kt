package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionAnalyticsTest {

    @Test
    fun `a gongfu session counts once and its water per steep and its leaf once`() = runTest {
        val data = analytics(gongfu(), western())

        assertEquals(2, data.totalSessions)
        assertEquals(4 * 100.0 + 300.0, data.totalWaterMl)
        assertEquals(5.0 + 3.0, data.totalTeaGrams)
        assertEquals(8.minutes, data.totalBrewingTime)
    }

    @Test
    fun `the average rating weighs each session once whatever its steep count`() = runTest {
        val data = analytics(gongfu(), western())

        // gongfu's steeps average 4, western's is 2
        assertEquals(3f, data.averageRating)
        assertEquals(2, data.ratedSessionsCount)
    }

    @Test
    fun `sessions still brewing are left out`() = runTest {
        val data = analytics(gongfu(status = SessionStatus.IN_PROGRESS), western())

        assertEquals(1, data.totalSessions)
        assertEquals(2f, data.averageRating)
    }

    @Test
    fun `a session belongs to the period it started in and with all its steeps`() = runTest {
        val sessions = FakeTeaSessionRepository(listOf(gongfu()))

        // Ends after steep 1 started but before steep 2 did
        val data = GetAnalyticsUseCase(sessions)(START, START + 30.seconds)

        assertEquals(1, data.totalSessions)
        assertEquals(400.0, data.totalWaterMl)
    }

    @Test
    fun `steeps per session are counted from the session's own steeps`() = runTest {
        val sessions = FakeTeaSessionRepository(listOf(gongfu(), western()))

        val insights = GetSteepInsightsUseCase(sessions, FakeTeaRepository())(START, END)

        assertEquals(2.5f, insights.averageSteepsPerSession)
    }

    private suspend fun analytics(vararg sessions: SessionWithSteeps) =
        GetAnalyticsUseCase(FakeTeaSessionRepository(sessions.toList()))(START, END)

    private fun gongfu(status: SessionStatus = SessionStatus.COMPLETED) = previewSession(
        id = "gongfu",
        ratings = listOf(3f, 5f, null, 4f),
        status = status,
        brewingTime = 1.minutes,
        waterQuantityMl = 100.0,
        teaQuantityGrams = 5f,
    )

    private fun western() = previewSession(
        id = "western",
        ratings = listOf(2f),
        status = SessionStatus.COMPLETED,
        brewingTime = 4.minutes,
        waterQuantityMl = 300.0,
        teaQuantityGrams = 3f,
    )

    private companion object {
        // previewSession starts every session here, its steeps one brewing time apart
        val START = Instant.fromEpochSeconds(1_790_000_000L)
        val END = START + 1.hours
    }
}
