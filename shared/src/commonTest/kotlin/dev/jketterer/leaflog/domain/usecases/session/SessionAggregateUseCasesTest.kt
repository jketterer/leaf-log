package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionAggregateUseCasesTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val transactor = FakeTransactor()
    private val recomputeStats = RecomputeStatsUseCase(sessions, teas)

    private val startSession = StartSessionUseCase(transactor, sessions, teas, recomputeStats, Clock.System)
    private val addSteep = AddSteepUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val editSteep = EditSteepUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val editSession = EditSessionUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val deleteSteep = DeleteSteepUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val finishSession = FinishSessionUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val reopenSession = ReopenSessionUseCase(transactor, sessions, recomputeStats, Clock.System)
    private val deleteSession = DeleteSessionUseCase(transactor, sessions, recomputeStats)
    private val saveTimerState = SaveTimerStateUseCase(transactor, sessions, Clock.System)
    private val discardSteep = DiscardSteepUseCase(sessions, deleteSession, deleteSteep, finishSession)

    @Test
    fun `a new session is in progress and unrated and its first steep shares its id`() = runTest {
        val started = start()

        assertEquals(SessionStatus.IN_PROGRESS, started.session.status)
        assertEquals(listOf(started.session.id), started.steeps.map { it.id })
        assertNull(started.effectiveRating)
        assertEquals(started, sessions.getSession(started.session.id))
    }

    @Test
    fun `brew again repeats the set-up and first steep but not how it went`() = runTest {
        val source = start(location = "Desk")
        editSteep(source.session.id) { it.copy(rating = 5f, notes = "Lovely") }.getOrThrow()

        val again = startSession(sessions.getSession(source.session.id)!!).getOrThrow()

        assertEquals(source.session.copy(id = again.session.id, planSessionId = source.session.id), again.session)
        assertEquals(source.steeps.single().brewingTime, again.steeps.single().brewingTime)
        assertNull(again.steeps.single().rating)
        assertNull(again.steeps.single().notes)
    }

    @Test
    fun `brew again plans the next steeps from the session it copied`() = runTest {
        val source = start()
        val id = source.session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()
        addSteep(id, 30.seconds, 95.0, 100.0).getOrThrow()

        val again = startSession(sessions.getSession(id)!!).getOrThrow()
        val plan = ResolvePlanUseCase(sessions)(again)

        assertEquals(id, again.session.planSessionId)
        assertEquals(20.seconds, again.steeps.single().brewingTime)
        assertEquals(35.seconds, plan.timeOf(4))
    }

    @Test
    fun `brewing a finished tea puts it back in stock`() = runTest {
        teas.setFinished(TEA.id, finished = true)

        start()

        assertNull(tea().finishedAt)
    }

    @Test
    fun `a session with no vessel is refused and nothing is saved`() = runTest {
        val result = startSession(
            teaId = TEA.id,
            vesselId = " ",
            waterType = WaterType.FILTERED,
            brewingTime = 20.seconds,
            temperatureCelsius = 95.0,
            waterQuantityMl = 100.0,
        )

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(emptyList(), sessions.getSessionsByTea(TEA.id))
    }

    @Test
    fun `a steep added mid-brew comes next and the session stays in progress`() = runTest {
        val started = start()

        val updated = addSteep(started.session.id, 25.seconds, 95.0, 100.0).getOrThrow()

        assertEquals(listOf(1, 2), updated.steeps.map { it.number })
        assertEquals(SessionStatus.IN_PROGRESS, updated.session.status)
        assertEquals(updated, sessions.getSession(started.session.id))
    }

    @Test
    fun `a forgotten steep added to a finished session follows the last one`() = runTest {
        val started = start()
        finishSession(started.session.id).getOrThrow()

        val updated = addSteep(started.session.id, 25.seconds, 95.0, 100.0).getOrThrow()

        val first = started.steeps.single()
        assertEquals(first.timestamp + first.brewingTime, updated.currentSteep.timestamp)
        assertEquals(SessionStatus.COMPLETED, updated.session.status)
    }

    @Test
    fun `the next steep starts with the last one's tags unconfirmed`() = runTest {
        val id = start().session.id
        editSteep(id) {
            it.copy(tags = listOf(SteepTag("floral", confirmed = true), SteepTag("sweet", confirmed = false)))
        }.getOrThrow()

        val next = addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()

        assertEquals(
            listOf(SteepTag("floral", confirmed = false), SteepTag("sweet", confirmed = false)),
            next.currentSteep.tags,
        )
        assertEquals(next, sessions.getSession(id))
    }

    @Test
    fun `an edit changes only what it names and null clears`() = runTest {
        val started = start()
        editSteep(started.session.id) { it.copy(rating = 4f, notes = "First sip") }.getOrThrow()

        val updated = editSteep(started.session.id) { it.copy(notes = null) }.getOrThrow()

        assertEquals(4f, updated.steeps.single().rating)
        assertNull(updated.steeps.single().notes)
    }

    @Test
    fun `an edit can't renumber a steep or touch its timer`() = runTest {
        val started = start()
        val id = started.session.id

        val renumbered = editSteep(id) { it.copy(number = 2) }
        val timerTouched = editSteep(id) { it.copy(timerStatus = TimerStatus.RUNNING) }

        assertTrue(renumbered.exceptionOrNull() is IllegalArgumentException)
        assertTrue(timerTouched.exceptionOrNull() is IllegalArgumentException)
        assertEquals(started, sessions.getSession(id))
    }

    @Test
    fun `a session edit changes its set-up but not its status`() = runTest {
        val started = start()
        val id = started.session.id

        val updated = editSession(id) { it.copy(vesselId = "mug", location = "  ") }.getOrThrow()
        val finished = editSession(id) { it.copy(status = SessionStatus.COMPLETED) }

        assertEquals("mug", updated.session.vesselId)
        assertNull(updated.session.location)
        assertTrue(finished.exceptionOrNull() is IllegalArgumentException)
        assertEquals(SessionStatus.IN_PROGRESS, sessions.getSession(id)!!.session.status)
    }

    @Test
    fun `a session's note is its own apart from steep 1's`() = runTest {
        val started = start(notes = "Cosy afternoon")
        val id = started.session.id
        editSteep(id) { it.copy(notes = "Floral") }.getOrThrow()

        val updated = editSession(id) { it.copy(notes = "  ") }.getOrThrow()

        assertEquals("Cosy afternoon", started.session.notes)
        assertNull(started.steeps.single().notes)
        assertEquals("Floral", updated.steeps.single().notes)
        assertNull(updated.session.notes)
        assertEquals(updated, sessions.getSession(id))
    }

    @Test
    fun `moving a session's start shifts every steep and keeps the gaps between them`() = runTest {
        val id = start(status = SessionStatus.COMPLETED).session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()
        val before = sessions.getSession(id)!!
        val startedAt = before.startedAt - 3.days

        val moved = editSession(id, startedAt = startedAt) { it }.getOrThrow()

        assertEquals(startedAt, moved.startedAt)
        assertEquals(
            before.steeps.map { it.timestamp - before.startedAt },
            moved.steeps.map { it.timestamp - moved.startedAt },
        )
        assertEquals(startedAt, tea().lastBrewedAt)
    }

    @Test
    fun `moving a session to another tea recomputes both teas and clears its source`() = runTest {
        teas.upsert(OTHER_TEA)
        val source = start(status = SessionStatus.COMPLETED).session.id
        val id = startSession(
            teaId = TEA.id,
            vesselId = "gaiwan",
            waterType = WaterType.FILTERED,
            brewingTime = 20.seconds,
            temperatureCelsius = 95.0,
            waterQuantityMl = 100.0,
            status = SessionStatus.COMPLETED,
            planSessionId = source,
        ).getOrThrow().session.id

        val moved = editSession(id) { it.copy(teaId = OTHER_TEA.id) }.getOrThrow()

        assertEquals(OTHER_TEA.id, moved.session.teaId)
        assertNull(moved.session.planSessionId)
        assertEquals(1, tea().totalSessions)
        assertEquals(1, teas.getById(OTHER_TEA.id)!!.totalSessions)
    }

    @Test
    fun `deleting a middle steep renumbers the ones after it`() = runTest {
        val id = start().session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()
        val third = addSteep(id, 30.seconds, 95.0, 100.0).getOrThrow().currentSteep
        val second = sessions.getSession(id)!!.steeps[1]

        deleteSteep(second.id).getOrThrow()

        val steeps = sessions.getSession(id)!!.steeps
        assertEquals(listOf(id, third.id), steeps.map { it.id })
        assertEquals(listOf(1, 2), steeps.map { it.number })
    }

    @Test
    fun `steep 1 can only go with its session`() = runTest {
        val id = start().session.id

        assertTrue(deleteSteep(id).exceptionOrNull() is IllegalArgumentException)
        assertEquals(1, sessions.getSession(id)!!.steeps.size)
    }

    @Test
    fun `a finished session counts once for its tea however many steeps`() = runTest {
        val id = start().session.id
        editSteep(id) { it.copy(rating = 3f) }.getOrThrow()
        val second = addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow().currentSteep
        editSteep(second.id) { it.copy(rating = 5f) }.getOrThrow()

        val finished = finishSession(id).getOrThrow()

        assertEquals(1, tea().totalSessions)
        assertEquals(4f, tea().averageRating)
        assertEquals(finished.startedAt, tea().lastBrewedAt)
    }

    @Test
    fun `sessions still brewing stay out of the stats`() = runTest {
        start()

        assertEquals(0, tea().totalSessions)
        assertNull(tea().lastBrewedAt)
    }

    @Test
    fun `finishing twice changes nothing the second time`() = runTest {
        val id = start().session.id
        val finished = finishSession(id).getOrThrow()

        assertEquals(finished, finishSession(id).getOrThrow())
        assertEquals(1, tea().totalSessions)
    }

    @Test
    fun `reopening takes the session back out of the stats`() = runTest {
        val id = start().session.id
        finishSession(id).getOrThrow()

        val reopened = reopenSession(id).getOrThrow()

        assertEquals(SessionStatus.IN_PROGRESS, reopened.session.status)
        assertEquals(0, tea().totalSessions)
    }

    @Test
    fun `a deleted session is gone and no longer counted`() = runTest {
        val id = start(status = SessionStatus.COMPLETED).session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()

        deleteSession(id).getOrThrow()

        assertNull(sessions.getSession(id))
        assertEquals(0, tea().totalSessions)
    }

    @Test
    fun `the timer is saved on the steep it names`() = runTest {
        val id = start().session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()

        saveTimerState(
            TimerState(
                sessionId = id,
                steepNumber = 2,
                status = TimerStatus.RUNNING,
                totalDuration = 25.seconds,
                remainingDuration = 10.seconds,
            ),
        ).getOrThrow()

        val timed = sessions.getSession(id)!!.steeps
        assertNull(timed[0].timerStatus)
        assertEquals(TimerStatus.RUNNING, timed[1].timerStatus)
        assertEquals(10_000L, timed[1].timerRemainingMs)
    }

    @Test
    fun `a completed timer records how long the steep brewed and leaves the plan alone`() = runTest {
        val id = start().session.id
        val timer = TimerState(sessionId = id, totalDuration = 25.seconds, remainingDuration = 10.seconds)

        saveTimerState(timer.copy(status = TimerStatus.RUNNING)).getOrThrow()
        assertNull(sessions.getSession(id)!!.currentSteep.actualTime)

        saveTimerState(timer.copy(status = TimerStatus.COMPLETE, remainingDuration = 0.seconds)).getOrThrow()
        val steep = sessions.getSession(id)!!.currentSteep
        assertEquals(25.seconds, steep.actualTime)
        assertEquals(20.seconds, steep.brewingTime)
    }

    @Test
    fun `a brew time is recorded to the nearest second`() = runTest {
        val id = start().session.id
        val timer = TimerState(sessionId = id, totalDuration = 20.seconds, status = TimerStatus.COMPLETE)

        saveTimerState(timer.copy(remainingDuration = 13.6.seconds)).getOrThrow()
        assertEquals(6.seconds, sessions.getSession(id)!!.currentSteep.actualTime)

        saveTimerState(timer.copy(remainingDuration = 13.4.seconds)).getOrThrow()
        assertEquals(7.seconds, sessions.getSession(id)!!.currentSteep.actualTime)
    }

    @Test
    fun `already brewed leaves the actual time null so the planned time stands`() = runTest {
        val id = start().session.id
        val notStarted = TimerState(sessionId = id, totalDuration = 20.seconds, remainingDuration = 20.seconds)

        saveTimerState(notStarted.completedAt(EPOCH)).getOrThrow()

        val steep = sessions.getSession(id)!!.currentSteep
        assertNull(steep.actualTime)
        assertEquals(20.seconds, steep.brewedTime)
    }

    @Test
    fun `finish early records the time it brewed`() = runTest {
        val id = start().session.id
        val running = TimerState(
            sessionId = id,
            totalDuration = 20.seconds,
            remainingDuration = 20.seconds,
            status = TimerStatus.RUNNING,
            startedAt = EPOCH,
        )

        saveTimerState(running.completedAt(EPOCH + 12.seconds)).getOrThrow()

        assertEquals(12.seconds, sessions.getSession(id)!!.currentSteep.actualTime)
    }

    @Test
    fun `correcting a brew time changes the time it brewed whether timed or not`() = runTest {
        val untimed = start().session.id
        val timed = start().session.id
        val running = TimerState(
            sessionId = timed,
            totalDuration = 20.seconds,
            remainingDuration = 20.seconds,
            status = TimerStatus.RUNNING,
            startedAt = EPOCH,
        )
        saveTimerState(running.completedAt(EPOCH + 12.seconds)).getOrThrow()

        for (id in listOf(untimed, timed)) {
            val steep = editSteep(id) { it.withBrewedTime(15.seconds) }.getOrThrow().currentSteep
            assertEquals(15.seconds, steep.brewedTime)
        }
    }

    @Test
    fun `brew again and a forgotten steep follow the time it actually brewed`() = runTest {
        val source = start()
        val id = source.session.id
        saveTimerState(TimerState(sessionId = id, totalDuration = 35.seconds, status = TimerStatus.COMPLETE))
            .getOrThrow()
        finishSession(id).getOrThrow()
        val brewed = sessions.getSession(id)!!

        val again = startSession(brewed).getOrThrow()
        val forgotten = addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()

        assertEquals(35.seconds, again.steeps.single().brewingTime)
        assertEquals(source.steeps.single().timestamp + 35.seconds, forgotten.currentSteep.timestamp)
    }

    @Test
    fun `a timer for a steep the session doesn't have is refused`() = runTest {
        val id = start().session.id

        val result = saveTimerState(TimerState(sessionId = id, steepNumber = 2))

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `a session starts rinsed when asked and brew again keeps that`() = runTest {
        val started = start(rinsed = true)
        assertTrue(started.session.rinsed)

        val again = startSession(sessions.getSession(started.session.id)!!).getOrThrow()
        assertTrue(again.session.rinsed)
        assertFalse(start().session.rinsed)
    }

    @Test
    fun `a session's rinse can be edited`() = runTest {
        val id = start(status = SessionStatus.COMPLETED).session.id

        editSession(id) { it.copy(rinsed = true) }.getOrThrow()

        assertTrue(sessions.getSession(id)!!.session.rinsed)
    }

    @Test
    fun `discarding the first steep deletes the session`() = runTest {
        val id = start().session.id

        discardSteep(id).getOrThrow()

        assertNull(sessions.getSession(id))
    }

    @Test
    fun `discarding a later steep keeps the earlier ones and finishes the session`() = runTest {
        val id = start().session.id
        addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow()

        discardSteep(id).getOrThrow()

        val kept = sessions.getSession(id)!!
        assertEquals(listOf(1), kept.steeps.map { it.number })
        assertEquals(SessionStatus.COMPLETED, kept.session.status)
        assertEquals(1, tea().totalSessions)
    }

    private suspend fun start(
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        location: String? = null,
        notes: String? = null,
        rinsed: Boolean = false,
    ): SessionWithSteeps = startSession(
        teaId = TEA.id,
        vesselId = "gaiwan",
        waterType = WaterType.FILTERED,
        brewingTime = 20.seconds,
        temperatureCelsius = 95.0,
        waterQuantityMl = 100.0,
        teaQuantityGrams = 5f,
        location = location,
        notes = notes,
        status = status,
        rinsed = rinsed,
    ).getOrThrow()

    private suspend fun tea() = teas.getById(TEA.id)!!

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val TEA = Tea(
            id = "tea",
            name = "Tieguanyin",
            teaTypeId = "oolong",
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )

        val OTHER_TEA = TEA.copy(id = "other", name = "Da Hong Pao")
    }
}
