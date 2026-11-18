package ee.ut.cs.mad.strideset.ui.running

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.ut.cs.mad.strideset.R
import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import ee.ut.cs.mad.strideset.running.GpsState
import ee.ut.cs.mad.strideset.running.RunCommand
import ee.ut.cs.mad.strideset.running.RunTracker
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal data class RunUiState(
    val run: Run? = null,
    val gpsState: GpsState = GpsState.SEARCHING,
    val loading: Boolean = true,
    val recovered: Boolean = false,
    val busy: Boolean = false,
    val errorText: String? = null,
    val errorRes: Int? = null,
    val loadFailed: Boolean = false,
    val discarded: Boolean = false,
    val paceRun: Run? = run,
)

internal class RunViewModel(
    private val repository: RunRepository,
    private val tracker: RunTracker,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private data class Pending(val command: RunCommand, val id: String)
    private var savedRun: Run? = null
    private var completedRun: Run? = null
    private var paceRun: Run? = null
    private var loaded = false
    private var loadFailed = false
    private var pending: Pending? = null
    private var errorRes: Int? = null
    private var discarded = false
    private var loadJob: Job? = null
    private val mutableState = MutableStateFlow(RunUiState())
    val state = mutableState.asStateFlow()

    init {
        reload()
        viewModelScope.launch { tracker.state.collect { publish() } }
        viewModelScope.launch {
            tracker.commandResult.collect { result ->
                val request = pending ?: return@collect
                if (result?.requestId != request.id) return@collect
                pending = null
                if (result.succeeded) {
                    if (request.command == RunCommand.DISCARD) discarded = true
                } else {
                    errorRes = R.string.run_command_failed
                }
                publish()
            }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loaded = false
        loadFailed = false
        errorRes = null
        loadJob = viewModelScope.launch {
            try {
                savedState.get<Long>("completedRunId")?.let { id ->
                    completedRun = repository.observeRun(id).first()?.takeIf { it.isFinished }
                }
                repository.observeActiveRun().collect {
                    savedRun = it
                    loaded = true
                    publish()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loaded = true
                loadFailed = true
                errorRes = R.string.run_load_error
                publish()
            }
        }
        publish()
    }

    fun beginCommand(command: RunCommand): String? {
        if (pending != null || (!loaded && !tracker.state.value.serviceRunning)) return null
        val id = UUID.randomUUID().toString()
        // Capture this before the activity can stop; a background save may finish after its state is saved.
        if (command == RunCommand.FINISH) savedState["completedRunId"] = state.value.run?.id
        pending = Pending(command, id)
        errorRes = null
        publish()
        return id
    }

    fun dispatchFailed(requestId: String) {
        if (pending?.id != requestId) return
        pending = null
        errorRes = R.string.run_command_failed
        publish()
    }

    fun leave() {
        completedRun = null
        savedState["completedRunId"] = null
        discarded = false
        errorRes = null
        publish()
    }

    private fun publish() {
        val live = tracker.state.value
        if (live.run?.isFinished == true && live.run.id == savedState.get<Long>("completedRunId")) {
            completedRun = live.run
        }
        val active = live.run?.takeIf { live.serviceRunning && !it.isFinished }
        // A stored run without a live service is recovered paused; only Resume starts GPS.
        val recovered = active == null && savedRun != null && completedRun == null
        val run = completedRun ?: active ?: savedRun?.copy(isPaused = true)
        val previousPace = paceRun
        // Keep the pace readable while the timer and GPS distance continue updating every second.
        if (run == null || previousPace == null || run.id != previousPace.id ||
            run.isFinished || run.isPaused != previousPace.isPaused ||
            run.activeDuration < previousPace.activeDuration ||
            run.activeDuration.minus(previousPace.activeDuration) >= Duration.ofSeconds(5) ||
            (previousPace.distanceMeters == 0.0 && run.distanceMeters > 0.0)) {
            paceRun = run
        }
        mutableState.value = RunUiState(
            run = run,
            paceRun = paceRun,
            gpsState = live.gpsState,
            loading = !loaded && active == null && completedRun == null,
            recovered = recovered,
            busy = pending != null,
            errorText = live.error.takeIf { pending == null && completedRun == null },
            errorRes = errorRes,
            loadFailed = loadFailed,
            discarded = discarded,
        )
    }
}
