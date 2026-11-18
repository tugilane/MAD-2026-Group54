package ee.ut.cs.mad.strideset.running

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.IBinder
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import ee.ut.cs.mad.strideset.MainActivity
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.StrideSetApplication
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RunCommand { START, PAUSE, RESUME, FINISH, DISCARD }

/** Owns the engine, location subscription, timer and all session writes. */
class RunTrackingService : Service() {
    private sealed interface Event {
        data class Command(val command: RunCommand, val startId: Int, val requestId: String) : Event
        data object Tick : Event
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private lateinit var session: RunSession
    private lateinit var tracker: RunTracker
    private lateinit var wakeLock: PowerManager.WakeLock
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as StrideSetApplication).container
        tracker = container.runTracker
        session = RunSession(container.runRepository, container.createRunLocationSource(), tracker,
            SystemClock::elapsedRealtime)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StrideSet:Running")
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.run_notification_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
        scope.launch {
            for (event in events) {
                var succeeded = true
                try {
                    when (event) {
                        is Event.Command -> when (event.command) {
                            RunCommand.START -> session.start()
                            RunCommand.PAUSE -> session.pause()
                            RunCommand.RESUME -> session.resume()
                            RunCommand.FINISH -> session.finish()
                            RunCommand.DISCARD -> session.discard()
                        }
                        Event.Tick -> {
                            val run = tracker.state.value.run
                            if (run != null && !run.isPaused && !run.isFinished && !locationReady(this@RunTrackingService)) {
                                session.pause(getString(R.string.run_location_unavailable))
                            } else {
                                session.tick()
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    succeeded = false
                    Log.e(TAG, "Running command failed", error)
                    session.fail(getString(R.string.run_tracking_error))
                }
                if (event is Event.Command) tracker.completeCommand(event.requestId, succeeded)
                updateNotificationAndPower()
                // A cold pause/finish/discard must not leave an ordinary background service alive.
                if (event is Event.Command && (!tracker.state.value.serviceRunning || !foreground) && stopSelfResult(event.startId)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    foreground = false
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(1000)
                events.send(Event.Tick)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = RunCommand.entries.firstOrNull { it.name == intent?.action }
        if (command == null) {
            if (!tracker.state.value.serviceRunning) stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val requestId = intent?.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        if (command == RunCommand.START || command == RunCommand.RESUME) {
            try {
                check(locationReady(this)) { getString(R.string.run_location_unavailable) }
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else 0
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
                foreground = true
            } catch (error: RuntimeException) {
                Log.e(TAG, "Could not start foreground tracking", error)
                tracker.publish(tracker.state.value.copy(error = getString(R.string.run_location_unavailable)))
                tracker.completeCommand(requestId, false)
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
        }
        events.trySend(Event.Command(command, startId, requestId))
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            session.close()
        } finally {
            scope.cancel()
            events.close()
            if (wakeLock.isHeld) wakeLock.release()
            stopForeground(STOP_FOREGROUND_REMOVE)
            super.onDestroy()
        }
    }

    @SuppressLint("MissingPermission", "WakelockTimeout")
    private fun updateNotificationAndPower() {
        val run = tracker.state.value.run
        val tracking = tracker.state.value.serviceRunning && run != null && !run.isPaused && !run.isFinished
        // Keep the one-second timer and five-second saves running while locked. Every exit releases this lock.
        if (tracking && !wakeLock.isHeld) wakeLock.acquire()
        if (!tracking && wakeLock.isHeld) wakeLock.release()
        // Foreground notifications are required even when POST_NOTIFICATIONS was declined.
        if (foreground) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
    }

    private fun notification(): Notification {
        val run = tracker.state.value.run
        val seconds = run?.activeDuration?.seconds ?: 0
        val elapsed = String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
        val openRun = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .putExtra(EXTRA_OPEN_RUN, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 0, openRun,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentTitle(getString(if (run?.isPaused == true) R.string.run_notification_paused else R.string.run_notification_active))
            .setContentText(getString(R.string.run_notification_time, elapsed))
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_OPEN_RUN = "ee.ut.cs.mad.strideset.OPEN_RUN"
        private const val EXTRA_REQUEST_ID = "ee.ut.cs.mad.strideset.RUN_REQUEST_ID"
        private const val CHANNEL_ID = "running"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "RunTrackingService"

        /** Start/resume only from a visible activity, after its location permission request succeeds. */
        fun sendCommand(
            activity: ComponentActivity,
            command: RunCommand,
            requestId: String = java.util.UUID.randomUUID().toString(),
        ) {
            check(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                "Running commands must come from the visible app"
            }
            val intent = Intent(activity, RunTrackingService::class.java).setAction(command.name)
                .putExtra(EXTRA_REQUEST_ID, requestId)
            if (command == RunCommand.START || command == RunCommand.RESUME) {
                check(locationReady(activity)) { activity.getString(R.string.run_location_unavailable) }
                ContextCompat.startForegroundService(activity, intent)
            } else {
                activity.startService(intent)
            }
        }

        private fun locationReady(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
