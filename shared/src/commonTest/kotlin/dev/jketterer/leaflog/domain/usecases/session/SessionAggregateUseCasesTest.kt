package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TimerState
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.usecases.configuration.GenerateConfigurationLabelUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.SaveBrewingConfigurationUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionAggregateUseCasesTest {

    private val sessions = FakeTeaSessionRepository()
    private val teas = FakeTeaRepository(listOf(TEA))
    private val configurations = FakeBrewingConfigurationRepository(listOf(CONFIG))
    private val transactor = FakeTransactor()
    private val recomputeStats = RecomputeStatsUseCase(sessions, teas, configurations)

    private val startSession = StartSessionUseCase(transactor, sessions, teas, recomputeStats)
    private val addSteep = AddSteepUseCase(transactor, sessions, recomputeStats)
    private val editSteep = EditSteepUseCase(transactor, sessions, recomputeStats)
    private val editSession = EditSessionUseCase(transactor, sessions, recomputeStats)
    private val deleteSteep = DeleteSteepUseCase(transactor, sessions, recomputeStats)
    private val finishSession = FinishSessionUseCase(transactor, sessions, recomputeStats)
    private val reopenSession = ReopenSessionUseCase(transactor, sessions, recomputeStats)
    private val deleteSession = DeleteSessionUseCase(transactor, sessions, recomputeStats)
    private val saveTimerState = SaveTimerStateUseCase(transactor, sessions)
    private val discardSteep = DiscardSteepUseCase(sessions, deleteSession, deleteSteep, finishSession)
    private val saveConfiguration = SaveBrewingConfigurationUseCase(
        transactor,
        configurations,
        sessions,
        recomputeStats,
        GenerateConfigurationLabelUseCase(),
    )

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

        assertEquals(source.session.copy(id = again.session.id), again.session)
        assertEquals(source.steeps.single().brewingTime, again.steeps.single().brewingTime)
        assertNull(again.steeps.single().rating)
        assertNull(again.steeps.single().notes)
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
    fun `an edit changes only what it names and null clears`() = runTest {
        val started = start(notes = "First sip")
        editSteep(started.session.id) { it.copy(rating = 4f) }.getOrThrow()

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
    fun `a finished session counts once for its tea and method however many steeps`() = runTest {
        val id = startSession(CONFIG).getOrThrow().session.id
        editSteep(id) { it.copy(rating = 3f) }.getOrThrow()
        val second = addSteep(id, 25.seconds, 95.0, 100.0).getOrThrow().currentSteep
        editSteep(second.id) { it.copy(rating = 5f) }.getOrThrow()

        val finished = finishSession(id).getOrThrow()

        assertEquals(1, tea().totalSessions)
        assertEquals(4f, tea().averageRating)
        assertEquals(finished.startedAt, tea().lastBrewedAt)
        assertEquals(1, config().timesUsed)
        assertEquals(finished.startedAt, config().lastUsedAt)
    }

    @Test
    fun `sessions still brewing stay out of the stats`() = runTest {
        startSession(CONFIG).getOrThrow()

        assertEquals(0, tea().totalSessions)
        assertNull(tea().lastBrewedAt)
        assertEquals(0, config().timesUsed)
    }

    @Test
    fun `finishing twice changes nothing the second time`() = runTest {
        val id = startSession(CONFIG).getOrThrow().session.id
        val finished = finishSession(id).getOrThrow()

        assertEquals(finished, finishSession(id).getOrThrow())
        assertEquals(1, config().timesUsed)
    }

    @Test
    fun `reopening takes the session back out of the stats`() = runTest {
        val id = startSession(CONFIG).getOrThrow().session.id
        finishSession(id).getOrThrow()

        val reopened = reopenSession(id).getOrThrow()

        assertEquals(SessionStatus.IN_PROGRESS, reopened.session.status)
        assertEquals(0, tea().totalSessions)
        assertEquals(0, config().timesUsed)
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
    fun `already brewed leaves the actual time null so the planned time stands`() = runTest {
        val id = start().session.id
        val notStarted = TimerState(sessionId = id, totalDuration = 20.seconds, remainingDuration = 20.seconds)

        saveTimerState(notStarted.completedAt(EPOCH)).getOrThrow()

        val steep = sessions.getSession(id)!!.currentSteep
        assertNull(steep.actualTime)
        assertEquals(20.seconds, steep.brewedTime)
    }

    @Test
    fun `poured early records the time it brewed`() = runTest {
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
    fun `quick brew starts unrated and remembers the method`() = runTest {
        val started = startSession(CONFIG).getOrThrow()

        assertNull(started.effectiveRating)
        assertEquals(CONFIG.id, started.session.usedConfigurationId)
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

    @Test
    fun `a method saved from a session counts that session as its first use`() = runTest {
        val id = start(status = SessionStatus.COMPLETED).session.id
        editSteep(id) { it.copy(rating = 4f) }.getOrThrow()

        val saved = saveConfiguration(sessions.getSession(id)!!, customLabel = "Mine").getOrThrow()

        assertEquals(1, saved.timesUsed)
        assertEquals(4f, saved.rating)
        assertEquals(saved.id, sessions.getSession(id)!!.session.usedConfigurationId)
    }

    @Test
    fun `a method saved from a session that used another leaves that one's count alone`() = runTest {
        val id = startSession(CONFIG).getOrThrow().session.id
        editSteep(id) { it.copy(rating = 4f) }.getOrThrow()
        finishSession(id).getOrThrow()

        val saved = saveConfiguration(sessions.getSession(id)!!, customLabel = "Mine").getOrThrow()

        assertEquals(0, saved.timesUsed)
        assertEquals(1, config().timesUsed)
    }

    private suspend fun start(
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        location: String? = null,
        notes: String? = null,
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
    ).getOrThrow()

    private suspend fun tea() = teas.getById(TEA.id)!!
    private suspend fun config() = configurations.getById(CONFIG.id)!!

    private companion object {
        val EPOCH = Instant.fromEpochMilliseconds(0)

        val TEA = Tea(
            id = "tea",
            name = "Tieguanyin",
            teaTypeId = "oolong",
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )

        val CONFIG = BrewingConfiguration(
            id = "config",
            teaId = TEA.id,
            vesselId = "gaiwan",
            teaQuantityGrams = 5f,
            waterQuantityMl = 100.0,
            temperatureCelsius = 95.0,
            brewingTime = 20.seconds,
            waterType = WaterType.FILTERED,
            sourceSessionId = null,
            rating = null,
            timesUsed = 7,
            lastUsedAt = EPOCH,
            label = null,
            createdAt = EPOCH,
            updatedAt = EPOCH,
        )
    }
}
