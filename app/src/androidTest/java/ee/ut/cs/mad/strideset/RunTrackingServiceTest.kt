package ee.ut.cs.mad.strideset

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.AppContainer
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.repository.RoomRunRepository
import ee.ut.cs.mad.strideset.running.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RunTrackingServiceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val app get() = context.applicationContext as StrideSetApplication
    private lateinit var original: AppContainer
    private lateinit var db: StrideSetDatabase
    private val tracker = RunTracker()
    private val source = ServiceLocationSource()
    private lateinit var repository: RoomRunRepository
    private val notifications get() = context.getSystemService(NotificationManager::class.java)

    @Before fun setUp() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("cmd location set-location-enabled true")
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS).forEach { permission ->
            shell("pm grant ${context.packageName} $permission")
        }
        original = app.container
        db = Room.inMemoryDatabaseBuilder(context, StrideSetDatabase::class.java).build()
        repository = RoomRunRepository(db.runDao())
        app.container = object : AppContainer by original {
            override val runRepository = repository
            override val runTracker = tracker
            override fun createRunLocationSource(): LocationSource = source
        }
    }

    @After fun tearDown() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        context.stopService(Intent(context, RunTrackingService::class.java))
        compose.waitUntil(10_000) { !tracker.state.value.serviceRunning }
        compose.waitForIdle()
        app.container = original
        db.close()
    }

    @Test fun duplicateStartsUseOneSubscriptionAndNotificationReturnsToRun() {
        command(RunCommand.START)
        awaitRunning()
        val id = tracker.state.value.run!!.id
        command(RunCommand.START)
        compose.waitForIdle()
        assertEquals(id, tracker.state.value.run!!.id)
        assertEquals(1, source.starts)
        compose.waitUntil(10_000) { notifications.activeNotifications.isNotEmpty() }
        val notification = notifications.activeNotifications.single().notification
        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        notification.contentIntent.send()
        compose.onNodeWithText("Run").assertIsDisplayed()
        assertFalse(compose.activity.intent.hasExtra(RunTrackingService.EXTRA_OPEN_RUN))
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep tracking in background").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Distance").assertIsDisplayed()
    }

    @Test fun trackingAndCheckpointsContinueDuringScreenLock() {
        command(RunCommand.START)
        awaitRunning()
        compose.runOnUiThread { source.emit(0.0, 0.0) }
        shell("input keyevent KEYCODE_SLEEP")
        compose.waitUntil(15_000) { (tracker.state.value.run?.activeDuration?.seconds ?: 0) >= 6 }
        compose.runOnUiThread { source.emit(0.0, 0.0001) }
        compose.waitUntil(5000) { tracker.state.value.run!!.distanceMeters > 0 }
        val checkpoint = runBlocking { repository.observeActiveRun().first()!! }
        assertTrue(checkpoint.activeDuration.seconds >= 5)
        assertTrue(source.active)
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        compose.waitForIdle()
        command(RunCommand.PAUSE)
        compose.waitUntil(5000) { tracker.state.value.run?.isPaused == true }
        compose.waitUntil(5000) { !source.active }
        command(RunCommand.DISCARD)
        compose.waitUntil(5000) { tracker.state.value.run == null && !tracker.state.value.serviceRunning }
        compose.waitUntil(5000) { notifications.activeNotifications.isEmpty() }
        assertNull(runBlocking { repository.observeActiveRun().first() })
    }

    @Test fun finishFlushesAndRemovesTheNotification() {
        command(RunCommand.START)
        awaitRunning()
        compose.waitUntil(5000) { tracker.state.value.run!!.activeDuration.seconds >= 1 }
        command(RunCommand.FINISH)
        compose.waitUntil(5000) { tracker.state.value.run?.isFinished == true }
        compose.waitUntil(5000) { notifications.activeNotifications.isEmpty() }
        assertFalse(source.active)
        val id = tracker.state.value.run!!.id
        val saved = runBlocking { repository.observeRun(id).first()!! }
        assertTrue(saved.isFinished)
        assertTrue(saved.activeDuration.seconds >= 1)
    }

    @Test fun stoppingTheServiceClosesItsLocationSubscription() {
        command(RunCommand.START)
        awaitRunning()
        context.stopService(Intent(context, RunTrackingService::class.java))
        compose.waitUntil(5000) { !tracker.state.value.serviceRunning }
        assertFalse(source.active)
        assertEquals(1, source.stops)
        assertTrue(tracker.state.value.run!!.isPaused)
        compose.waitUntil(5000) { notifications.activeNotifications.isEmpty() }
    }

    @Test fun pausingAStoredRunDoesNotLeaveAnUnpromotedServiceRunning() {
        runBlocking { repository.startRun() }
        command(RunCommand.PAUSE)
        compose.waitUntil(5000) { tracker.state.value.run?.isPaused == true }
        compose.waitUntil(5000) { !tracker.state.value.serviceRunning }
        assertTrue(runBlocking { repository.observeActiveRun().first()!! }.isPaused)
        assertEquals(0, source.starts)
        assertTrue(notifications.activeNotifications.isEmpty())
    }

    private fun command(command: RunCommand) {
        compose.runOnIdle { RunTrackingService.sendCommand(compose.activity, command) }
    }

    private fun awaitRunning() {
        compose.waitUntil(10_000) { tracker.state.value.run != null && source.active }
        assertNull(tracker.state.value.error)
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }

    private class ServiceLocationSource : LocationSource {
        @Volatile var active = false
        @Volatile var starts = 0
        @Volatile var stops = 0
        private var onFix: ((LocationFix) -> Unit)? = null
        override fun start(onFix: (LocationFix) -> Unit, onUnavailable: () -> Unit) {
            starts++
            active = true
            this.onFix = onFix
        }
        override fun stop() { active = false; stops++; onFix = null }
        fun emit(latitude: Double, longitude: Double) {
            onFix?.invoke(LocationFix(latitude, longitude, 3.0, SystemClock.elapsedRealtime()))
        }
    }
}
