package ee.ut.cs.mad.strideset.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.repository.RoomRunRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRunRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "run-persistence-test.db"
    private val zone = ZoneId.of("Europe/Tallinn")
    private val clock = Clock.fixed(Instant.parse("2026-11-17T22:30:00Z"), zone)
    private lateinit var db: StrideSetDatabase
    private lateinit var repository: RoomRunRepository

    private fun open() {
        db = Room.databaseBuilder(context, StrideSetDatabase::class.java, name).build()
        repository = RoomRunRepository(db.runDao(), clock)
    }

    @Before fun setUp() { context.deleteDatabase(name); open() }
    @After fun tearDown() { db.close(); context.deleteDatabase(name) }

    @Test fun progressAndPauseSurviveReopening() = runBlocking {
        val id = repository.startRun()
        repository.updateProgress(id, 1234.5, Duration.ofSeconds(420))
        repository.pauseRun(id)
        db.close()
        open()
        val run = repository.observeActiveRun().first()!!
        assertEquals(id, run.id)
        assertEquals(1234.5, run.distanceMeters, 0.0)
        assertEquals(Duration.ofSeconds(420), run.activeDuration)
        assertTrue(run.isPaused)
        repository.resumeRun(id)
        assertFalse(repository.observeRun(id).first()!!.isPaused)
    }

    @Test fun onlyOneUnfinishedRunAndDiscardDeletesIt() = runBlocking {
        val id = repository.startRun()
        assertThrows(IllegalStateException::class.java) { runBlocking { repository.startRun() } }
        repository.pauseRun(id)
        assertThrows(IllegalStateException::class.java) { runBlocking { repository.startRun() } }
        repository.discardRun(id)
        assertNull(repository.observeRun(id).first())
        assertNotNull(repository.startRun())
    }

    @Test fun finishedRunsAreKeptAndQueriedByLocalStartDate() = runBlocking {
        val id = repository.startRun()
        repository.finishRun(id)
        assertNull(repository.observeActiveRun().first())
        assertTrue(repository.observeRun(id).first()!!.isFinished)
        val day = LocalDate.of(2026, 11, 18)
        assertEquals(listOf(id), repository.observeFinishedRuns(day, day).first().map { it.id })
        assertTrue(repository.observeFinishedRuns(day.minusDays(1), day.minusDays(1)).first().isEmpty())
        repository.startRun()
        assertEquals(1, repository.observeFinishedRuns(day, day).first().size)
    }

    @Test fun invalidProgressCannotCorruptStoredTotals() = runBlocking {
        val id = repository.startRun()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateProgress(id, Double.NaN, Duration.ZERO) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateProgress(id, -1.0, Duration.ZERO) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateProgress(id, 0.0, Duration.ofSeconds(-1)) }
        }
        assertEquals(0.0, repository.observeRun(id).first()!!.distanceMeters, 0.0)
    }
}
