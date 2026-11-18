package ee.ut.cs.mad.strideset

import android.Manifest
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.AppContainer
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.repository.RoomRunRepository
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import ee.ut.cs.mad.strideset.running.LocationFix
import ee.ut.cs.mad.strideset.running.LocationSource
import ee.ut.cs.mad.strideset.running.RunTracker
import ee.ut.cs.mad.strideset.running.RunTrackingService
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RunFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val app get() = context.applicationContext as StrideSetApplication
    private lateinit var original: AppContainer
    private lateinit var database: StrideSetDatabase
    private lateinit var repository: RoomRunRepository
    private val tracker = RunTracker()
    private val source = ManualGps()
    private var failFinish = false

    @Before fun setUp() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("cmd location set-location-enabled true")
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS).forEach { shell("pm grant ${context.packageName} $it") }
        original = app.container
        database = Room.inMemoryDatabaseBuilder(context, StrideSetDatabase::class.java).build()
        repository = RoomRunRepository(database.runDao())
        app.container = object : AppContainer by original {
            override val runRepository = object : RunRepository by repository {
                override suspend fun finishRun(runId: Long): Run {
                    if (failFinish) throw IOException("Simulated full disk")
                    return repository.finishRun(runId)
                }
            }
            override val runTracker = tracker
            override fun createRunLocationSource(): LocationSource = source
        }
    }

    @After fun tearDown() {
        context.stopService(Intent(context, RunTrackingService::class.java))
        compose.waitUntil(10_000) { !tracker.state.value.serviceRunning }
        compose.activityRule.scenario.close()
        app.container = original
        database.close()
    }

    @Test fun startPauseResumeFinishShowsTheSavedSummary() {
        openRun()
        compose.onNodeWithText("Ready to run").assertIsDisplayed()
        compose.onNodeWithText("Start run").performClick()
        awaitTracking()
        val id = tracker.state.value.run!!.id
        compose.runOnUiThread { source.emit(0.0, 0.0) }
        compose.waitUntil(5000) { tracker.state.value.run!!.activeDuration.seconds >= 2 }
        compose.runOnUiThread { source.emit(0.0, 0.0001) }
        compose.onNodeWithText("0.01").assertIsDisplayed()
        compose.onNodeWithText("Pause").performClick()
        compose.waitUntil(5000) { tracker.state.value.run!!.isPaused }
        compose.onNodeWithText("Resume").performClick()
        awaitTracking()
        compose.onNodeWithText("Finish run").performClick()
        compose.onNodeWithText("Save run").performClick()
        compose.onNodeWithText("Run complete").assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRun(id).first()!! }.isFinished)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Run complete").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        openRun()
        compose.onNodeWithText("Ready to run").assertIsDisplayed()
    }

    @Test fun discardRequiresConfirmationAndCancelKeepsTracking() {
        openRun()
        compose.onNodeWithText("Start run").performClick()
        awaitTracking()
        compose.onNodeWithText("Discard run").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(source.active)
        assertNotNull(runBlocking { repository.observeActiveRun().first() })
        compose.onNodeWithText("Discard run").performClick()
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
        assertNull(runBlocking { repository.observeActiveRun().first() })
        assertFalse(source.active)
    }

    @Test fun recoveredRunRemainsPausedUntilExplicitResume() {
        val id = runBlocking {
            repository.startRun().also { repository.updateProgress(it, 1000.0, Duration.ofSeconds(300)) }
        }
        openRun()
        compose.onNodeWithText("Run paused").assertIsDisplayed()
        compose.onNodeWithText("1.00").assertIsDisplayed()
        assertEquals(0, source.starts)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        assertEquals(0, source.starts)
        compose.onNodeWithText("Resume").performClick()
        awaitTracking()
        assertEquals(id, tracker.state.value.run!!.id)
        assertEquals(1, source.starts)
    }

    @Test fun failedFinishKeepsTheRunAndCanBeRetried() {
        openRun()
        compose.onNodeWithText("Start run").performClick()
        awaitTracking()
        failFinish = true
        compose.onNodeWithText("Finish run").performClick()
        compose.onNodeWithText("Save run").performClick()
        compose.waitUntil(5000) { tracker.state.value.error != null }
        compose.onNodeWithText("Run complete").assertDoesNotExist()
        assertNotNull(runBlocking { repository.observeActiveRun().first() })
        failFinish = false
        compose.onNodeWithText("Finish run").performClick()
        compose.onNodeWithText("Save run").performClick()
        compose.onNodeWithText("Run complete").assertIsDisplayed()
    }

    @Test fun backOffersAnExplicitBackgroundExit() {
        openRun()
        compose.onNodeWithText("Start run").performClick()
        awaitTracking()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Leave this run?").assertIsDisplayed()
        compose.onNodeWithText("Continue run").performClick()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Keep tracking in background").performClick()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
        assertTrue(source.active)
        openRun()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        assertEquals(1, source.starts)
    }

    @Test fun disabledGpsCanBeEnabledAndRetriedWithoutLosingTheScreen() {
        // Some Google emulator images put a location-off warning over the tested activity.
        // Wait for that asynchronous window, rather than racing it after location is disabled.
        try {
            instrumentation.uiAutomation.executeAndWaitForEvent(
                { shell("cmd location set-location-enabled false") },
                { it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    it.packageName?.toString() == "com.google.android.gms" },
                5000,
            ).recycle()
        } catch (_: TimeoutException) {
            // Devices without Google Play services have no location-off warning.
        }
        if (instrumentation.uiAutomation.rootInActiveWindow?.packageName == "com.google.android.gms") {
            shell("input keyevent KEYCODE_BACK")
            instrumentation.uiAutomation.waitForIdle(500, 5000)
        }
        openRun()
        compose.onNodeWithText("Start run").performClick()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
        assertNull(runBlocking { repository.observeActiveRun().first() })
        shell("cmd location set-location-enabled true")
        compose.onNodeWithText("Start run").performClick()
        awaitTracking()
        compose.onNodeWithText("Pause").assertIsDisplayed()
    }

    private fun openRun() { compose.onNodeWithText("Start run").performClick() }
    private fun awaitTracking() {
        compose.waitUntil(10_000) { source.active && tracker.state.value.run?.isPaused == false }
        compose.waitForIdle()
    }
    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use {
            ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() }
        }
    }
    private class ManualGps : LocationSource {
        @Volatile var active = false
        var starts = 0
        private var callback: ((LocationFix) -> Unit)? = null
        override fun start(onFix: (LocationFix) -> Unit, onUnavailable: () -> Unit) {
            starts++; active = true; callback = onFix
        }
        override fun stop() { active = false; callback = null }
        fun emit(lat: Double, lon: Double) {
            callback?.invoke(LocationFix(lat, lon, 3.0, SystemClock.elapsedRealtime()))
        }
    }
}
