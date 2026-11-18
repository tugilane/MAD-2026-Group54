package ee.ut.cs.mad.strideset

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.repository.RoomRunRepository
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import ee.ut.cs.mad.strideset.running.RunCommand
import ee.ut.cs.mad.strideset.running.RunTracker
import ee.ut.cs.mad.strideset.running.RunTrackingState
import ee.ut.cs.mad.strideset.ui.running.RunViewModel
import ee.ut.cs.mad.strideset.ui.running.RunScreen
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RunViewModelTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: StrideSetDatabase
    private lateinit var repository: RoomRunRepository
    private val store = ViewModelStore()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            StrideSetDatabase::class.java).build()
        repository = RoomRunRepository(database.runDao())
    }

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
        database.close()
    }

    @Test fun aFinishInFlightIsReconciledAfterProcessRestoration() {
        val id = runBlocking { repository.startRun() }
        val handle = SavedStateHandle()
        lateinit var first: RunViewModel
        compose.runOnUiThread {
            first = RunViewModel(repository, RunTracker(), handle)
            store.put("first", first)
        }
        compose.waitUntil(5000) { !first.state.value.loading }
        compose.runOnUiThread { assertNotNull(first.beginCommand(RunCommand.FINISH)) }
        // This is the bundle captured when Home is pressed, before the service finishes saving.
        val capturedId = handle.get<Long>("completedRunId")
        assertEquals(id, capturedId)
        runBlocking { repository.finishRun(id) }
        lateinit var restored: RunViewModel
        compose.runOnUiThread {
            restored = RunViewModel(repository, RunTracker(), SavedStateHandle(mapOf("completedRunId" to capturedId)))
            store.put("restored", restored)
        }
        compose.waitUntil(5000) { !restored.state.value.loading }
        assertEquals(id, restored.state.value.run!!.id)
        assertTrue(restored.state.value.run!!.isFinished)
    }

    @Test fun aRestoredFinishStillShowsItsSummaryWhenTheServiceFinishesLater() {
        val id = runBlocking { repository.startRun() }
        val tracker = RunTracker()
        lateinit var model: RunViewModel
        compose.runOnUiThread {
            model = RunViewModel(repository, tracker, SavedStateHandle(mapOf("completedRunId" to id)))
            store.put("restored", model)
        }
        compose.waitUntil(5000) { !model.state.value.loading }
        assertFalse(model.state.value.run!!.isFinished)
        val finished = runBlocking { repository.finishRun(id) }
        compose.runOnUiThread {
            tracker.publish(RunTrackingState(finished))
            tracker.completeCommand("original-request", true)
        }
        compose.waitUntil(5000) { model.state.value.run?.isFinished == true }
        assertEquals(id, model.state.value.run!!.id)
        compose.runOnUiThread { model.leave() }
        compose.waitUntil(5000) { model.state.value.run == null }
    }

    @Test fun aLiveRunAcceptsPauseBeforeTheFirstDatabaseEmission() {
        val delayed = object : RunRepository by repository {
            override fun observeActiveRun() = flow<Run?> { awaitCancellation() }
        }
        lateinit var model: RunViewModel
        compose.runOnUiThread {
            val tracker = RunTracker().apply {
                publish(RunTrackingState(Run(1, Instant.EPOCH), serviceRunning = true))
            }
            model = RunViewModel(delayed, tracker, SavedStateHandle())
            store.put("live", model)
            assertFalse(model.state.value.loading)
            assertNotNull(model.beginCommand(RunCommand.PAUSE))
        }
    }

    @Test fun averagePaceRefreshesEveryFiveSecondsButPauseAndFinishAreImmediate() {
        val tracker = RunTracker()
        var run = Run(1, Instant.EPOCH, activeDuration = Duration.ofSeconds(30), distanceMeters = 100.0)
        lateinit var model: RunViewModel
        compose.runOnUiThread {
            tracker.publish(RunTrackingState(run, serviceRunning = true))
            model = RunViewModel(repository, tracker, SavedStateHandle())
            store.put("pace", model)
        }
        compose.setContent {
            val state by model.state.collectAsState()
            StrideSetTheme { RunScreen(state, DistanceUnit.KILOMETERS, {}, {}, {}) }
        }
        compose.onNodeWithText("5:00").assertIsDisplayed()
        compose.runOnIdle {
            run = run.copy(activeDuration = Duration.ofSeconds(31), distanceMeters = 110.0)
            tracker.publish(RunTrackingState(run, serviceRunning = true))
        }
        compose.onNodeWithText("00:00:31").assertIsDisplayed()
        compose.onNodeWithText("0.11").assertIsDisplayed()
        compose.onNodeWithText("5:00").assertIsDisplayed()
        compose.runOnIdle {
            run = run.copy(activeDuration = Duration.ofSeconds(35), distanceMeters = 120.0)
            tracker.publish(RunTrackingState(run, serviceRunning = true))
        }
        compose.onNodeWithText("4:52").assertIsDisplayed()
        compose.runOnIdle {
            run = run.copy(activeDuration = Duration.ofSeconds(36), distanceMeters = 125.0, isPaused = true)
            tracker.publish(RunTrackingState(run, serviceRunning = true))
        }
        compose.onNodeWithText("4:48").assertIsDisplayed()
        compose.runOnIdle {
            val request = checkNotNull(model.beginCommand(RunCommand.FINISH))
            run = run.copy(activeDuration = Duration.ofSeconds(38), distanceMeters = 135.0,
                isPaused = false, finishedAt = Instant.EPOCH.plusSeconds(38))
            tracker.publish(RunTrackingState(run))
            tracker.completeCommand(request, true)
        }
        compose.onNodeWithText("Run complete").assertIsDisplayed()
        compose.onNodeWithText("4:41").assertIsDisplayed()
    }
}
