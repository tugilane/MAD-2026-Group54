package ee.ut.cs.mad.strideset.running

import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeRunRepository : RunRepository {
    val active = MutableStateFlow<Run?>(null)
    var saved: Run? = null
    var failSave = false
    var writes = 0
    private var nextId = 1L
    override fun observeActiveRun() = active
    override fun observeRun(id: Long) = active.map { run -> (run ?: saved)?.takeIf { it.id == id } }
    override suspend fun startRun(): Long {
        check(active.value == null)
        val run = Run(id = nextId++, startedAt = Instant.EPOCH)
        active.value = run
        return run.id
    }
    override suspend fun updateProgress(runId: Long, distanceMeters: Double, activeDuration: Duration) {
        if (failSave) error("Disk full")
        writes++
        active.value = active.value!!.copy(distanceMeters = distanceMeters, activeDuration = activeDuration)
    }
    override suspend fun pauseRun(runId: Long) { active.value = active.value!!.copy(isPaused = true) }
    override suspend fun resumeRun(runId: Long) { active.value = active.value!!.copy(isPaused = false) }
    override suspend fun finishRun(runId: Long): Run {
        if (failSave) error("Disk full")
        saved = active.value!!.copy(finishedAt = Instant.EPOCH.plusSeconds(100), isPaused = false)
        active.value = null
        return checkNotNull(saved)
    }
    override suspend fun discardRun(runId: Long) { active.value = null }
    override fun observeFinishedRuns(start: LocalDate, endInclusive: LocalDate) = MutableStateFlow(listOfNotNull(saved))
}

class RunSessionTest {
    private var now = 0L
    private val repository = FakeRunRepository()
    private val source = FakeLocationSource()
    private val state = RunTracker()
    private fun session() = RunSession(repository, source, state) { now }

    @Test fun duplicateStartsDoNotResetTotalsOrSubscribeTwice() = runBlocking {
        val session = session()
        session.start()
        now = 8000
        session.start()
        assertEquals(1, source.starts)
        assertEquals(Duration.ofSeconds(8), state.state.value.run!!.activeDuration)
    }

    @Test fun checkpointsEveryFiveSecondsAndPauseStopsSource() = runBlocking {
        val session = session()
        session.start()
        now = 4000
        session.tick()
        assertEquals(0, repository.writes)
        now = 5000
        session.tick()
        assertEquals(1, repository.writes)
        session.pause()
        assertTrue(repository.active.value!!.isPaused)
        assertEquals(1, source.stops)
        now = 20_000
        session.tick()
        assertEquals(Duration.ofSeconds(5), state.state.value.run!!.activeDuration)
        session.close()
        assertFalse(state.state.value.serviceRunning)
    }

    @Test fun sourceFailurePausesAndPublishesAnActionableError() = runBlocking {
        val session = session()
        session.start()
        source.disable()
        now = 1000
        session.tick()
        assertTrue(state.state.value.run!!.isPaused)
        assertNotNull(state.state.value.error)
    }

    @Test fun resumeIsIdempotentAndDoesNotIncludePausedMovement() = runBlocking {
        val session = session()
        session.start()
        source.emit(LocationFix(0.0, 0.0, 3.0, now))
        now = 10_000
        session.pause()
        session.pause()
        now = 100_000
        session.resume()
        session.resume()
        source.emit(LocationFix(0.0, 0.01, 3.0, now))
        now += 10_000
        source.emit(LocationFix(0.0, 0.0105, 3.0, now))
        session.tick()
        assertEquals(2, source.starts)
        assertEquals(1, source.stops)
        assertEquals(Duration.ofSeconds(20), state.state.value.run!!.activeDuration)
        assertEquals(55.6, state.state.value.run!!.distanceMeters, 0.2)
    }

    @Test fun anExistingCheckpointIsAttachedPausedUntilExplicitResume() = runBlocking {
        repository.startRun()
        repository.updateProgress(1, 100.0, Duration.ofSeconds(30))
        now = 1_000_000
        val session = session()
        session.start()
        assertEquals(0, source.starts)
        assertTrue(state.state.value.run!!.isPaused)
        session.resume()
        now += 2000
        session.tick()
        assertEquals(Duration.ofSeconds(32), state.state.value.run!!.activeDuration)
    }

    @Test fun finishFlushesFinalProgressAndStopsTheSource() = runBlocking {
        val session = session()
        session.start()
        now = 2500
        session.finish()
        assertEquals(Duration.ofMillis(2500), repository.saved!!.activeDuration)
        assertTrue(state.state.value.run!!.isFinished)
        assertFalse(state.state.value.serviceRunning)
        assertEquals(1, source.stops)
        session.close()
        assertEquals(1, source.stops)
    }

    @Test fun discardStopsTrackingEvenWhenProgressCannotBeSaved() = runBlocking {
        val session = session()
        session.start()
        repository.failSave = true
        session.discard()
        assertNull(repository.active.value)
        assertNull(state.state.value.run)
        assertEquals(1, source.stops)
    }

    @Test fun checkpointFailureRetainsPausedTotalsAndAllowsRetry() = runBlocking {
        val session = session()
        session.start()
        now = 5000
        repository.failSave = true
        try {
            session.tick()
            fail("Expected persistence failure")
        } catch (error: IllegalStateException) {
            session.fail("Could not save progress. Try again.")
        }
        now = 50_000
        assertEquals(Duration.ofSeconds(5), state.state.value.run!!.activeDuration)
        assertTrue(state.state.value.run!!.isPaused)
        assertNotNull(state.state.value.error)
        assertEquals(1, source.stops)
        repository.failSave = false
        session.resume()
        now += 1000
        session.finish()
        assertEquals(Duration.ofSeconds(6), repository.saved!!.activeDuration)
    }

    @Test fun closeStopsCallbacksAndPublishesPausedState() = runBlocking {
        val session = session()
        session.start()
        now = 2000
        session.close()
        source.emit(LocationFix(0.0, 1.0, 3.0, now))
        now = 20_000
        assertTrue(state.state.value.run!!.isPaused)
        assertEquals(Duration.ofSeconds(2), state.state.value.run!!.activeDuration)
        assertFalse(state.state.value.serviceRunning)
        assertEquals(1, source.stops)
    }

    @Test fun finishDoesNotDependOnASeparateReadAfterCommitting() = runBlocking {
        val flakyReads = object : RunRepository by repository {
            override fun observeRun(id: Long) = flow {
                if (repository.saved != null) throw IOException("Read unavailable after committed save")
                emit(repository.active.value)
            }
        }
        val session = RunSession(flakyReads, source, state) { now }
        session.start()
        now = 3000
        session.finish()
        assertTrue(state.state.value.run!!.isFinished)
        assertFalse(state.state.value.serviceRunning)
        assertEquals(Duration.ofSeconds(3), repository.saved!!.activeDuration)
    }
}
