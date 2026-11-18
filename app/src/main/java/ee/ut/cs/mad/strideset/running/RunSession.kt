package ee.ut.cs.mad.strideset.running

import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Called serially by the service, on the same thread as location callbacks. */
class RunSession(
    private val repository: RunRepository,
    private val source: LocationSource,
    private val tracker: RunTracker,
    private val nowMillis: () -> Long,
) {
    private var run: Run? = null
    private var engine: TrackingEngine? = null
    private var sourceRunning = false
    private var sourceUnavailable = false
    private var lastCheckpoint = nowMillis()
    private var error: String? = null

    suspend fun start() {
        if (engine != null) {
            publish()
            return
        }
        // A fresh service never silently resumes an interrupted session.
        if (loadCheckpoint()) return
        val id = repository.startRun()
        run = checkNotNull(repository.observeRun(id).first())
        engine = TrackingEngine(nowMillis = nowMillis)
        lastCheckpoint = nowMillis()
        publish()
        startSource()
    }

    suspend fun resume() {
        if (engine == null && !loadCheckpoint()) return
        val engine = checkNotNull(engine)
        if (!engine.snapshot().isPaused) return
        // Retry any unsaved in-memory progress before resuming the timer.
        checkpoint()
        repository.resumeRun(checkNotNull(run).id)
        engine.resume()
        error = null
        sourceUnavailable = false
        publish()
        startSource()
    }

    suspend fun pause(message: String? = null) {
        if (engine == null && !loadCheckpoint()) return
        pauseLocally()
        error = message
        publish()
        checkpoint()
        repository.pauseRun(checkNotNull(run).id)
    }

    suspend fun tick() {
        val engine = engine ?: return
        if (sourceUnavailable) {
            sourceUnavailable = false
            pause("GPS is unavailable. Enable precise location and GPS, then resume.")
            return
        }
        publish()
        if (!engine.snapshot().isPaused && nowMillis() - lastCheckpoint >= 5000) checkpoint()
    }

    suspend fun finish() {
        if (engine == null && !loadCheckpoint()) return
        pause()
        val id = checkNotNull(run).id
        run = repository.finishRun(id)
        engine = null
        error = null
        publish()
    }

    suspend fun discard() {
        if (engine == null && !loadCheckpoint()) return
        pauseLocally()
        publish()
        // Deleting a run must still work if saving progress fails, for example on a full disk.
        repository.discardRun(checkNotNull(run).id)
        engine = null
        run = null
        error = null
        publish()
    }

    /** Keep the live totals paused even when persistence is temporarily unavailable. */
    suspend fun fail(message: String) {
        pauseLocally()
        error = message
        publish()
        if (engine == null) return
        try {
            checkpoint()
            repository.pauseRun(checkNotNull(run).id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The last durable checkpoint remains recoverable; the screen can retry live totals.
        }
    }

    fun close() {
        pauseLocally()
        publish(serviceRunning = false)
        engine = null
    }

    private suspend fun loadCheckpoint(): Boolean {
        val saved = repository.observeActiveRun().first() ?: return false
        run = saved.copy(isPaused = true)
        engine = TrackingEngine(saved.distanceMeters, saved.activeDuration, true, nowMillis)
        lastCheckpoint = nowMillis()
        publish()
        repository.pauseRun(saved.id)
        return true
    }

    private fun startSource() {
        if (sourceRunning) return
        sourceRunning = true
        source.start(
            onFix = { fix -> engine?.accept(fix); publish() },
            onUnavailable = { sourceUnavailable = true },
        )
    }

    private fun pauseLocally() {
        engine?.pause()
        if (sourceRunning) {
            sourceRunning = false
            source.stop()
        }
    }

    private suspend fun checkpoint() {
        val progress = engine?.snapshot() ?: return
        repository.updateProgress(checkNotNull(run).id, progress.distanceMeters, progress.activeDuration)
        lastCheckpoint = nowMillis()
    }

    private fun publish(serviceRunning: Boolean = engine != null) {
        val progress = engine?.snapshot()
        if (progress != null) {
            run = run?.copy(distanceMeters = progress.distanceMeters,
                activeDuration = progress.activeDuration, isPaused = progress.isPaused)
        }
        tracker.publish(RunTrackingState(run, progress?.gpsState ?: GpsState.SEARCHING, serviceRunning, error))
    }
}
