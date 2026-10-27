package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.DailyStats
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDailyStatsUseCaseTest {

    // previewSession brews at PreviewInstant
    private val brewDay = PreviewInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val useCase = GetDailyStatsUseCase(
        FakeTeaSessionRepository(
            listOf(previewSession(status = SessionStatus.COMPLETED, waterQuantityMl = 150.0)),
        ),
    )

    @Test
    fun `a session counts toward the day it was brewed`() = runTest {
        assertEquals(
            DailyStats(sessionCount = 1, totalWaterMl = 150.0, differentTeasCount = 1),
            useCase(brewDay).first(),
        )
    }

    @Test
    fun `the same sessions count nothing on the next day`() = runTest {
        assertEquals(DailyStats(), useCase(brewDay.plus(1, DateTimeUnit.DAY)).first())
    }
}
